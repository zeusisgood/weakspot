package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.BoostMath;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.network.HitMessage;
import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * クライアント側の弱点処理。弱点の位置は自分のクライアントだけが持ち、他プレイヤーとは共有しない。
 *
 * ヒット判定は毎フレーム行う（素早く照準を動かしたときに tick 単位だと取りこぼすため）。
 * 破壊速度のブーストは PlayerControllerMP の tick ごとの進捗に掛かるので、tick 単位で管理する。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
public final class ClientWeakSpotHandler {

    private static final Random RANDOM = new Random();
    /** 叩くのをやめてからこの tick 以内にブロックが消えたら、自分で壊したとみなす（低 FPS でも取りこぼさない程度の余裕）。 */
    private static final int BROKEN_DETECTION_TICKS = 5;

    /** ClientTickEvent の START で増える。PlayerControllerMP の進捗計算はその後に走る。 */
    static long clientTick;
    static WeakSpot spot;

    private static long lastHitTick = Long.MIN_VALUE / 2;
    private static BlockPos boostPos;
    private static long boostHitTick = Long.MIN_VALUE / 2;
    /** 瞬間破壊の判定中は、自分のブーストを掛けない。 */
    private static boolean suppressBoost;
    /** ワールドに入った瞬間（統計の「今回」の開始）と出た瞬間（保存）を検出する。 */
    private static boolean inWorld;

    private ClientWeakSpotHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null) {
            if (inWorld) {
                inWorld = false;
                StatsManager.save();
            }
            reset();
            return;
        }
        if (!inWorld) {
            inWorld = true;
            StatsManager.startSession();
        }
        if (mc.isGamePaused()) {
            return;
        }
        clientTick++;
        if (spot != null && mc.world.isAirBlock(spot.pos)) {
            // 直前まで叩いていたブロックが消えた = 自分で壊した
            if (clientTick - spot.lastActiveTick <= BROKEN_DETECTION_TICKS) {
                StatsManager.recordBlockBroken(spot.hits);
            }
            spot = null;
        } else if (spot != null && clientTick - spot.lastActiveTick > WeakSpotConfig.lingerTicks) {
            spot = null;
        }
        WeakSpotRenderer.expireFlashes(clientTick);
        StatsManager.tick(clientTick);
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null) {
            return;
        }
        updateAim(mc);
        WeakSpotRenderer.render(mc, spot, clientTick, event.getPartialTicks());
    }

    private static void updateAim(Minecraft mc) {
        EntityPlayerSP player = mc.player;
        if (!mc.playerController.getIsHittingBlock() || player.capabilities.isCreativeMode || player.isSpectator()) {
            return;
        }
        RayTraceResult target = mc.objectMouseOver;
        if (target == null || target.typeOfHit != RayTraceResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = target.getBlockPos();
        IBlockState state = mc.world.getBlockState(pos);
        if (!isEligible(mc.world, player, pos, state)) {
            return;
        }

        if (spot == null || !spot.matches(pos, target.sideHit)) {
            spot = WeakSpot.spawn(mc.world, pos, state, target.sideHit, target.hitVec,
                    WeakSpotConfig.weakSpotRadiusRatio, WeakSpotConfig.edgeMargin, WeakSpotConfig.minMoveDistance,
                    RANDOM);
        }
        spot.lastActiveTick = clientTick;

        if (spot.isHitBy(target.hitVec) && clientTick - lastHitTick >= WeakSpotConfig.minHitIntervalTicks) {
            onHit(mc);
        }
    }

    /** 壊せないブロック（硬度が負）と、今の破壊速度で1tick以内に壊れるブロックは対象外。 */
    private static boolean isEligible(World world, EntityPlayerSP player, BlockPos pos, IBlockState state) {
        if (state.getBlock().isAir(state, world, pos) || state.getBlockHardness(world, pos) < 0) {
            return false;
        }
        suppressBoost = true;
        try {
            return state.getPlayerRelativeBlockHardness(player, world, pos) < 1.0F;
        } finally {
            suppressBoost = false;
        }
    }

    private static void onHit(Minecraft mc) {
        WeakSpotRenderer.addFlash(spot, clientTick);
        mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.BLOCK_NOTE_PLING, 2.0F));

        spot.hits++;
        StatsManager.recordHit(BoostMath.extraTicksPerHit(
                WeakSpotConfig.boostMultiplier, WeakSpotConfig.boostDurationTicks));

        lastHitTick = clientTick;
        boostHitTick = clientTick;
        boostPos = spot.pos;
        WeakSpotMod.network.sendToServer(new HitMessage(spot.pos));

        spot.relocate(WeakSpotConfig.edgeMargin, WeakSpotConfig.minMoveDistance, RANDOM);
    }

    /** ヒット後の次の tick から boostDurationTicks 回分の進捗計算に倍率を掛ける。 */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!event.getEntityPlayer().world.isRemote || suppressBoost) {
            return;
        }
        if (event.getEntityPlayer() != Minecraft.getMinecraft().player || !event.getPos().equals(boostPos)) {
            return;
        }
        long sinceHit = clientTick - boostHitTick;
        if (sinceHit > 0 && sinceHit <= WeakSpotConfig.boostDurationTicks) {
            event.setNewSpeed((float) (event.getNewSpeed() * WeakSpotConfig.boostMultiplier));
        }
    }

    private static void reset() {
        spot = null;
        boostPos = null;
        lastHitTick = Long.MIN_VALUE / 2;
        boostHitTick = Long.MIN_VALUE / 2;
        WeakSpotRenderer.clearFlashes();
    }
}
