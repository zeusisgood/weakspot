package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.BowMath;
import io.github.zeusisgood.weakspot.common.FallMath;
import io.github.zeusisgood.weakspot.common.FishingMath;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.MarkerColor;
import io.github.zeusisgood.weakspot.common.MarkerMotion;
import io.github.zeusisgood.weakspot.common.MarkerShape;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
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
     * 出し方: どこでも（10〜20 度）、上下だけ（yaw は視線に合わせる）、左右だけ（pitch は視線に合わせる。1.8.0。1.11.1 から ±60 度までで止め、yaw のずれを広げる）、
     * 上下だけで yaw は出した時点のまま（弓を馬・豚の上で引いたとき。弓を引く短い間なので、向きを追いかけない。1.8.6 で
     * BowSpot から移した）、足元の方向の枠（FEET_WINDOW。落下）。
     */
    static final int FREE = 0;
    static final int VERTICAL = 1;
    static final int HORIZONTAL = 2;
    static final int VERTICAL_FIXED = 3;
    /**
     * 足元の方向の枠（1.11.0。落下）: 出した時点の向きを覚え、下向き 40〜80 度・左右 ±25 度の枠の中だけに出す
     * （FallMath.nextWindowSpot）。照準から離れても出し直さず、画面にないときは方向の矢印を出す。
     */
    static final int FEET_WINDOW = 4;

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
    /** FEET_WINDOW の枠の中心の向き（出した時点の yaw）。 */
    private double anchorYaw;

    /**
     * defaultRgb は円の初期値の色。輪と中心は、それを白に寄せた色。1.7.0 から、色と形は種類ごとの設定
     * （MarkerLook。統計画面の「弱点マーカー」タブ）で変えられる。
     */
    /** 色の初期値は、その種類の色（HitKind.defaultColor）。 */
    HudSpot(HitKind kind) {
        this(kind, kind.defaultColor());
    }

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
            if (mode == FEET_WINDOW) {
                return;
            }
            // 照準から離れすぎた弱点は、今の照準の近くに出し直す（1.8.2。ヒットには数えない。その場で切り替える）
            boolean tooFar = mode == HORIZONTAL
                    ? BowMath.isTooFarHorizontal(yaw, player.rotationYaw, player.rotationPitch)
                    : BowMath.isTooFar(yaw, pitch, player.rotationYaw, player.rotationPitch, mode != VERTICAL, true);
            if (tooFar) {
                next(player, yaw, pitch);
                motion.jumpTo(yaw, pitch);
            }
            return;
        }
        mode = newMode;
        anchorYaw = player.rotationYaw;
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
        if (WeakSpotConfig.client.markers.weakSpotTrailEnabled) {
            motion.moveTo(yaw, pitch, Minecraft.getSystemTime());
        } else {
            motion.jumpTo(yaw, pitch);
        }
    }

    private void next(EntityPlayer player, double prevYaw, double prevPitch) {
        // 1.8.1: 交互に出し、照準が水平から離れすぎたら水平の側に出す（真上・真下まで行かないように）
        if (mode == FEET_WINDOW) {
            double[] n = FallMath.nextWindowSpot(anchorYaw, prevYaw, prevPitch, random);
            yaw = n[0];
            pitch = n[1];
        } else if (mode == VERTICAL || mode == VERTICAL_FIXED) {
            pitchSide = BowMath.nextPitchSign(player.rotationPitch, pitchSide, keepNearHorizon, random);
            yaw = player.rotationYaw;
            pitch = BowMath.nextVerticalPitch(player.rotationPitch, prevPitch, pitchSide, screen.fovDegrees(),
                    random);
        } else if (mode == HORIZONTAL) {
            yawSide = BowMath.nextYawSign(yawSide, random);
            // 1.11.1: 上下を向くほど左右を広げる（真上・真下で照準の中央に出ないように。pitch は描く時に止める）
            yaw = BowMath.nextWideHorizontalYaw(player.rotationYaw, player.rotationPitch, prevYaw, yawSide,
                    screen.fovDegrees(), random);
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
        double allowed = FishingMath.allowedAngle(radius() * scale, screen.fovDegrees(),
                screen.viewportHeight());
        return FishingMath.isAimed(p[2], allowed);
    }

    /** 弱点の向き (u = yaw, v = pitch) を、出し方に合わせて視線で置き換えてから、画面に写す。 */
    private double[] projectAt(double u, double v) {
        return project(mode == VERTICAL ? viewYaw : u, mode == HORIZONTAL ? BowMath.horizontalPitch(viewPitch) : v);
    }

    private double[] project(double y, double p) {
        double[] d = BowMath.vector(y, p);
        return screen.project(eyeX + d[0] * SPOT_DISTANCE, eyeY + d[1] * SPOT_DISTANCE, eyeZ + d[2] * SPOT_DISTANCE);
    }

    /** 画面の上の半径（GUI の座標。1.11.0 から設定 spotSize と種類ごとの倍率を掛ける）。 */
    private double radius() {
        return FishingMath.SPOT_SCREEN_RADIUS * ClientSettings.get().spotSize(kind);
    }

    private static final float[] ARROW_EDGE = {0x1A / 255F, 0x1A / 255F, 0x1A / 255F};

    /** HUD に描く（RenderGameOverlayEvent.Post の中で、beginOverlay と endOverlay の間で呼ぶ）。 */
    void draw(Minecraft mc) {
        if (!has || !screen.isValid()) {
            return;
        }
        double scale = new ScaledResolution(mc).getScaleFactor();
        long nowMs = Minecraft.getSystemTime();
        double radius = radius();
        boolean trail = WeakSpotConfig.client.markers.weakSpotTrailEnabled;
        int rgb = MarkerLook.color(kind, defaultRgb);
        float[][] look = palette == null ? ScreenProjection.lookOf(rgb)
                : MarkerLook.palette(kind, palette[0], palette[1], palette[2]);
        MarkerShape shape = MarkerLook.shape(kind);
        if (trail) {
            for (MarkerMotion.Afterimage image : motion.afterimages(nowMs)) {
                double[] p = projectAt(image.u, image.v);
                if (p == null) {
                    continue;
                }
                ScreenProjection.drawAfterimage(p[0] / scale, p[1] / scale, radius, shape, look,
                        (float) image.alpha(nowMs));
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
            if (mode == FEET_WINDOW) {
                // 枠の外を向いている: 弱点の方向に矢印（弱点の色に、濃い縁 #1A1A1A）
                ScreenProjection.edgeArrow(new ScaledResolution(mc), BowMath.wrapDegrees(yaw - viewYaw),
                        pitch - viewPitch, look[0], ARROW_EDGE);
            }
            return;
        }
        double gx = p[0] / scale;
        double gy = p[1] / scale;
        ScreenProjection.drawMarker(gx, gy, radius, shape, look, 0.45F, 0.9F);
        ScreenProjection.drawHeadHighlight(gx, gy, radius, shape, trail ? motion.headHighlight(nowMs) : 0);
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
