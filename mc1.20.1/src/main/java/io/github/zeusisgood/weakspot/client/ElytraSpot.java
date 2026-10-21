package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.ComboFactor;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ComputeFovModifierEvent;

/** 速さはクライアントで足す（エリトラの動きは本人のクライアントが決める）。上限は付けない。サーバーは検証だけ（MoveHits）。 */
final class ElytraSpot extends AimSpotKind {

    /** 視野はこの間に元に戻り、雲の尾もこの間だけ出す。 */
    private static final int DASH_TICKS = 10;
    private static final float DASH_FOV = 1.25F;
    private static final int SPARKS = 8;
    private static final int CLOUDS_PER_TICK = 2;

    private long dashTick = Long.MIN_VALUE / 2;

    ElytraSpot() {
        super(HitKind.ELYTRA, new HudSpot(HitKind.ELYTRA).alternateOnly());
    }

    @Override
    void clear() {
        super.clear();
        dashTick = Long.MIN_VALUE / 2;
    }

    @Override
    boolean wanted(LocalPlayer player, SyncedSettings settings) {
        return player.isFallFlying();
    }

    @Override
    int keepTicks() {
        return MOVING_KEEP_TICKS;
    }

    @Override
    int placement(LocalPlayer player) {
        return HudSpot.FREE;
    }

    @Override
    void afterTick(Minecraft mc) {
        long since = ClientWeakSpotHandler.clientTick - dashTick;
        if (since >= 0 && since < DASH_TICKS && mc.player.isFallFlying()) {
            trail(mc.player);
        }
    }

    /** 激流と同じく、今の速さに足す。 */
    @Override
    void onHit(Minecraft mc, LocalPlayer player, SyncedSettings settings, int streak) {
        double power = settings.elytraBoostPower * ComboFactor.factor(streak);
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(player.getDeltaMovement().add(look.x * power, look.y * power, look.z * power));
        dashTick = ClientWeakSpotHandler.clientTick;
        for (int i = 0; i < SPARKS; i++) {
            mc.level.addParticle(ParticleTypes.FIREWORK, player.getX(), player.getY(), player.getZ(),
                    player.getRandom().nextGaussian() * 0.15, player.getRandom().nextGaussian() * 0.15,
                    player.getRandom().nextGaussian() * 0.15);
        }
        // 花火の打ち上げの音を少し低めに鳴らす
        mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH,
                SoundSource.PLAYERS, 1.0F, 0.8F, false);
    }

    private static void trail(LocalPlayer player) {
        Vec3 look = player.getLookAngle();
        for (int i = 0; i < CLOUDS_PER_TICK; i++) {
            player.level().addParticle(ParticleTypes.CLOUD, player.getX() - look.x, player.getY() + 0.5 - look.y,
                    player.getZ() - look.z, 0, 0, 0);
        }
    }

    @Override
    void onFovUpdate(ComputeFovModifierEvent event) {
        long since = ClientWeakSpotHandler.clientTick - dashTick;
        if (since < 0 || since >= DASH_TICKS) {
            return;
        }
        double left = 1 - since / (double) DASH_TICKS;
        event.setNewFovModifier((float) (event.getNewFovModifier() * (1 + (DASH_FOV - 1) * left * left)));
    }
}
