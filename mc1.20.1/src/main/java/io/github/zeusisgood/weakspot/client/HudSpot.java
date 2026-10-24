package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.Trinkets;
import io.github.zeusisgood.weakspot.common.BowMath;
import io.github.zeusisgood.weakspot.common.FallMath;
import io.github.zeusisgood.weakspot.common.FishingMath;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.MarkerColor;
import io.github.zeusisgood.weakspot.common.MarkerMotion;
import io.github.zeusisgood.weakspot.common.MarkerShape;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import java.util.Random;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 向き（yaw / pitch）で持つ、照準のまわりの弱点。画面上に一定の大きさで出し、照準を合わせるだけで当たる。
 * 「上下だけ」のときは pitch だけを持ち、yaw はいつも今の視線に合わせる（進む向きがぶれない）。
 */
final class HudSpot {

    /** ブロック。向きだけが意味を持つ。 */
    private static final double SPOT_DISTANCE = 16.0;

    private final ScreenProjection screen = new ScreenProjection();
    private final HitKind kind;
    private final int defaultRgb;
    private final MarkerMotion motion = new MarkerMotion(0, 0);
    private final Random random = new Random();

    /**
    * 出し方。左右だけは、pitch を視線に合わせて ±60 度で止め、yaw のずれを広げる。
    * VERTICAL_FIXED は、yaw を出した時点のままにする（馬・豚の上で弓を引く短い間なので、向きを追いかけない）。
    */
    static final int FREE = 0;
    static final int VERTICAL = 1;
    static final int HORIZONTAL = 2;
    static final int VERTICAL_FIXED = 3;
    /** 落下: 出した時点の向きを基準にした枠の中だけに出す。照準から離れても出し直さず、画面にないときは方向の矢印を出す。 */
    static final int FEET_WINDOW = 4;

    private boolean has;
    private int mode;
    /** -1 / +1。0 はまだない。次は反対側に出す。 */
    private int pitchSide;
    private int yawSide;
    private boolean keepNearHorizon = true;
    private double yaw;
    private double pitch;
    private double eyeX;
    private double eyeY;
    private double eyeZ;
    private double viewYaw;
    private double viewPitch;
    /** 出した時点の yaw。 */
    private double anchorYaw;
    /** 次に移る向き {yaw, pitch}（片眼鏡の予告）。 */
    private double[] next;

    HudSpot(HitKind kind) {
        this(kind, kind.defaultColor());
    }

    HudSpot(HitKind kind, int defaultRgb) {
        this.kind = kind;
        this.defaultRgb = defaultRgb;
    }

    /** 弓の橙のように、円・輪・中心の初期値の色を別に持つ。色を書き換えたら、その色から作る。 */
    private float[][] palette;

    HudSpot withPalette(float[] disk, float[] ring, float[] center) {
        palette = new float[][] {disk, ring, center};
        return this;
    }

    /** 水平に戻す決まりを使わない（エリトラ）。 */
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

    /** 出し方が変わったときも出し直す。 */
    void ensure(Player player, int newMode) {
        if (has && mode == newMode) {
            if (mode == FEET_WINDOW) {
                return;
            }
            // 照準から離れすぎた弱点は、今の照準の近くに出し直す（ヒットには数えない）
            boolean tooFar = mode == HORIZONTAL
                    ? BowMath.isTooFarHorizontal(yaw, player.getYRot(), player.getXRot())
                    : BowMath.isTooFar(yaw, pitch, player.getYRot(), player.getXRot(), mode != VERTICAL, true);
            if (tooFar) {
                setTo(place(player.getYRot(), player.getXRot(), yaw, pitch));
                motion.jumpTo(yaw, pitch);
                planNext();
            }
            return;
        }
        mode = newMode;
        anchorYaw = player.getYRot();
        setTo(place(player.getYRot(), player.getXRot(), player.getYRot(), player.getXRot()));
        motion.jumpTo(yaw, pitch);
        planNext();
        has = true;
    }

    /** ボートが曲がると、乗っている人の視線も回るので、弱点も一緒に回す。 */
    void rotateYaw(double delta) {
        if (!has || delta == 0) {
            return;
        }
        yaw += delta;
        motion.shift(delta, 0);
        if (next != null) {
            next[0] += delta;
        }
    }

    /** 予告した向きへ移り、その次を決める。 */
    void relocate(Player player) {
        setTo(next != null ? next : place(player.getYRot(), player.getXRot(), yaw, pitch));
        if (WeakSpotConfig.client.markers.weakSpotTrailEnabled) {
            motion.moveTo(yaw, pitch, Util.getMillis());
        } else {
            motion.jumpTo(yaw, pitch);
        }
        planNext();
    }

    private void setTo(double[] p) {
        yaw = p[0];
        pitch = p[1];
    }

    /**
    * 当てたときの視線は今の弱点に合っているので、視線を今の弱点の向きとみなして前もって決める（片眼鏡の予告。
    * 付け外しで移り方が変わらないよう、いつも決めておく）。
    */
    private void planNext() {
        next = place(yaw, pitch, yaw, pitch);
    }

