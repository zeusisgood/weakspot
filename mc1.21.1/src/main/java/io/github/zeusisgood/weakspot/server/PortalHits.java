package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.common.ComboFactor;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PortalProcessor;
import net.minecraft.world.level.block.NetherPortalBlock;

/**
 * PortalProcessor の portalTime はゲートの中で毎 tick 1 増え、出ると 4 ずつ減る。ゲートの getPortalTransitionTime
 * （サバイバル 80）に達すると、バニラが次の tick に移動する。ネザーゲートだけ（エンドのゲートは待たない）。
 */
public final class PortalHits {

    private PortalHits() {
    }

    /** 1.12.2 版はリフレクションで読めないと false。1.21.1 版はいつも使える。 */
    public static boolean isAvailable() {
        return true;
    }

    public static void onHit(ServerPlayer player, int streak) {
        PortalProcessor portal = player.portalProcess;
        if (!HitGate.allowed(player, HitKind.PORTAL) || player.isPassenger() || portal == null
                || !(portal.portal instanceof NetherPortalBlock) || portal.portalTime <= 0
                || !HitGate.ready(player, HitKind.PORTAL)) {
            return;
        }
        int combo = HitGate.accept(player, HitKind.PORTAL, player.blockPosition(), streak);
        int added = (int) Math.round(WeakSpotConfig.server.portal.portalHitTicks * ComboFactor.factor(combo));
        portal.portalTime = Math.min(portal.portal.getPortalTransitionTime(player.serverLevel(), player),
                portal.portalTime + added);
    }
}
