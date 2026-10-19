package io.github.zeusisgood.weakspot.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import io.github.zeusisgood.weakspot.FallDamage;
import io.github.zeusisgood.weakspot.common.ComboFactor;
import io.github.zeusisgood.weakspot.common.FallMath;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * 当てると、落ちた距離を減らす。ダメージはサーバーが着地のときに、サーバーの落ちた距離から決める（ここで減らすのは、
 * 見込みのダメージの表示のため）。見込みの計算は FallDamage（両側で同じ）。
 */
final class FallSpot extends AimSpotKind {

    private static final int STREAK_PARTICLES = 8;
    private static final int LANDING_PARTICLES = 16;
    private static final int HINT_OFFSET = 22;
    private static final int LETHAL_RGB = 0xFF2020;
    private static final int HURT_RGB = 0xFFAA00;
    private static final int SAFE_RGB = 0x55FF55;
    private static final int SURVIVE_RGB = 0xAAFF55;
    /** 縁の幅は、画面の短い辺に対する比率。 */
    private static final float[] EDGE_RGB = {0xB0 / 255F, 0, 0};
    private static final double EDGE_RATIO = 0.12;
    private static final long FLASH_MS = 500;
    private static final int MESSAGE_TICKS = 30;

    private boolean hitThisFall;
    private float lastAirFall;
    private boolean inAir;
    /** 翻訳キー。 */
    private String messageKey;
    private int messageRgb;
    private long messageTick = Long.MIN_VALUE / 2;

    FallSpot() {
        super(HitKind.FALL, new HudSpot(HitKind.FALL));
    }

    @Override
    void clear() {
        super.clear();
        hitThisFall = false;
        inAir = false;
        messageKey = null;
    }

    /** エリトラ・水・溶岩・はしご・乗り物・クリエイティブの飛行は除く。 */
    private static boolean falling(LocalPlayer player) {
        return !player.onGround() && !player.isFallFlying() && !player.isInWater() && !player.isInLava()
                && !player.isPassenger() && !player.onClimbable() && !player.getAbilities().flying;
    }

    /** 落ち始めから出す（落下距離を超えてから出すと、当てるのが間に合わない）。当ててその見込みが下がれば消える。 */
    @Override
    boolean wanted(LocalPlayer player, SyncedSettings settings) {
        return falling(player) && (player.getY() < player.yo || player.fallDistance != 0)
                && FallDamage.landingFall(player) > settings.fallMinDistance;
    }

    @Override
    int placement(LocalPlayer player) {
        return HudSpot.FEET_WINDOW;
    }

