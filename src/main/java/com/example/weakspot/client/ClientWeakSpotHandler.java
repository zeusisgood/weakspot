package com.example.weakspot.client;

import com.example.weakspot.RightClickTargets;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.HitStreak;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.network.HitMessage;
import java.util.Arrays;
import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
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
 * 左クリックの長押し（採掘）と、右クリックの押しっぱなし（作物・苗木、機械）の弱点を扱う。弱点は一度に1つだけ。
 *
 * ヒット判定は毎フレーム行う（素早く照準を動かしたときに tick 単位だと取りこぼすため）。
 * 破壊速度のブーストは PlayerControllerMP の tick ごとの進捗に掛かるので、tick 単位で管理する。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
public final class ClientWeakSpotHandler {

    private static final Random RANDOM = new Random();

    /** ClientTickEvent の START で増える。PlayerControllerMP の進捗計算はその後に走る。 */
    static long clientTick;
    static WeakSpot spot;
    /** 今のフレームの partialTicks（ヒットした瞬間を、フレームの途中の値まで含めてコンボの表示に渡す）。 */
    private static float framePartialTicks;

    /** 種類ごとの最後のヒット（ヒット間隔の制限に使う）。 */
    private static final long[] LAST_HIT_TICK = new long[HitKind.values().length];
    /** 連続ヒット数。ブロックや種類をまたいで続き、ヒット音のピッチとコンボの表示に使う。 */
    static final HitStreak STREAK = new HitStreak();
    /** 死亡・ディメンション移動（どちらもプレイヤーが作り直される）を見分けるため。 */
    private static EntityPlayerSP lastPlayer;
    private static BlockPos boostPos;
    private static long boostHitTick = Long.MIN_VALUE / 2;
    /** 瞬間破壊の判定中は、自分のブーストを掛けない。 */
    private static boolean suppressBoost;

    static {
        Arrays.fill(LAST_HIT_TICK, Long.MIN_VALUE / 2);
    }

