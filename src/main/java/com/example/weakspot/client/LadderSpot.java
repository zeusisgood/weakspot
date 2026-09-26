package com.example.weakspot.client;

import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.TimedBoostMath;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.config.WeakSpotConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.MoverType;

/**
 * はしごの弱点（自分だけ。1.7.0）。はしご・ツタ（バニラの isOnLadder に当たるもの）を登り降りしている間、照準の
 * 真上か真下に木の茶色の弱点を出し、照準を合わせるだけでヒットにする（左右に向きを変えると、はしごから外れるため）。
 * 速さはクライアントで足す（プレイヤーの動きはクライアントが決める。ボートと同じ方式）。サーバーは検証だけ（MoveHits）。
 * 1.8.6 から AimSpotKind（AimSpots が回す）。
 */
final class LadderSpot extends AimSpotKind {

    /** 登り降りしているとみなす、1 tick の縦の移動（ブロック）。 */
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

    /** はしごを登り降りしているか（弱点を出す条件と、速さを足す条件）。 */
    private static boolean climbing(EntityPlayerSP player) {
        return player.isOnLadder() && !player.isRiding() && !player.isElytraFlying()
                && Math.abs(player.posY - player.prevPosY) >= MOVING_SPEED;
    }

    @Override
    boolean wanted(EntityPlayerSP player, SyncedSettings settings) {
        return climbing(player);
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

    /**
     * 加速中は、その tick に登り降りした分の (倍率 − 1) 倍を、縦に足す。1 tick に足す量に上限は付けない
     * （ユーザーの方針）。はしごの上では落下の距離を数えない（バニラと同じ）。
     */
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
        return WeakSpotConfig.ladderBoostBarEnabled && boost.isActive(ClientWeakSpotHandler.clientTick)
                && mc.player.isOnLadder();
    }

    @Override
    void drawGauge(Minecraft mc, float partialTicks) {
        HudSpot.gauge(mc, boost.remaining(ClientWeakSpotHandler.clientTick, partialTicks),
                MarkerLook.color(HitKind.LADDER), true);
    }
}
