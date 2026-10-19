package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.ComboFactor;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.FOVUpdateEvent;

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
    boolean wanted(EntityPlayerSP player, SyncedSettings settings) {
        return player.isElytraFlying();
    }

    @Override
    int keepTicks() {
        return MOVING_KEEP_TICKS;
    }

    @Override
    int placement(EntityPlayerSP player) {
        return HudSpot.FREE;
    }

    @Override
    void afterTick(Minecraft mc) {
        long since = ClientWeakSpotHandler.clientTick - dashTick;
        if (since >= 0 && since < DASH_TICKS && mc.player.isElytraFlying()) {
            trail(mc.player);
        }
    }

    /** 激流と同じく、今の速さに足す。 */
    @Override
    void onHit(Minecraft mc, EntityPlayerSP player, SyncedSettings settings, int streak) {
        double power = settings.elytraBoostPower * ComboFactor.factor(streak);
        Vec3d look = player.getLookVec();
        player.motionX += look.x * power;
        player.motionY += look.y * power;
        player.motionZ += look.z * power;
        dashTick = ClientWeakSpotHandler.clientTick;
        for (int i = 0; i < SPARKS; i++) {
            mc.world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, player.posX, player.posY, player.posZ,
                    player.getRNG().nextGaussian() * 0.15, player.getRNG().nextGaussian() * 0.15,
                    player.getRNG().nextGaussian() * 0.15);
        }
        // 1.12 にはトライデントの音がないので、花火の打ち上げの音を少し低めに鳴らす
        mc.world.playSound(player.posX, player.posY, player.posZ, SoundEvents.ENTITY_FIREWORK_LAUNCH,
                SoundCategory.PLAYERS, 1.0F, 0.8F, false);
    }

    private static void trail(EntityPlayerSP player) {
        Vec3d look = player.getLookVec();
        for (int i = 0; i < CLOUDS_PER_TICK; i++) {
            player.world.spawnParticle(EnumParticleTypes.CLOUD, player.posX - look.x, player.posY + 0.5 - look.y,
                    player.posZ - look.z, 0, 0, 0);
        }
    }

    @Override
    void onFovUpdate(FOVUpdateEvent event) {
        long since = ClientWeakSpotHandler.clientTick - dashTick;
        if (since < 0 || since >= DASH_TICKS) {
            return;
        }
        double left = 1 - since / (double) DASH_TICKS;
        event.setNewfov((float) (event.getNewfov() * (1 + (DASH_FOV - 1) * left * left)));
    }
}