    /** {yaw, pitch}。lookYaw / lookPitch は視線。 */
    private double[] place(double lookYaw, double lookPitch, double prevYaw, double prevPitch) {
        if (mode == FEET_WINDOW) {
            return FallMath.nextWindowSpot(anchorYaw, prevYaw, prevPitch, random);
        } else if (mode == VERTICAL || mode == VERTICAL_FIXED) {
            pitchSide = BowMath.nextPitchSign(lookPitch, pitchSide, keepNearHorizon, random);
            return new double[] {lookYaw,
                    BowMath.nextVerticalPitch(lookPitch, prevPitch, pitchSide, screen.fovDegrees(), random)};
        } else if (mode == HORIZONTAL) {
            yawSide = BowMath.nextYawSign(yawSide, random);
            // pitch は描く時に止める
            return new double[] {BowMath.nextWideHorizontalYaw(lookYaw, lookPitch, prevYaw, yawSide,
                    screen.fovDegrees(), random), lookPitch};
        }
        pitchSide = BowMath.nextPitchSign(lookPitch, pitchSide, keepNearHorizon, random);
        yawSide = BowMath.nextYawSign(yawSide, random);
        return BowMath.nextSpot(lookYaw, lookPitch, prevYaw, prevPitch, yawSide, pitchSide, screen.fovDegrees(),
                random);
    }

    /** 毎フレーム呼ぶ。行列を覚える。出ていないとき・画面を開いているときは false。 */
    boolean aimed(Minecraft mc, float partialTicks) {
        screen.invalidate();
        if (!has || mc.player == null) {
            return false;
        }
        Player player = mc.player;
        Vec3 eye = player.getEyePosition(partialTicks);
        eyeX = eye.x;
        eyeY = eye.y;
        eyeZ = eye.z;
        viewYaw = player.getViewYRot(partialTicks);
        viewPitch = player.getViewXRot(partialTicks);
        if (!screen.capture(mc, partialTicks) || mc.screen != null) {
            return false;
        }
        double[] p = projectAt(yaw, pitch);
        if (p == null) {
            return false;
        }
        double scale = mc.getWindow().getGuiScale();
        double allowed = FishingMath.allowedAngle(radius() * scale, screen.fovDegrees(),
                screen.viewportHeight());
        return FishingMath.isAimed(p[2], allowed);
    }

    /** u = yaw, v = pitch。 */
    private double[] projectAt(double u, double v) {
        return project(mode == VERTICAL ? viewYaw : u, mode == HORIZONTAL ? BowMath.horizontalPitch(viewPitch) : v);
    }

    private double[] project(double y, double p) {
        double[] d = BowMath.vector(y, p);
        return screen.project(eyeX + d[0] * SPOT_DISTANCE, eyeY + d[1] * SPOT_DISTANCE, eyeZ + d[2] * SPOT_DISTANCE);
    }

    /** GUI の座標。 */
    private double radius() {
        return FishingMath.SPOT_SCREEN_RADIUS * OwnSize.of(kind);
    }

    private static final float[] ARROW_EDGE = {0x1A / 255F, 0x1A / 255F, 0x1A / 255F};

    /** beginOverlay と endOverlay の間で呼ぶ。 */
    void draw(Minecraft mc) {
        if (!has || !screen.isValid()) {
            return;
        }
        double scale = mc.getWindow().getGuiScale();
        long nowMs = Util.getMillis();
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
                ScreenProjection.edgeArrow(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(),
                        BowMath.wrapDegrees(yaw - viewYaw),
                        pitch - viewPitch, look[0], ARROW_EDGE);
            }
            return;
        }
        double gx = p[0] / scale;
        double gy = p[1] / scale;
        ScreenProjection.drawMarker(gx, gy, radius, shape, look, 0.45F, 0.9F);
        ScreenProjection.drawHeadHighlight(gx, gy, radius, shape, trail ? motion.headHighlight(nowMs) : 0);
        if (next != null && mc.player != null && Trinkets.monocle(mc.player) && !motion.isMoving(nowMs)) {
            double[] n = projectAt(next[0], next[1]);
            if (n != null) {
                ScreenProjection.drawForecast(n[0] / scale, n[1] / scale, radius, shape, look);
            }
        }
    }

    /** fraction は 0〜1。beginOverlay と endOverlay の間で呼ぶ。 */
    static void gauge(Minecraft mc, double fraction, int rgb, boolean above) {
        double x0 = mc.getWindow().getGuiScaledWidth() / 2.0 - GAUGE_WIDTH / 2.0;
        double cy = mc.getWindow().getGuiScaledHeight() / 2.0 + (above ? -GAUGE_OFFSET : GAUGE_OFFSET);
        double y0 = cy - GAUGE_HEIGHT / 2.0;
        ScreenProjection.rect(x0, y0, x0 + GAUGE_WIDTH, y0 + GAUGE_HEIGHT, GAUGE_BACK);
        float[] fill = MarkerColor.towardWhite(rgb, 0);
        ScreenProjection.rect(x0, y0, x0 + GAUGE_WIDTH * Math.max(0, Math.min(1, fraction)), y0 + GAUGE_HEIGHT,
                new float[] {fill[0], fill[1], fill[2], 1});
    }

    static final int GAUGE_WIDTH = 40;
    static final int GAUGE_HEIGHT = 3;
    static final int GAUGE_OFFSET = 12;
    private static final float[] GAUGE_BACK = {0x1E / 255F, 0x1E / 255F, 0x1E / 255F, 0.5F};

    /** GUI の描画（RenderGuiEvent など）の中で呼ぶ。HUD の図形は時計回りに頂点を並べるので、カリングを切る。 */
    static void beginOverlay() {
        Draw.beginShapes(false);
    }

    static void endOverlay() {
        Draw.endShapes();
    }
}
