package com.example.weakspot.client;

import com.example.weakspot.common.BowMath;
import com.example.weakspot.common.FishingMath;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.MarkerColor;
import com.example.weakspot.common.MarkerMotion;
import com.example.weakspot.common.MarkerShape;
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
    private final HitKind kind;
    private final int defaultRgb;
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

    /**
     * defaultRgb は円の初期値の色。輪と中心は、それを白に寄せた色。1.7.0 から、色と形は種類ごとの設定
     * （MarkerLook。統計画面の「弱点」タブ）で変えられる。
     */
    HudSpot(HitKind kind, int defaultRgb) {
        this.kind = kind;
        this.defaultRgb = defaultRgb;
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
        int rgb = MarkerLook.color(kind, defaultRgb);
        MarkerShape shape = MarkerLook.shape(kind);
        if (trail) {
            for (MarkerMotion.Afterimage image : motion.afterimages(nowMs)) {
                double[] p = project(vertical ? viewYaw : image.u, image.v);
                if (p != null) {
                    ScreenProjection.afterimage(p[0] / scale, p[1] / scale, radius, shape, rgb,
                            (float) image.alpha(nowMs));
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
        ScreenProjection.marker(gx, gy, radius, shape, rgb, 0.45F, 1);
        double head = trail ? motion.headHighlight(nowMs) : 0;
        if (head > 0) {
            ScreenProjection.fillShape(gx, gy, radius, shape, new float[] {1, 1, 1}, (float) (0.5 * head));
        }
    }

    /**
     * 照準の上（above）か下に、残り時間・溜めのゲージを描く（1.7.0。乗り物・はしご・走りは上、投げる物は下）。
     * fraction は 0〜1。beginOverlay と endOverlay の間で呼ぶ。
     */
    static void gauge(Minecraft mc, double fraction, int rgb, boolean above) {
        ScaledResolution res = new ScaledResolution(mc);
        double x0 = res.getScaledWidth() / 2.0 - GAUGE_WIDTH / 2.0;
        double cy = res.getScaledHeight() / 2.0 + (above ? -GAUGE_OFFSET : GAUGE_OFFSET);
        double y0 = cy - GAUGE_HEIGHT / 2.0;
        ScreenProjection.rect(x0, y0, x0 + GAUGE_WIDTH, y0 + GAUGE_HEIGHT, GAUGE_BACK);
        float[] fill = MarkerColor.towardWhite(rgb, 0);
        ScreenProjection.rect(x0, y0, x0 + GAUGE_WIDTH * Math.max(0, Math.min(1, fraction)), y0 + GAUGE_HEIGHT,
                new float[] {fill[0], fill[1], fill[2], 1});
    }

    /** ゲージの大きさと、照準の中心からの距離（乗り物のゲージ、弓の引きゲージと同じ）。背景は #1E1E1E 半透明。 */
    static final int GAUGE_WIDTH = 40;
    static final int GAUGE_HEIGHT = 3;
    static final int GAUGE_OFFSET = 12;
    private static final float[] GAUGE_BACK = {0x1E / 255F, 0x1E / 255F, 0x1E / 255F, 0.5F};

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
