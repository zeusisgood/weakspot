package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.ComboFactor;
import io.github.zeusisgood.weakspot.common.FallMath;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;

/**
 * 落下の弱点（自分だけ。1.11.0）。高い所から落ちている間（落ちた距離が fallMinDistance を超えた）、照準のまわり（10〜20 度）に
 * 雲の白の弱点を出し、照準を合わせるだけでヒットにする。当てると、落ちた距離を fallReduceBlocks × コンボの掛け数だけ減らす
 * （ダメージはサーバーが着地のときに、サーバーの落ちた距離から決める。ここで減らすのは、見込みのダメージの表示のため）。
 * 照準の上に、着地したときの見込みのダメージ（ハート）を出す。当てて、ダメージなしで着地したら「着地成功」の演出。
 */
final class FallSpot extends AimSpotKind {

    private static final int STREAK_PARTICLES = 8;
    private static final int LANDING_PARTICLES = 16;
    private static final int HINT_OFFSET = 22;
    private static final int RED = 0xFF5555;
    private static final int GREEN = 0x55FF55;

    /** この落下で当てたか、着地の直前の落ちた距離。 */
    private boolean hitThisFall;
    private float lastAirFall;
    private boolean inAir;

    FallSpot() {
        super(HitKind.FALL, new HudSpot(HitKind.FALL));
    }

    @Override
    void clear() {
        super.clear();
        hitThisFall = false;
        inAir = false;
    }

    /** 落ちているか（出す条件。エリトラ・水・溶岩・はしご・乗り物・クリエイティブの飛行は除く）。 */
    private static boolean falling(EntityPlayerSP player) {
        return !player.onGround && !player.isElytraFlying() && !player.isInWater() && !player.isInLava()
                && !player.isRiding() && !player.isOnLadder() && !player.capabilities.isFlying;
    }

    @Override
    boolean wanted(EntityPlayerSP player, SyncedSettings settings) {
        return falling(player) && player.fallDistance > settings.fallMinDistance;
    }

    @Override
    int placement(EntityPlayerSP player) {
        return HudSpot.FREE;
    }

    @Override
    void onHit(Minecraft mc, EntityPlayerSP player, SyncedSettings settings, int streak) {
        player.fallDistance = (float) Math.max(0,
                player.fallDistance - settings.fallReduceBlocks * ComboFactor.factor(streak));
        hitThisFall = true;
        for (int i = 0; i < STREAK_PARTICLES; i++) {
            mc.world.spawnParticle(EnumParticleTypes.CLOUD, player.posX + player.getRNG().nextGaussian() * 0.4,
                    player.posY + 0.2, player.posZ + player.getRNG().nextGaussian() * 0.4, 0, 0.3, 0);
        }
        mc.world.playSound(player.posX, player.posY, player.posZ, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                SoundCategory.PLAYERS, 0.4F, 1.6F, false);
    }

    /** 着地を見て、当てていてダメージなしなら「着地成功」。 */
    @Override
    void tickEnd(Minecraft mc) {
        EntityPlayerSP player = mc.player;
        if (!player.onGround) {
            inAir = true;
            lastAirFall = player.fallDistance;
            return;
        }
        if (inAir && hitThisFall && !player.isInWater()
                && FallMath.damagePoints(lastAirFall, jumpBoost(player)) <= 0) {
            for (int i = 0; i < LANDING_PARTICLES; i++) {
                double angle = Math.PI * 2 * i / LANDING_PARTICLES;
                mc.world.spawnParticle(EnumParticleTypes.CLOUD, player.posX, player.posY + 0.1, player.posZ,
                        Math.cos(angle) * 0.2, 0.02, Math.sin(angle) * 0.2);
            }
            mc.world.playSound(player.posX, player.posY, player.posZ, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    SoundCategory.PLAYERS, 0.5F, 1.2F, false);
        }
        inAir = false;
        hitThisFall = false;
    }

    private static int jumpBoost(EntityPlayerSP player) {
        PotionEffect jump = player.getActivePotionEffect(MobEffects.JUMP_BOOST);
        return jump == null ? 0 : jump.getAmplifier() + 1;
    }

    @Override
    boolean hasGauge(Minecraft mc, float partialTicks) {
        return WeakSpotConfig.client.hud.fallDamageHintEnabled && spot.has();
    }

    /** 見込みのダメージ（ハート）を照準の上に出す（ダメージがあれば赤、なければ緑）。 */
    @Override
    void drawAfterOverlay(Minecraft mc, float partialTicks) {
        EntityPlayerSP player = mc.player;
        int points = FallMath.damagePoints(player.fallDistance, jumpBoost(player));
        String text = "♥ " + FallMath.heartsLabel(points);
        FontRenderer font = mc.fontRenderer;
        ScaledResolution res = new ScaledResolution(mc);
        font.drawStringWithShadow(text, res.getScaledWidth() / 2F - font.getStringWidth(text) / 2F,
                res.getScaledHeight() / 2F - HINT_OFFSET - font.FONT_HEIGHT / 2F, points > 0 ? RED : GREEN);
    }
}
