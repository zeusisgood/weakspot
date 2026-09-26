package com.example.weakspot.client;

import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.TimedBoostMath;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.server.MoveHits;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraftforge.client.event.FOVUpdateEvent;

/**
 * 走りの弱点（自分だけ。1.7.0）。地面を走っている間、照準の真上か真下に赤の弱点を出し、照準を合わせるだけで
 * ヒットにする（左右に向きを変えると進む向きがぶれるため。馬と同じ）。速さはサーバーが移動速度の修正でかける
 * （MoveHits。自分のクライアントにも届いて効く）。ここでは残り時間のゲージと、視野の広がりの抑えだけを受け持つ。
 * 1.8.6 から AimSpotKind（AimSpots が回す）。
 */
final class SprintSpot extends AimSpotKind {

    /** 走っているとみなす、1 tick の水平の移動（ブロック）。 */
    private static final double MOVING_SPEED = 0.05;
    /** 走りの加速中の、視野の倍率の上限（見た目だけ。速さには上限を付けない）。 */
    private static final float MAX_FOV = 1.3F;

    private final TimedBoost boost = new TimedBoost();

    SprintSpot() {
        super(HitKind.SPRINT, new HudSpot(HitKind.SPRINT));
    }

    @Override
    void clear() {
        super.clear();
        boost.clear();
    }

    @Override
    boolean wanted(EntityPlayerSP player, SyncedSettings settings) {
        return player.isSprinting() && !player.isRiding()
                && !player.isElytraFlying() && !player.isInWater() && !player.isOnLadder()
                && Math.hypot(player.posX - player.prevPosX, player.posZ - player.prevPosZ) >= MOVING_SPEED;
    }

    @Override
    int placement(EntityPlayerSP player) {
        return HudSpot.VERTICAL;
    }


    @Override
    void onHit(Minecraft mc, EntityPlayerSP player, SyncedSettings settings, int streak) {
        boost.start(TimedBoostMath.multiplier(settings.sprintBoostMultiplier, settings.sprintBoostMaxMultiplier,
                streak), settings.sprintBoostDurationTicks, ClientWeakSpotHandler.clientTick);
    }

    /**
     * 走りの加速の修正がかかっている間、バニラの視野の広がり（移動速度から決まる）を MAX_FOV までに抑える。
     * 修正を除いた速さでの視野より狭くはしない。
     */
    @Override
    void onFovUpdate(FOVUpdateEvent event) {
        IAttributeInstance speed = event.getEntity().getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        AttributeModifier modifier = speed == null ? null : speed.getModifier(MoveHits.SPRINT_MODIFIER);
        if (modifier == null) {
            return;
        }
        float walk = event.getEntity().capabilities.getWalkSpeed();
        if (walk <= 0) {
            return;
        }
        double value = speed.getAttributeValue();
        double without = value / (1 + modifier.getAmount());
        // バニラ: 視野 *= (速さ / 歩く速さ + 1) / 2。修正を除いた速さの視野に直す
        float plain = (float) (event.getNewfov() * (without / walk + 1) / (value / walk + 1));
        event.setNewfov(Math.min(event.getNewfov(), Math.max(plain, MAX_FOV)));
    }

    @Override
    boolean hasGauge(Minecraft mc, float partialTicks) {
        return WeakSpotConfig.sprintBoostBarEnabled && boost.isActive(ClientWeakSpotHandler.clientTick)
                && !mc.player.isRiding();
    }

    @Override
    void drawGauge(Minecraft mc, float partialTicks) {
        HudSpot.gauge(mc, boost.remaining(ClientWeakSpotHandler.clientTick, partialTicks),
                MarkerLook.color(HitKind.SPRINT), true);
    }
}