    private ClientWeakSpotHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null) {
            reset();
            return;
        }
        if (mc.player != lastPlayer || mc.player.getHealth() <= 0) {
            // 死亡したとき、リスポーンやディメンション移動でプレイヤーが作り直されたときは、連続ヒットを最初に戻す
            lastPlayer = mc.player;
            resetStreak();
        }
        if (mc.isGamePaused()) {
            return;
        }
        clientTick++;
        int broken = STREAK.expire(clientTick);
        if (broken > 0) {
            ComboHud.onBreak(broken, clientTick);
        }
        if (spot != null && (isGone(mc.world, spot)
                || clientTick - spot.lastActiveTick > ClientSettings.get().lingerTicks)) {
            spot = null;
        }
        WeakSpotRenderer.expireFlashes(clientTick);
    }

    /** 弱点を出したブロックがなくなった（壊れた、育ちきった、など）。 */
    private static boolean isGone(World world, WeakSpot spot) {
        if (world.isAirBlock(spot.pos)) {
            return true;
        }
        IBlockState state = world.getBlockState(spot.pos);
        switch (spot.kind) {
            case GROWTH:
                return !RightClickTargets.isGrowable(world, spot.pos, state, ClientSettings.get());
            case MACHINE:
                return !RightClickTargets.isMachine(world, spot.pos, state, ClientSettings.get());
            default:
                return false;
        }
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null) {
            return;
        }
        framePartialTicks = event.getPartialTicks();
        updateAim(mc);
        WeakSpotRenderer.render(mc, spot, clientTick, event.getPartialTicks());
    }

    private static void updateAim(Minecraft mc) {
        EntityPlayerSP player = mc.player;
        if (player.capabilities.isCreativeMode || player.isSpectator()) {
            return;
        }
        RayTraceResult target = mc.objectMouseOver;
        if (target == null || target.typeOfHit != RayTraceResult.Type.BLOCK) {
            return;
        }
        if (mc.playerController.getIsHittingBlock()) {
            aimMining(mc, target);
        } else if (isHoldingUse(mc)) {
            aimRightClick(mc, target);
        }
    }

    /** 右クリックを押しっぱなしにしているか（バニラが右クリックを繰り返す条件と同じ）。 */
    private static boolean isHoldingUse(Minecraft mc) {
        return mc.currentScreen == null && mc.gameSettings.keyBindUseItem.isKeyDown() && !mc.player.isHandActive();
    }

    private static void aimMining(Minecraft mc, RayTraceResult target) {
        BlockPos pos = target.getBlockPos();
        IBlockState state = mc.world.getBlockState(pos);
        if (!isEligible(mc.world, mc.player, pos, state)) {
            return;
        }
        SyncedSettings settings = ClientSettings.get();
        if (spot == null || !spot.matches(HitKind.MINING, pos, target.sideHit)) {
            spot = WeakSpot.spawn(HitKind.MINING, mc.world, pos, state, target.sideHit, target.hitVec,
                    settings.weakSpotRadiusRatio, 0, settings.edgeMargin, settings.minMoveDistance, RANDOM);
        }
        spot.lastActiveTick = clientTick;
        if (spot.isHitBy(target.hitVec) && canHit(HitKind.MINING, settings.minHitIntervalTicks)) {
            onHit(mc);
        }
    }

    /**
     * 右クリックの弱点。植物は一番大きい面（多くは上面。サトウキビなどは側面）に、機械は狙っている面に出す。
     * 照準がその面に当たっているときだけヒットにする。
     */
    private static void aimRightClick(Minecraft mc, RayTraceResult target) {
        BlockPos pos = target.getBlockPos();
        SyncedSettings settings = ClientSettings.get();
        HitKind kind = RightClickTargets.classify(mc.world, mc.player, pos, settings);
        if (kind == null) {
            return;
        }
        IBlockState state = mc.world.getBlockState(pos);
        AxisAlignedBB box = state.getSelectedBoundingBox(mc.world, pos);
        EnumFacing face = kind == HitKind.GROWTH ? growthFace(mc, pos, box, target.sideHit) : target.sideHit;
        if (spot == null || !spot.matches(kind, pos, face) || !spot.box.equals(box)) {
            double minRadius = kind == HitKind.GROWTH ? settings.growthMinRadius : 0;
            spot = WeakSpot.spawn(kind, mc.world, pos, state, face, target.hitVec,
                    settings.weakSpotRadiusRatio, minRadius, settings.edgeMargin, settings.minMoveDistance, RANDOM);
        }
        spot.lastActiveTick = clientTick;
        if (target.sideHit == spot.face && spot.isHitBy(target.hitVec)
                && canHit(kind, minHitInterval(kind, settings))) {
            onHit(mc);
        }
    }

    /**
     * 成長の弱点を出す面。同じ大きさの側面が複数あるとき（サトウキビなど）は、今の弱点の面が見えている間は変えない
     * （照準が隣の側面へ移ってもちらつかない）。見えなくなったとき（回り込んだとき）や出し直すときに選び直す。
     */
    private static EnumFacing growthFace(Minecraft mc, BlockPos pos, AxisAlignedBB box, EnumFacing aimed) {
        Vec3d eye = mc.player.getPositionEyes(1.0F);
        EnumFacing keep = spot != null && spot.kind == HitKind.GROWTH && spot.pos.equals(pos) && spot.box.equals(box)
                && WeakSpot.isFacing(box, spot.face, eye) ? spot.face : null;
        return WeakSpot.growthFace(box, eye, aimed, keep);
    }

    private static int minHitInterval(HitKind kind, SyncedSettings settings) {
        return kind == HitKind.MACHINE ? settings.machineMinHitIntervalTicks : settings.growthMinHitIntervalTicks;
    }

    private static boolean canHit(HitKind kind, int minInterval) {
        return clientTick - LAST_HIT_TICK[kind.ordinal()] >= minInterval;
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
        HitKind kind = spot.kind;
        WeakSpotRenderer.addFlash(spot, clientTick);
        int hitStreak = STREAK.hit(clientTick);
        HitSounds.playOwn(hitStreak);
        ComboHud.onHit(hitStreak, clientTick + framePartialTicks);

        LAST_HIT_TICK[kind.ordinal()] = clientTick;
        if (kind == HitKind.MINING) {
            boostHitTick = clientTick;
            boostPos = spot.pos;
        }
        WeakSpotMod.network.sendToServer(new HitMessage(kind, spot.pos, hitStreak));

        SyncedSettings settings = ClientSettings.get();
        spot.relocate(settings.edgeMargin, settings.minMoveDistance, RANDOM);
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
        SyncedSettings settings = ClientSettings.get();
        long sinceHit = clientTick - boostHitTick;
        if (sinceHit > 0 && sinceHit <= settings.boostDurationTicks) {
            event.setNewSpeed((float) (event.getNewSpeed() * settings.boostMultiplier));
        }
    }

    private static void reset() {
        spot = null;
        boostPos = null;
        Arrays.fill(LAST_HIT_TICK, Long.MIN_VALUE / 2);
        boostHitTick = Long.MIN_VALUE / 2;
        lastPlayer = null;
        resetStreak();
        WeakSpotRenderer.clearFlashes();
        HitSounds.clear();
    }

    private static void resetStreak() {
        STREAK.reset();
        ComboHud.clear();
    }
}
