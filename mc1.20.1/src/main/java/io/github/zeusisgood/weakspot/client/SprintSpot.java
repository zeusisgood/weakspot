package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.TimedBoost;
import io.github.zeusisgood.weakspot.common.TimedBoostMath;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import io.github.zeusisgood.weakspot.server.MoveHits;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.ComputeFovModifierEvent;

/**
 * 照準の真上か真下だけに出す（左右に向きを変えると進む向きがぶれるため）。速さはサーバーが移動速度の修正でかける
 * （MoveHits。自分のクライアントにも届いて効く）。ここでは残り時間のゲージと、視野の広がりの抑えだけを受け持つ。
 */
final class SprintSpot extends AimSpotKind {

    /** 1 tick の水平の移動（ブロック）。 */
    private static final double MOVING_SPEED = 0.05;
    /** 見た目だけ。速さには上限を付けない。 */
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
    boolean wanted(LocalPlayer player, SyncedSettings settings) {
        return player.isSprinting() && !player.isPassenger()
                && !player.isFallFlying() && !player.isInWater() && !player.onClimbable()
                && Math.hypot(player.getX() - player.xo, player.getZ() - player.zo) >= MOVING_SPEED;
    }

    @Override
    int keepTicks() {
        return MOVING_KEEP_TICKS;
    }

    @Override
    int placement(LocalPlayer player) {
        return HudSpot.VERTICAL;
    }

    @Override
    void onHit(Minecraft mc, LocalPlayer player, SyncedSettings settings, int streak) {
        boost.start(TimedBoostMath.multiplier(settings.sprintBoostMultiplier, settings.sprintBoostMaxMultiplier,
                streak), settings.sprintBoostDurationTicks, ClientWeakSpotHandler.clientTick);
    }

    /** バニラの視野の広がり（移動速度から決まる）を MAX_FOV までに抑える。修正を除いた速さでの視野より狭くはしない。 */
    @Override
    void onFovUpdate(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeModifier modifier = speed == null ? null : speed.getModifier(MoveHits.SPRINT_MODIFIER);
        if (modifier == null) {
            return;
        }
        float walk = player.getAbilities().getWalkingSpeed();
        if (walk <= 0) {
            return;
        }
        double value = speed.getValue();
        double without = value / (1 + modifier.getAmount());
        // バニラ: 視野 *= (速さ / 歩く速さ + 1) / 2。修正を除いた速さの視野に直す
        float current = event.getNewFovModifier();
        float plain = (float) (current * (without / walk + 1) / (value / walk + 1));
        event.setNewFovModifier(Math.min(current, Math.max(plain, MAX_FOV)));
    }

    @Override
    boolean hasGauge(Minecraft mc, float partialTicks) {
        return WeakSpotConfig.client.hud.sprintBoostBarEnabled && boost.isActive(ClientWeakSpotHandler.clientTick)
                && !mc.player.isPassenger();
    }

    @Override
    void drawGauge(Minecraft mc, float partialTicks) {
        HudSpot.gauge(mc, boost.remainingFraction(ClientWeakSpotHandler.clientTick, partialTicks),
                MarkerLook.color(HitKind.SPRINT), true);
    }
}
