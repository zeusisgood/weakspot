package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.Reflect;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.BoostMath;
import io.github.zeusisgood.weakspot.common.ComboFactor;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import java.lang.reflect.Field;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 自分の採掘のブースト（クライアント。1.8.9 で ClientWeakSpotHandler から分けた）。PlayerControllerMP は tick ごとに
 * 進捗を積むので、ヒットの次の tick から boostDurationTicks の間だけ、破壊速度に倍率（コンボの掛け数つき）を掛ける。
 * サーバーは瞬間判定なので別の計算（server/ServerBoostTracker・BoostMath）。
 *
 * 1.11.2: 掘る前のヒットを覚えておき、同じブロックを掘り始めたらブーストをかける。当てたブロックを壊したら、
 * 次を掘り始めるまでの待ち（PlayerControllerMP の非公開の blockHitDelay、5 tick）を 0 にする。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
final class MiningBoost {

    private static BlockPos boostPos;
    private static long boostHitTick = Long.MIN_VALUE / 2;
    /** 最後の採掘ヒットのコンボの掛け数（1.8.7。サーバーが古ければ 1）。 */
    private static double boostFactor = 1;
    /** 瞬間破壊の判定中は、自分のブーストを掛けない。 */
    private static boolean suppressBoost;

    private static final Field HIT_DELAY = Reflect.field(PlayerControllerMP.class,
            "skipping the wait after breaking a block that was hit", "blockHitDelay", "field_78781_i");
    /** 当てたブロック（1.11.2）。壊したら、次を掘り始めるまでの待ちを 0 にする。 */
    private static BlockPos hitBlock;
    /** 掘る前のヒット（1.11.2）。掘り始めたらブーストをかける。 */
    private static BlockPos preHitPos;
    private static long preHitTick;
    private static int preHitStreak;

    private MiningBoost() {
    }

    /** 採掘の弱点に当てた（streak はヒット後の連続ヒット数）。 */
    static void onHit(BlockPos pos, int streak) {
        boostHitTick = ClientWeakSpotHandler.clientTick;
        boostPos = pos;
        boostFactor = comboFactor(streak);
        hitBlock = pos;
    }

    /** 掘る前の採掘の弱点に当てた（1.11.2）。 */
    static void onPreHit(BlockPos pos, int streak) {
        preHitPos = pos;
        preHitTick = ClientWeakSpotHandler.clientTick;
        preHitStreak = streak;
        hitBlock = pos;
    }

    /** そのブロックに、掘る前のヒットがまだ残っているか（掘る前に当てられるのは 1 回まで）。 */
    static boolean hasPreHit(BlockPos pos) {
        return pos.equals(preHitPos) && ClientWeakSpotHandler.clientTick - preHitTick <= BoostMath.PRE_DIG_TICKS;
    }

    /** そのブロックを掘っている（フレームごと）。掘る前に当てていれば、掘り始めたここからブーストをかける。 */
    static void onDigging(BlockPos pos) {
        if (preHitPos == null || !preHitPos.equals(pos)) {
            return;
        }
        if (hasPreHit(pos)) {
            onHit(pos, preHitStreak);
        }
        preHitPos = null;
    }

    /** 掘る前の弱点を出せるブロックか（1.11.2）。1 tick で壊れるブロックも出す。クリエイティブは一瞬で壊れるので出さない。 */
    static boolean isPreDigEligible(World world, EntityPlayerSP player, BlockPos pos, IBlockState state) {
        return !player.capabilities.isCreativeMode && !state.getBlock().isAir(state, world, pos)
                && state.getBlockHardness(world, pos) >= 0;
    }

    /**
     * 当てたブロックが壊れたら、次を掘り始めるまでの待ちを 0 にする（1.11.2）。バニラは壊した tick の進捗の計算の中で
     * 待ちを 5 にするので、その tick の END で戻す。
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || hitBlock == null || HIT_DELAY == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.playerController == null) {
            hitBlock = null;
            return;
        }
        if (!mc.world.isAirBlock(hitBlock)) {
            return;
        }
        hitBlock = null;
        try {
            if (HIT_DELAY.getInt(mc.playerController) > 0) {
                HIT_DELAY.setInt(mc.playerController, 0);
            }
        } catch (IllegalAccessException e) {
            // 読めなければ、バニラの待ちのまま
        }
    }

    /** 一時オフ・ワールドを出たとき。 */
    static void clear() {
        boostPos = null;
        boostHitTick = Long.MIN_VALUE / 2;
        boostFactor = 1;
        hitBlock = null;
        preHitPos = null;
    }

    /** 壊せないブロック（硬度が負）と、今の破壊速度で1tick以内に壊れるブロックは対象外。 */
    static boolean isEligible(World world, EntityPlayerSP player, BlockPos pos, IBlockState state) {
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

    /** ヒット後の次の tick から boostDurationTicks 回分の進捗計算に倍率を掛ける。 */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!event.getEntityPlayer().world.isRemote || suppressBoost || !KindSwitches.isEnabled(HitKind.MINING)) {
            return;
        }
        if (event.getEntityPlayer() != Minecraft.getMinecraft().player || !event.getPos().equals(boostPos)) {
            return;
        }
        SyncedSettings settings = ClientSettings.get();
        long sinceHit = ClientWeakSpotHandler.clientTick - boostHitTick;
        if (sinceHit > 0 && sinceHit <= settings.boostDurationTicks) {
            event.setNewSpeed((float) (event.getNewSpeed()
                    * BoostMath.clientMultiplier(settings.boostMultiplier, boostFactor)));
        }
    }

    /** 採掘のコンボの掛け数（1.8.7。1.9.0 からサーバーの設定 miningComboBonus でオフにできる）。 */
    static double comboFactor(int combo) {
        return ClientSettings.get().miningComboBonus ? ComboFactor.factor(combo) : 1;
    }
}
