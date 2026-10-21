package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.common.ComboFactor;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.server.level.ServerPlayer;

/**
 * Entity の portalTime はネザーゲートの中で毎 tick 1 増え、出ると 4 ずつ減る。getPortalWaitTime()（サバイバル 80）に
 * 達すると、バニラが次の tick に移動する（エンドのゲートは数えずにすぐ移動する）。
 */
public final class PortalHits {

    private PortalHits() {
    }

    /** 1.12.2 版はリフレクションで読めないと false。1.20.1・1.21.1 版はいつも使える。 */
    public static boolean isAvailable() {
        return true;
    }

    public static void onHit(ServerPlayer player, int streak) {
        if (!HitGate.allowed(player, HitKind.PORTAL) || player.isPassenger() || player.portalTime <= 0
                || !HitGate.ready(player, HitKind.PORTAL)) {
            return;
        }
        int combo = HitGate.accept(player, HitKind.PORTAL, player.blockPosition(), streak);
        int added = (int) Math.round(WeakSpotConfig.server.portal.portalHitTicks * ComboFactor.factor(combo));
        player.portalTime = Math.min(player.getPortalWaitTime(), player.portalTime + added);
    }
}
