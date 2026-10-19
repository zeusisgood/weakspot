package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.BlockHealthBar;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import io.github.zeusisgood.weakspot.server.MachineStates;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.RayTraceResult;

/** 描くのは WeakSpotRenderer。 */
final class OwnSpotBars {

    private OwnSpotBars() {
    }

    private static WeakSpot spot() {
        return ClientWeakSpotHandler.spot;
    }

    /** 0〜1。出さないときは -1。長押しをやめた・壊れたときは、掘っていない扱いになってすぐに消える。 */
    static double healthRemaining(Minecraft mc, float partialTicks) {
        RayTraceResult target = mc.objectMouseOver;
        if (spot() == null || spot().lastActiveTick != ClientWeakSpotHandler.clientTick || mc.player.capabilities.isCreativeMode
                || target == null || target.typeOfHit != RayTraceResult.Type.BLOCK
                || !spot().matches(HitKind.MINING, target.getBlockPos(), target.sideHit)) {
            return -1;
        }
        double progress = MiningProgress.progress(mc.playerController, spot().pos, partialTicks);
        return progress < 0 ? -1 : BlockHealthBar.remaining(progress);
    }

    /** 出すときは、サーバーに値を問い合わせる（間隔があいていなければ送らない）。 */
    static boolean machineBar(Minecraft mc) {
        if (!WeakSpotConfig.client.hud.machineBarEnabled || !ClientWeakSpotHandler.machineSpotActive()
                || !MachineStates.hasBar(mc.world.getTileEntity(spot().pos))) {
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
        return GrowthBar.progress(mc.world, spot().pos);
    }

    /** 0〜1。出さないときは -1。 */
    static double animalProgress() {
        if (spot() == null || spot().kind != HitKind.ANIMAL || spot().lastActiveTick != ClientWeakSpotHandler.clientTick) {
            return -1;
        }
        AnimalStates.State state = AnimalStates.get(spot().entity.getEntityId(), ClientWeakSpotHandler.clientTick);
        return state == null ? -1 : state.barProgress();
    }
}
