package com.example.weakspot.client;

import com.example.weakspot.common.BowMath;
import com.example.weakspot.common.FishingMath;
import com.example.weakspot.common.MarkerMotion;
import com.example.weakspot.config.WeakSpotConfig;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;

/**
 * 画面上に一定の大きさで出す、向き（yaw / pitch）で持つ弱点（1.6.0。乗り物・食事の弱点）。弓の弱点（BowSpot）と
 * 同じ描き方と当たり判定（ScreenProjection、FishingMath.allowedAngle）で、照準を合わせるだけで当たる。
 * 「上下だけ」（馬・豚に乗っているとき）のときは pitch だけを持ち、yaw はいつも今の視線に合わせる（進む向きがぶれない）。
 */
final class HudSpot {

    /** 弱点の点を置く、目からの距離（ブロック）。向きだけが意味を持つ。 */
    private static final double SPOT_DISTANCE = 16.0;

    private final ScreenProjection screen = new ScreenProjection();
    private final float[] disk;
    private final float[] ring;
    private final float[] center;
    private final MarkerMotion motion = new MarkerMotion(0, 0);
    private final Random random = new Random();

    private boolean has;
    private boolean vertical;
    private double yaw;
    private double pitch;
    private double eyeX;
    private double eyeY;
    private double eyeZ;
    private double viewYaw;

    /** rgb は円の色。輪と中心は、それを白に寄せた色。 */
    HudSpot(int rgb) {
        disk = rgb(rgb, 0);
        ring = rgb(rgb, 0.5F);
        center = rgb(rgb, 0.8F);
    }

    private static float[] rgb(int rgb, float toWhite) {
        float r = (rgb >> 16 & 0xFF) / 255F;
        float g = (rgb >> 8 & 0xFF) / 255F;
        float b = (rgb & 0xFF) / 255F;
        return new float[] {r + (1 - r) * toWhite, g + (1 - g) * toWhite, b + (1 - b) * toWhite};
    }

    boolean has() {
        return has;
    }

    void clear() {
        has = false;
        screen.invalidate();
    }

    /** まだ出ていなければ、今の視線の近くに出す。 */
    void ensure(EntityPlayer player, boolean verticalOnly) {
        if (has && vertical == verticalOnly) {
            return;
        }
        vertical = verticalOnly;
        next(player, player.rotationYaw, player.rotationPitch);
        motion.jumpTo(yaw, pitch);
        has = true;
    }

    /** 当てたあと、次の位置へ動かす。 */
    void relocate(EntityPlayer player) {
        next(player, yaw, pitch);
        if (WeakSpotConfig.weakSpotTrailEnabled) {
            motion.moveTo(yaw, pitch, Minecraft.getSystemTime());
        } else {
            motion.jumpTo(yaw, pitch);
        }
    }

    private void next(EntityPlayer player, double prevYaw, double prevPitch) {
        if (vertical) {
            yaw = player.rotationYaw;
            pitch = BowMath.nextVerticalPitch(player.rotationPitch, prevPitch, screen.fovDegrees(), random);
        } else {
            double[] n = BowMath.nextSpot(player.rotationYaw, player.rotationPitch, prevYaw, prevPitch,
                    screen.fovDegrees(), random);
            yaw = n[0];
            pitch = n[1];
        }
    }

    /**
     * RenderWorldLastEvent から毎フレーム呼ぶ。行列を覚えて、照準がこの弱点に合っていれば true。
     * 出ていないとき・画面を開いているときは false。
     */
    boolean aimed(Minecraft mc, float partialTicks) {
        screen.invalidate();
        if (!has || mc.player == null) {
            return false;
        }
        EntityPlayer player = mc.player;
        eyeX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        eyeY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks + player.getEyeHeight();
        eyeZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
        viewYaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTicks;
        if (!screen.capture(mc, partialTicks) || mc.currentScreen != null) {
            return false;
        }
        double[] p = project(vertical ? viewYaw : yaw, pitch);
        if (p == null) {
            return false;
        }
        double scale = new ScaledResolution(mc).getScaleFactor();
        double allowed = FishingMath.allowedAngle(FishingMath.SPOT_SCREEN_RADIUS * scale, screen.fovDegrees(),
                screen.viewportHeight());
        return FishingMath.isAimed(p[2], allowed);
    }

    private double[] project(double y, double p) {
        double[] d = BowMath.vector(y, p);
        return screen.project(eyeX + d[0] * SPOT_DISTANCE, eyeY + d[1] * SPOT_DISTANCE, eyeZ + d[2] * SPOT_DISTANCE);
    }

    /** HUD に描く（RenderGameOverlayEvent.Post の中で、beginOverlay と endOverlay の間で呼ぶ）。 */
    void draw(Minecraft mc) {
        if (!has || !screen.isValid()) {
            return;
        }
        double scale = new ScaledResolution(mc).getScaleFactor();
        long nowMs = Minecraft.getSystemTime();
        double radius = FishingMath.SPOT_SCREEN_RADIUS;
        boolean trail = WeakSpotConfig.weakSpotTrailEnabled;
        if (trail) {
            for (MarkerMotion.Afterimage image : motion.afterimages(nowMs)) {
                double[] p = project(vertical ? viewYaw : image.u, image.v);
                if (p != null) {
                    float a = (float) image.alpha(nowMs);
                    ScreenProjection.fill(p[0] / scale, p[1] / scale, radius, disk, 0.35F * a);
                    ScreenProjection.outline(p[0] / scale, p[1] / scale, radius, ring, 0.5F * a);
                }
            }
        }
        double u = yaw;
        double v = pitch;
        if (trail) {
            double[] m = motion.position(nowMs);
            u = m[0];
            v = m[1];
        }
        double[] p = project(vertical ? viewYaw : u, v);
        if (p == null) {
            return;
        }
        double gx = p[0] / scale;
        double gy = p[1] / scale;
        ScreenProjection.fill(gx, gy, radius, disk, 0.45F);
        ScreenProjection.outline(gx, gy, radius, ring, 0.9F);
        ScreenProjection.fill(gx, gy, radius * 0.3, center, 0.9F);
        double head = trail ? motion.headHighlight(nowMs) : 0;
        if (head > 0) {
            ScreenProjection.fill(gx, gy, radius, new float[] {1, 1, 1}, (float) (0.5 * head));
        }
    }

    /** HUD に描く前の状態（BowSpot と同じ）。 */
    static void beginOverlay() {
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableDepth();
        GlStateManager.glLineWidth(2.0F);
    }

    static void endOverlay() {
        GlStateManager.glLineWidth(1.0F);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }
}
