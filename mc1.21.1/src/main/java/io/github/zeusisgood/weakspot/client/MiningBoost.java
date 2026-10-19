package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.BoostMath;
import io.github.zeusisgood.weakspot.common.ComboFactor;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * MultiPlayerGameMode は tick ごとに進捗を積むので、クライアントはヒットの次の tick から boostDurationTicks の間だけ、
 * 破壊速度に倍率を掛ける。サーバーは瞬間判定なので別の計算（ServerBoostTracker・BoostMath）。
 *
 * ブロックをクリックするたびにブーストを捨てる（サーバーもクリックのたびに数え直す）。クライアントだけ速いと、
 * クリックし直したブロックがクライアントだけでその場で壊れ、ゴーストブロックになる。
 */
@EventBusSubscriber(modid = WeakSpotMod.MODID, value = Dist.CLIENT)
final class MiningBoost {

    private static BlockPos boostPos;
    private static long boostHitTick = Long.MIN_VALUE / 2;
    /** サーバーが古ければ 1。 */
    private static double boostFactor = 1;
    private static boolean suppressBoost;

    /** 壊したら、次を掘り始めるまでの待ち（MultiPlayerGameMode の destroyDelay。accesstransformer.cfg）を 0 にする。 */
    private static BlockPos hitBlock;
    private static BlockPos preHitPos;
    private static long preHitTick;
    private static int preHitStreak;

    private MiningBoost() {
    }

    /** streak はヒット後の数。 */
    static void onHit(BlockPos pos, int streak) {
        boostHitTick = ClientWeakSpotHandler.clientTick;
        boostPos = pos;
        boostFactor = comboFactor(streak);
        hitBlock = pos;
    }

    static void onPreHit(BlockPos pos, int streak) {
        preHitPos = pos;
        preHitTick = ClientWeakSpotHandler.clientTick;
        preHitStreak = streak;
        hitBlock = pos;
    }

    /** 掘る前に当てられるのは 1 回まで。 */
    static boolean hasPreHit(BlockPos pos) {
        return pos.equals(preHitPos) && ClientWeakSpotHandler.clientTick - preHitTick <= BoostMath.PRE_DIG_TICKS;
    }

    /** フレームごとに呼ぶ。 */
    static void onDigging(BlockPos pos) {
        if (preHitPos == null || !preHitPos.equals(pos)) {
            return;
        }
        if (hasPreHit(pos)) {
            onHit(pos, preHitStreak);
        }
        preHitPos = null;
    }

    /** 1 tick で壊れるブロックも出す。クリエイティブは一瞬で壊れるので出さない。 */
    static boolean isPreDigEligible(Level world, LocalPlayer player, BlockPos pos, BlockState state) {
        return !player.isCreative() && !state.isAir() && state.getDestroySpeed(world, pos) >= 0;
    }

    /** バニラは壊した tick の進捗の計算の中で待ちを 5 にするので、その tick の Post で戻す。 */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (hitBlock == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gameMode == null) {
            hitBlock = null;
            return;
        }
        if (!mc.level.isEmptyBlock(hitBlock)) {
            return;
        }
        hitBlock = null;
        if (mc.gameMode.destroyDelay > 0) {
            mc.gameMode.destroyDelay = 0;
        }
    }

    /**
    * MultiPlayerGameMode.startDestroyBlock から来る（その場で壊れるかの判定より先）。長押しの間も毎 tick
    * CLIENT_HOLD で来るので、掘り始め（START）だけを見る。
    * 掘る前のヒットと、待ちを 0 にするための当てたブロックは残す。
    */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getLevel().isClientSide() || event.getEntity() != Minecraft.getInstance().player
                || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }
        boostPos = null;
        boostHitTick = Long.MIN_VALUE / 2;
        boostFactor = 1;
    }

    static void clear() {
        boostPos = null;
        boostHitTick = Long.MIN_VALUE / 2;
        boostFactor = 1;
        hitBlock = null;
        preHitPos = null;
    }

    /** 壊せないブロック（硬度が負）と、今の破壊速度で 1 tick 以内に壊れるブロックは対象外。 */
    static boolean isEligible(Level world, LocalPlayer player, BlockPos pos, BlockState state) {
        if (state.isAir() || state.getDestroySpeed(world, pos) < 0) {
            return false;
        }
        suppressBoost = true;
        try {
            return state.getDestroyProgress(player, world, pos) < 1.0F;
        } finally {
            suppressBoost = false;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!event.getEntity().level().isClientSide() || suppressBoost || !KindSwitches.isEnabled(HitKind.MINING)) {
            return;
        }
        BlockPos pos = event.getPosition().orElse(null);
        if (event.getEntity() != Minecraft.getInstance().player || pos == null || !pos.equals(boostPos)) {
            return;
        }
        SyncedSettings settings = ClientSettings.get();
        long sinceHit = ClientWeakSpotHandler.clientTick - boostHitTick;
        if (sinceHit > 0 && sinceHit <= settings.boostDurationTicks) {
            event.setNewSpeed((float) (event.getNewSpeed()
                    * BoostMath.clientMultiplier(settings.boostMultiplier, boostFactor)));
        }
    }

    /** サーバーの設定 miningComboBonus でオフにできる。 */
    static double comboFactor(int combo) {
        return ClientSettings.get().miningComboBonus ? ComboFactor.factor(combo) : 1;
    }
}