    @Override
    void onHit(Minecraft mc, LocalPlayer player, SyncedSettings settings, int streak) {
        FallMath.Outlook before = FallDamage.landingOutlook(player);
        // サーバーと同じく、落ち始めに当てた分は負の「貯め」として着地まで残す
        player.fallDistance = (float) Math.max(FallDamage.MIN_FALL,
                player.fallDistance - settings.fallReduceBlocks * ComboFactor.factor(streak));
        FallMath.Outlook after = FallDamage.landingOutlook(player);
        hitThisFall = true;
        for (int i = 0; i < STREAK_PARTICLES; i++) {
            mc.level.addParticle(ParticleTypes.CLOUD, player.getX() + player.getRandom().nextGaussian() * 0.4,
                    player.getY() + 0.2, player.getZ() + player.getRandom().nextGaussian() * 0.4, 0, 0.3, 0);
        }
        mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.4F, 1.6F, false);
        // 死亡見込みから一気にダメージなしなら「ノーダメージ」だけ
        if (after == FallMath.Outlook.SAFE && before != FallMath.Outlook.SAFE) {
            showMessage("weakspot.fall.noDamage", SAFE_RGB);
            HitSounds.accentRun(ComboEffects.pitches(new int[] {0, 2, 4}),
                    0, 2);
        } else if (after == FallMath.Outlook.HURT && before == FallMath.Outlook.LETHAL) {
            showMessage("weakspot.fall.survive", SURVIVE_RGB);
            HitSounds.accentRun(ComboEffects.pitches(new int[] {2, HitSounds.scale().perOctave()}), 2, 2);
        }
    }

    private void showMessage(String key, int rgb) {
        messageKey = key;
        messageRgb = rgb;
        messageTick = ClientWeakSpotHandler.clientTick;
    }

    private boolean messageShown() {
        return messageKey != null && ClientWeakSpotHandler.clientTick - messageTick < MESSAGE_TICKS;
    }

    @Override
    void tickEnd(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (!player.onGround()) {
            inAir = true;
            lastAirFall = player.fallDistance;
            return;
        }
        if (inAir && hitThisFall && !player.isInWater()
                && FallDamage.outlook(player, lastAirFall) == FallMath.Outlook.SAFE) {
            for (int i = 0; i < LANDING_PARTICLES; i++) {
                double angle = Math.PI * 2 * i / LANDING_PARTICLES;
                mc.level.addParticle(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(),
                        Math.cos(angle) * 0.2, 0.02, Math.sin(angle) * 0.2);
            }
            mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS, 0.5F, 1.2F, false);
        }
        inAir = false;
        hitThisFall = false;
    }

    @Override
    boolean hasGauge(Minecraft mc, float partialTicks) {
        return WeakSpotConfig.client.hud.fallDamageHintEnabled && (spot.has() || messageShown());
    }

    /** 0.35〜1。 */
    private static double flash() {
        double t = (Util.getMillis() % FLASH_MS) / (double) FLASH_MS;
        return 0.35 + 0.65 * (0.5 + 0.5 * Math.cos(t * 2 * Math.PI));
    }

    /** beginOverlay と endOverlay の間で呼ぶ。 */
    @Override
    void drawGauge(Minecraft mc, float partialTicks) {
        if (!spot.has() || FallDamage.landingOutlook(mc.player) != FallMath.Outlook.LETHAL) {
            return;
        }
        double w = mc.getWindow().getGuiScaledWidth();
        double h = mc.getWindow().getGuiScaledHeight();
        double edge = Math.min(w, h) * EDGE_RATIO;
        float a = (float) (0.55 * flash());
        BufferBuilder buffer = Draw.begin(Draw.QUADS);
        // 外側が濃く、内側へ透明に
        quad(buffer, 0, 0, w, 0, w, edge, 0, edge, a, a, 0, 0);
        quad(buffer, 0, h - edge, w, h - edge, w, h, 0, h, 0, 0, a, a);
        quad(buffer, 0, 0, edge, 0, edge, h, 0, h, a, 0, 0, a);
        quad(buffer, w - edge, 0, w, 0, w, h, w - edge, h, 0, a, a, 0);
        Draw.end(buffer);
    }

    private static void quad(BufferBuilder buffer, double x1, double y1, double x2, double y2, double x3, double y3,
                             double x4, double y4, float a1, float a2, float a3, float a4) {
        Draw.vertex(buffer, x1, y1, 0, EDGE_RGB[0], EDGE_RGB[1], EDGE_RGB[2], a1);
        Draw.vertex(buffer, x4, y4, 0, EDGE_RGB[0], EDGE_RGB[1], EDGE_RGB[2], a4);
        Draw.vertex(buffer, x3, y3, 0, EDGE_RGB[0], EDGE_RGB[1], EDGE_RGB[2], a3);
        Draw.vertex(buffer, x2, y2, 0, EDGE_RGB[0], EDGE_RGB[1], EDGE_RGB[2], a2);
    }

    @Override
    void drawAfterOverlay(GuiGraphics graphics, Minecraft mc, float partialTicks) {
        LocalPlayer player = mc.player;
        Font font = mc.font;
        float cx = mc.getWindow().getGuiScaledWidth() / 2F;
        float y = mc.getWindow().getGuiScaledHeight() / 2F - HINT_OFFSET - font.lineHeight / 2F;
        if (spot.has()) {
            double landing = FallDamage.landingFall(player);
            double damage = FallDamage.expected(player, landing);
            FallMath.Outlook outlook = FallDamage.outlook(player, landing);
            String text = (outlook == FallMath.Outlook.LETHAL ? "\u2620 " : "\u2665 ") + FallMath.heartsLabel(damage);
            int rgb = outlook == FallMath.Outlook.LETHAL ? LETHAL_RGB : outlook == FallMath.Outlook.HURT ? HURT_RGB
                    : SAFE_RGB;
            int alpha = outlook == FallMath.Outlook.LETHAL ? (int) (255 * flash()) : 255;
            graphics.drawString(font, text, (int) (cx - font.width(text) / 2F), (int) y, alpha << 24 | rgb, true);
        }
        if (messageShown()) {
            String text = ChatFormatting.BOLD + I18n.get(messageKey);
            double t = ClientWeakSpotHandler.clientTick - messageTick + partialTicks;
            int alpha = (int) Math.max(8, Math.min(255, 255 * (MESSAGE_TICKS - t) / 10.0));
            graphics.pose().pushPose();
            graphics.pose().translate(cx, y - 12, 0);
            graphics.pose().scale(1.5F, 1.5F, 1);
            graphics.drawString(font, text, -font.width(text) / 2, -font.lineHeight / 2, alpha << 24 | messageRgb,
                    true);
            graphics.pose().popPose();
        }
    }
}
