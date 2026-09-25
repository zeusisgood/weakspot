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
 * 画面上に一定の大きさで出す、向き（yaw / pitch）で持つ弱点（1.6.0。照準のまわりの弱点。1.8.6 から弓も。
 * 種類ごとの処理は AimSpotKind）。描き方と当たり判定は ScreenProjection、FishingMath.allowedAngle で、照準を合わせるだけで当たる。
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

    /**
     * 出し方: どこでも（10〜20 度）、上下だけ（yaw は視線に合わせる）、左右だけ（pitch は視線に合わせる。1.8.0）、
     * 上下だけで yaw は出した時点のまま（弓を馬・豚の上で引いたとき。弓を引く短い間なので、向きを追いかけない。1.8.6 で
     * BowSpot から移した）。
     */
    static final int FREE = 0;
    static final int VERTICAL = 1;
    static final int HORIZONTAL = 2;
    static final int VERTICAL_FIXED = 3;

    private boolean has;
    private int mode;
    /** 前の弱点を出した側（1.8.1。上下 -1 / +1、左右 -1 / +1、0 はまだない）。次は反対側に出す。 */
    private int pitchSide;
    private int yawSide;
    /** 照準が水平から離れすぎたら水平の側に出すか（1.8.1。エリトラだけ false）。 */
    private boolean keepNearHorizon = true;
    private double yaw;
    private double pitch;
    private double eyeX;
    private double eyeY;
    private double eyeZ;
    private double viewYaw;
    private double viewPitch;

    /**
     * defaultRgb は円の初期値の色。輪と中心は、それを白に寄せた色。1.7.0 から、色と形は種類ごとの設定
     * （MarkerLook。統計画面の「弱点マーカー」タブ）で変えられる。
     */
    HudSpot(HitKind kind, int defaultRgb) {
        this.kind = kind;
        this.defaultRgb = defaultRgb;
    }

    /** 円・輪・中心の初期値の色を別に持つ（弓の橙。1.8.6 で BowSpot から移した）。色を書き換えたら、その色から作る。 */
    private float[][] palette;

    HudSpot withPalette(float[] disk, float[] ring, float[] center) {
        palette = new float[][] {disk, ring, center};
        return this;
    }

    /** 上下も左右も交互だけにする（水平に戻す決まりを使わない。1.8.1。エリトラ）。 */
    HudSpot alternateOnly() {
        keepNearHorizon = false;
        return this;
    }

    boolean has() {
        return has;
    }

    void clear() {
        has = false;
        screen.invalidate();
    }

    /** まだ出ていなければ（出し方が変わったときも）、今の視線の近くに newMode の出し方で出す。 */
    void ensure(EntityPlayer player, int newMode) {
        if (has && mode == newMode) {
            // 照準から離れすぎた弱点は、今の照準の近くに出し直す（1.8.2。ヒットには数えない。その場で切り替える）
            if (BowMath.isTooFar(yaw, pitch, player.rotationYaw, player.rotationPitch, mode != VERTICAL,
                    mode != HORIZONTAL)) {
                next(player, yaw, pitch);
                motion.jumpTo(yaw, pitch);
            }
            return;
        }
        mode = newMode;
        next(player, player.rotationYaw, player.rotationPitch);
        motion.jumpTo(yaw, pitch);
        has = true;
    }

    /**
     * 弱点の yaw を delta 度だけ回す（1.8.2。ボートが曲がると、乗っている人の視線も回るので、弱点も一緒に回す）。
     * 表示の移動と残像も、同じだけずらす。
     */
    void rotateYaw(double delta) {
        if (!has || delta == 0) {
            return;
        }
        yaw += delta;
        motion.shift(delta, 0);
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
        // 1.8.1: 交互に出し、照準が水平から離れすぎたら水平の側に出す（真上・真下まで行かないように）
        if (mode == VERTICAL || mode == VERTICAL_FIXED) {
            pitchSide = BowMath.nextPitchSign(player.rotationPitch, pitchSide, keepNearHorizon, random);
            yaw = player.rotationYaw;
            pitch = BowMath.nextVerticalPitch(player.rotationPitch, prevPitch, pitchSide, screen.fovDegrees(),
                    random);
        } else if (mode == HORIZONTAL) {
            yawSide = BowMath.nextYawSign(yawSide, random);
            yaw = BowMath.nextHorizontalYaw(player.rotationYaw, prevYaw, yawSide, screen.fovDegrees(), random);
            pitch = player.rotationPitch;
        } else {
            pitchSide = BowMath.nextPitchSign(player.rotationPitch, pitchSide, keepNearHorizon, random);
            yawSide = BowMath.nextYawSign(yawSide, random);
            double[] n = BowMath.nextSpot(player.rotationYaw, player.rotationPitch, prevYaw, prevPitch, yawSide,
                    pitchSide, screen.fovDegrees(), random);
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
        viewPitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
        if (!screen.capture(mc, partialTicks) || mc.currentScreen != null) {
            return false;
        }
        double[] p = projectAt(yaw, pitch);
        if (p == null) {
            return false;
        }
        double scale = new ScaledResolution(mc).getScaleFactor();
        double allowed = FishingMath.allowedAngle(FishingMath.SPOT_SCREEN_RADIUS * scale, screen.fovDegrees(),
                screen.viewportHeight());
        return FishingMath.isAimed(p[2], allowed);
    }

    /** 弱点の向き (u = yaw, v = pitch) を、出し方に合わせて視線で置き換えてから、画面に写す。 */
    private double[] projectAt(double u, double v) {
        return project(mode == VERTICAL ? viewYaw : u, mode == HORIZONTAL ? viewPitch : v);
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
        float[][] look = palette == null ? null : MarkerLook.palette(kind, palette[0], palette[1], palette[2]);
        MarkerShape shape = MarkerLook.shape(kind);
        if (trail) {
            for (MarkerMotion.Afterimage image : motion.afterimages(nowMs)) {
                double[] p = projectAt(image.u, image.v);
                if (p == null) {
                    continue;
                }
                float a = (float) image.alpha(nowMs);
                if (look == null) {
                    ScreenProjection.afterimage(p[0] / scale, p[1] / scale, radius, shape, rgb, a);
                } else {
                    ScreenProjection.fillShape(p[0] / scale, p[1] / scale, radius, shape, look[0], 0.35F * a);
                    ScreenProjection.outlineShape(p[0] / scale, p[1] / scale, radius, shape, look[1], 0.5F * a);
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
        double[] p = projectAt(u, v);
        if (p == null) {
            return;
        }
        double gx = p[0] / scale;
        double gy = p[1] / scale;
        if (look == null) {
            ScreenProjection.marker(gx, gy, radius, shape, rgb, 0.45F, 1);
        } else {
            ScreenProjection.fillShape(gx, gy, radius, shape, look[0], 0.45F);
            ScreenProjection.outlineShape(gx, gy, radius, shape, look[1], 0.9F);
            if (shape.hasCenterDot()) {
                ScreenProjection.fill(gx, gy, radius * 0.3, look[2], 0.9F);
            }
        }
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
        // HUD の図形は時計回りに頂点を並べるので、カリングを切らないと塗りが消える（1.7.1）
        GlStateManager.disableCull();
        GlStateManager.glLineWidth(2.0F);
    }

    static void endOverlay() {
        GlStateManager.glLineWidth(1.0F);
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }
}
