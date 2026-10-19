package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.TimedBoost;
import io.github.zeusisgood.weakspot.common.TimedBoostMath;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.MoverType;

/**
 * 照準の真上か真下だけに出す（左右に向きを変えると、はしごから外れるため）。速さはクライアントで足す
 * （プレイヤーの動きはクライアントが決める）。サーバーは検証だけ（MoveHits）。
 */
final class LadderSpot extends AimSpotKind {

    /** 1 tick の縦の移動（ブロック）。 */
    private static final double MOVING_SPEED = 0.05;

    private final TimedBoost boost = new TimedBoost();

    LadderSpot() {
        super(HitKind.LADDER, new HudSpot(HitKind.LADDER));
    }

    @Override
    void clear() {
        super.clear();
        boost.clear();
    }

    private static boolean climbing(EntityPlayerSP player) {
        return player.isOnLadder() && !player.isRiding() && !player.isElytraFlying()
                && Math.abs(player.posY - player.prevPosY) >= MOVING_SPEED;
    }

    @Override
    boolean wanted(EntityPlayerSP player, SyncedSettings settings) {
        return climbing(player);
    }

    @Override
    int keepTicks() {
        return MOVING_KEEP_TICKS;
    }

    @Override
    int placement(EntityPlayerSP player) {
        return HudSpot.VERTICAL;
    }

    @Override
    void onHit(Minecraft mc, EntityPlayerSP player, SyncedSettings settings, int streak) {
        boost.start(TimedBoostMath.multiplier(settings.ladderBoostMultiplier, settings.ladderBoostMaxMultiplier,
                streak), settings.ladderBoostDurationTicks, ClientWeakSpotHandler.clientTick);
    }

    /** 1 tick に足す量に上限は付けない（速さの上限は付けない方針）。はしごの上では落下の距離を数えない（バニラと同じ）。 */
    @Override
    void tickEnd(Minecraft mc) {
        EntityPlayerSP player = mc.player;
        if (!boost.isActive(ClientWeakSpotHandler.clientTick) || !KindSwitches.isEnabled(HitKind.LADDER)
                || !climbing(player)) {
            return;
        }
        double dy = (player.posY - player.prevPosY) * TimedBoostMath.extra(boost.multiplier());
        if (Math.abs(dy) > 1e-4) {
            player.move(MoverType.SELF, 0, dy, 0);
            if (player.isOnLadder()) {
                player.fallDistance = 0;
            }
        }
    }

    @Override
    boolean hasGauge(Minecraft mc, float partialTicks) {
        return WeakSpotConfig.client.hud.ladderBoostBarEnabled && boost.isActive(ClientWeakSpotHandler.clientTick)
                && mc.player.isOnLadder();
    }

    @Override
    void drawGauge(Minecraft mc, float partialTicks) {
        HudSpot.gauge(mc, boost.remainingFraction(ClientWeakSpotHandler.clientTick, partialTicks),
                MarkerLook.color(HitKind.LADDER), true);
    }
}
