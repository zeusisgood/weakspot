package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.BlockHealthBar;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import io.github.zeusisgood.weakspot.server.MachineStates;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** 描くのは WeakSpotRenderer。 */
final class OwnSpotBars {

    private OwnSpotBars() {
    }

    private static WeakSpot spot() {
        return ClientWeakSpotHandler.spot;
    }

    /** 0〜1。出さないときは -1。長押しをやめた・壊れたときは、掘っていない扱いになってすぐに消える。 */
    static double healthRemaining(Minecraft mc, float partialTicks) {
        HitResult target = mc.hitResult;
        if (spot() == null || spot().lastActiveTick != ClientWeakSpotHandler.clientTick || mc.player.isCreative()
                || !(target instanceof BlockHitResult block) || target.getType() != HitResult.Type.BLOCK
                || !spot().matches(HitKind.MINING, block.getBlockPos(), block.getDirection())) {
            return -1;
        }
        double progress = MiningProgress.progress(mc.gameMode, spot().pos, partialTicks);
        return progress < 0 ? -1 : BlockHealthBar.remaining(progress);
    }

    /** 出すときは、サーバーに値を問い合わせる（間隔があいていなければ送らない）。 */
    static boolean machineBar(Minecraft mc) {
        if (!WeakSpotConfig.client.hud.machineBarEnabled || !ClientWeakSpotHandler.machineSpotActive()
                || !MachineStates.hasBar(mc.level.getBlockEntity(spot().pos))) {
            return false;
        }
        MachineBars.query(spot().pos, ClientWeakSpotHandler.clientTick, false);
        return true;
    }

    /** 0〜1。出さないときは -1。 */
    static double growthProgress(Minecraft mc) {
        if (spot() == null || spot().kind != HitKind.GROWTH || spot().lastActiveTick != ClientWeakSpotHandler.clientTick) {
            return -1;
        }
        return GrowthBar.progress(mc.level, spot().pos);
    }

    /** 0〜1。出さないときは -1。 */
    static double animalProgress() {
        if (spot() == null || spot().kind != HitKind.ANIMAL || spot().lastActiveTick != ClientWeakSpotHandler.clientTick) {
            return -1;
        }
        AnimalStates.State state = AnimalStates.get(spot().entity.getId(), ClientWeakSpotHandler.clientTick);
        return state == null ? -1 : state.barProgress();
    }
}
