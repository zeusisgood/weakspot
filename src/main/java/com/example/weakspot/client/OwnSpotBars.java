package com.example.weakspot.client;

import com.example.weakspot.common.BlockHealthBar;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.server.MachineStates;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.RayTraceResult;

/**
 * 自分のブロック・動物の弱点に付くバーの値（1.8.9 で ClientWeakSpotHandler から分けた）: 採掘の耐久バー、機械の進み具合、
 * 成長バー、動物の足元のバー。描くのは WeakSpotRenderer。
 */
final class OwnSpotBars {

    private OwnSpotBars() {
    }

    private static WeakSpot spot() {
        return ClientWeakSpotHandler.spot;
    }

    /**
     * 耐久バーに出す、掘っているブロックの残りの耐久（0〜1）。出さないときは -1。
     * 採掘の弱点がこのフレームの照準の面に出ていて（弱点が出るブロックで、クリエイティブでない）、
     * そのブロックを今掘っているときだけ出す。長押しをやめた・壊れたときは、掘っていない扱いになってすぐに消える。
     */
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

    /**
     * 機械の進み具合のバーを出すか（1.6.0）。機械の弱点がかまど・醸造台・スポナーに出ていて、このフレームで照準が
     * 合っているとき。出すときは、サーバーに値を問い合わせる（間隔があいていなければ送らない）。
     */
    static boolean machineBar(Minecraft mc) {
        if (!WeakSpotConfig.machineBarEnabled || !ClientWeakSpotHandler.machineSpotActive()
                || !MachineStates.hasBar(mc.world.getTileEntity(spot().pos))) {
            return false;
        }
        MachineBars.query(spot().pos, ClientWeakSpotHandler.clientTick, false);
        return true;
    }

    /** 成長バーに出す作物の進み具合（0〜1）。成長の弱点が今出ているときだけ。出さないときは -1。 */
    static double growthProgress(Minecraft mc) {
        if (spot() == null || spot().kind != HitKind.GROWTH || spot().lastActiveTick != ClientWeakSpotHandler.clientTick) {
            return -1;
        }
        return GrowthBar.progress(mc.world, spot().pos);
    }

    /** 動物の足元のバーに出す進み具合（0〜1）。動物の弱点が今出ていて、サーバーの返事があるときだけ。出さないときは -1。 */
    static double animalProgress() {
        if (spot() == null || spot().kind != HitKind.ANIMAL || spot().lastActiveTick != ClientWeakSpotHandler.clientTick) {
            return -1;
        }
        AnimalStates.State state = AnimalStates.get(spot().entity.getEntityId(), ClientWeakSpotHandler.clientTick);
        return state == null ? -1 : state.barProgress();
    }
}
