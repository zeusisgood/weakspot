package com.example.weakspot.client;

import com.example.weakspot.common.ComboFactor;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.config.SyncedSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.FOVUpdateEvent;

/**
 * エリトラの弱点（自分だけ。1.7.0）。エリトラで飛んでいる間、照準から 10〜20 度の所に空の青の弱点を出し、照準を
 * 合わせるだけでヒットにする。当てた瞬間に、見ている向きへ一気に飛び出す（トライデントの激流のような急加速）。
 * 速さはクライアントで足す（エリトラの動きは本人のクライアントが決める）。上限は付けない（ユーザーの方針）。
 * 急加速の演出（視野が一瞬広がる、雲の尾、打ち上げの音）は自分の画面だけ。サーバーは検証だけ（MoveHits）。
 * 1.8.6 から AimSpotKind（AimSpots が回す）。
 */
final class ElytraSpot extends AimSpotKind {

    /** 空の青 #7FB2FF。 */
    private static final int RGB = 0x7FB2FF;
    /** 演出の長さ（tick）。視野はこの間に元に戻り、雲の尾もこの間だけ出す。 */
    private static final int DASH_TICKS = 10;
    /** 当てた瞬間の視野の倍率。 */
    private static final float DASH_FOV = 1.25F;
    private static final int SPARKS = 8;
    private static final int CLOUDS_PER_TICK = 2;

    private long dashTick = Long.MIN_VALUE / 2;

    ElytraSpot() {
        super(HitKind.ELYTRA, new HudSpot(HitKind.ELYTRA, RGB).alternateOnly());
    }

    @Override
    void clear() {
        super.clear();
        dashTick = Long.MIN_VALUE / 2;
    }

    @Override
    boolean wanted(EntityPlayerSP player, SyncedSettings settings) {
        return settings.elytraWeakSpotEnabled && player.isElytraFlying();
    }

    @Override
    int placement(EntityPlayerSP player) {
        return HudSpot.FREE;
    }

    @Override
    int minHitInterval(SyncedSettings settings) {
        return settings.elytraMinHitIntervalTicks;
    }

    @Override
    void afterTick(Minecraft mc) {
        long since = ClientWeakSpotHandler.clientTick - dashTick;
        if (since >= 0 && since < DASH_TICKS && mc.player.isElytraFlying()) {
            trail(mc.player);
        }
    }

    /** 見ている向きへ power（ブロック/tick）だけ速さを足す（激流と同じく、今の速さに足す）。 */
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

    /** 急加速のあと、後ろに雲の尾を出す。 */
    private static void trail(EntityPlayerSP player) {
        Vec3d look = player.getLookVec();
        for (int i = 0; i < CLOUDS_PER_TICK; i++) {
            player.world.spawnParticle(EnumParticleTypes.CLOUD, player.posX - look.x, player.posY + 0.5 - look.y,
                    player.posZ - look.z, 0, 0, 0);
        }
    }

    /** 当てた瞬間に視野を DASH_FOV 倍に広げ、DASH_TICKS かけて元に戻す（ease-out）。 */
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
