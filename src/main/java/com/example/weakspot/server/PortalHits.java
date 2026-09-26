package com.example.weakspot.server;

import com.example.weakspot.Reflect;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.ComboFactor;
import com.example.weakspot.config.WeakSpotConfig;
import java.lang.reflect.Field;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Mod;

/**
 * ネザーゲートの弱点のヒット通知の検証と効果（論理サーバー。1.8.0）。ゲートの中にいる間（非公開の portalCounter が
 * 0 より大きい。ゲートの中で毎 tick 1 増え、出ると 4 ずつ減る）に受け付け、portalCounter に
 * portalHitTicks × コンボの掛け数を足す。getMaxInPortalTime()（サバイバル 80）に達すると、バニラが次の tick に移動する。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class PortalHits {

    /** Entity の非公開の portalCounter（SRG field_82153_h）。読めなければ、ゲートの弱点を出さない。 */
    private static Field portalCounter;
    private static boolean resolved;

    private PortalHits() {
    }

    /** 待ち時間を読み書きできるか（できなければ SyncedSettings で portalWeakSpotEnabled を false にして送る）。 */
    public static synchronized boolean isAvailable() {
        if (!resolved) {
            resolved = true;
            portalCounter = Reflect.field(Entity.class, "the portal weak spot", "portalCounter", "field_82153_h");
        }
        return portalCounter != null;
    }

    public static void onHit(EntityPlayerMP player, int streak) {
        if (!HitGate.allowed(player, HitKind.PORTAL) || player.isRiding()
                || !isAvailable()) {
            return;
        }
        int counter;
        try {
            counter = portalCounter.getInt(player);
        } catch (IllegalAccessException | RuntimeException e) {
            return;
        }
        if (counter <= 0) {
            return;
        }
        if (!HitGate.ready(player, HitKind.PORTAL)) {
            return;
        }
        int combo = HitGate.accept(player, HitKind.PORTAL, new BlockPos(player), streak);
        int added = (int) Math.round(WeakSpotConfig.portalHitTicks * ComboFactor.factor(combo));
        try {
            portalCounter.setInt(player, Math.min(player.getMaxInPortalTime(), counter + added));
        } catch (IllegalAccessException | RuntimeException e) {
            // 読めたのに書けないことは、まずない。ヒットには数えたまま
        }
    }

}
