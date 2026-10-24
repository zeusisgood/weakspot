package io.github.zeusisgood.weakspot.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import io.github.zeusisgood.weakspot.common.ItemRules;
import io.github.zeusisgood.weakspot.common.MarkerColor;
import io.github.zeusisgood.weakspot.common.MarkerShape;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * RenderLevelStageEvent の時点の行列とカメラの位置を onFrame で受け取り、capture で覚えて、そのフレームの HUD の描画と
 * 当たり判定に使う（HUD に画面上で一定の大きさの弱点を描くため）。
 */
final class ScreenProjection {

    private static final int SEGMENTS = 32;

    private static final float[] FRAME_MV = new float[16];
    private static final float[] FRAME_PROJ = new float[16];
    private static Vec3 frameCamera;

    private final float[] mv = new float[16];
    private final float[] proj = new float[16];
    private final int[] vp = new int[4];
    private double camX;
    private double camY;
    private double camZ;
    private boolean valid;

    /** 毎フレーム、ワールドの描画の最後に呼ぶ。camera はカメラ（目）の位置。 */
    static void onFrame(Matrix4f modelView, Matrix4f projection, Vec3 camera) {
        modelView.get(FRAME_MV);
        projection.get(FRAME_PROJ);
        frameCamera = camera;
    }

    /** そのフレームの行列を写す。使えれば true。 */
    boolean capture(Minecraft mc, float partialTicks) {
        valid = false;
        if (frameCamera == null) {
            return false;
        }
        camX = frameCamera.x;
        camY = frameCamera.y;
        camZ = frameCamera.z;
        System.arraycopy(FRAME_MV, 0, mv, 0, 16);
        System.arraycopy(FRAME_PROJ, 0, proj, 0, 16);
        vp[0] = 0;
        vp[1] = 0;
        vp[2] = mc.getWindow().getWidth();
        vp[3] = mc.getWindow().getHeight();
        valid = vp[3] > 0 && proj[5] != 0;
        return valid;
    }

    boolean isValid() {
        return valid;
    }

    void invalidate() {
        valid = false;
    }

    /** 実際のウィンドウのピクセル。 */
    int viewportHeight() {
        return vp[3];
    }

    /** {左端からのピクセル, 上端からのピクセル, 視線との角度（ラジアン）} を返す（実際のウィンドウのピクセル）。カメラの後ろ、画面の外なら null。 */
    double[] project(double wx, double wy, double wz) {
        double x = wx - camX;
        double y = wy - camY;
        double z = wz - camZ;
        double ex = mv[0] * x + mv[4] * y + mv[8] * z + mv[12];
        double ey = mv[1] * x + mv[5] * y + mv[9] * z + mv[13];
        double ez = mv[2] * x + mv[6] * y + mv[10] * z + mv[14];
        if (ez >= -0.05) {
            return null;
        }
        double cx = proj[0] * ex + proj[4] * ey + proj[8] * ez + proj[12];
        double cy = proj[1] * ex + proj[5] * ey + proj[9] * ez + proj[13];
        double cw = proj[3] * ex + proj[7] * ey + proj[11] * ez + proj[15];
        if (cw <= 0) {
            return null;
        }
        double px = (cx / cw + 1) / 2 * vp[2];
        double py = vp[3] - (cy / cw + 1) / 2 * vp[3];
        if (px < 0 || px > vp[2] || py < 0 || py > vp[3]) {
            return null;
        }
        double angle = Math.acos(Math.max(-1, Math.min(1, -ez / Math.sqrt(ex * ex + ey * ey + ez * ez))));
        return new double[] {px, py, angle};
    }

    /** 射影行列の [1][1] が 1 / tan(fov / 2)。弓を引くと狭くなるので、フレームごとに求める。 */
    double fovDegrees() {
        return Math.toDegrees(2 * Math.atan(1.0 / proj[5]));
    }

    static void fill(double x, double y, double radius, float[] rgb, float a) {
        BufferBuilder buffer = Draw.begin(Draw.TRIANGLE_FAN);
        Draw.vertex(buffer, x, y, 0, rgb[0], rgb[1], rgb[2], a);
        for (int i = 0; i <= SEGMENTS; i++) {
            double angle = 2 * Math.PI * i / SEGMENTS;
            Draw.vertex(buffer, x + radius * Math.cos(angle), y + radius * Math.sin(angle), 0, rgb[0], rgb[1], rgb[2], a);
        }
        Draw.end(buffer);
    }

    static void outline(double x, double y, double radius, float[] rgb, float a) {
        BufferBuilder buffer = Draw.begin(Draw.LINE_LOOP);
        for (int i = 0; i < SEGMENTS; i++) {
            double angle = 2 * Math.PI * i / SEGMENTS;
            Draw.vertex(buffer, x + radius * Math.cos(angle), y + radius * Math.sin(angle), 0, rgb[0], rgb[1], rgb[2], a);
        }
        Draw.endLoop(buffer);
    }

    /** 大きさは、ブロックの弱点の WeakSpotRenderer と同じ比率。 */
    static void fillShape(double x, double y, double radius, MarkerShape shape, float[] rgb, float a) {
        if (shape == MarkerShape.CIRCLE) {
            fill(x, y, radius, rgb, a);
            return;
        }
        double outer = radius * shape.radiusScale;
        BufferBuilder buffer;
        if (shape.filled) {
            buffer = Draw.begin(Draw.TRIANGLE_FAN);
            Draw.vertex(buffer, x, y, 0, rgb[0], rgb[1], rgb[2], a);
            for (int i = 0; i <= shape.sides; i++) {
                double angle = shape.rotation + 2 * Math.PI * i / shape.sides;
                double r = outer * shape.pointScale(i);
                Draw.vertex(buffer, x + r * Math.cos(angle), y + r * Math.sin(angle), 0, rgb[0], rgb[1], rgb[2], a);
            }
        } else {
            double inner = outer * MarkerShape.RING_INNER_RATIO;
            buffer = Draw.begin(Draw.TRIANGLE_STRIP);
            for (int i = 0; i <= shape.sides; i++) {
                double angle = shape.rotation + 2 * Math.PI * i / shape.sides;
                Draw.vertex(buffer, x + outer * Math.cos(angle), y + outer * Math.sin(angle), 0, rgb[0], rgb[1], rgb[2], a);
                Draw.vertex(buffer, x + inner * Math.cos(angle), y + inner * Math.sin(angle), 0, rgb[0], rgb[1], rgb[2], a);
            }
        }
        Draw.end(buffer);
    }

    static void outlineShape(double x, double y, double radius, MarkerShape shape, float[] rgb, float a) {
        if (shape == MarkerShape.CIRCLE) {
            outline(x, y, radius, rgb, a);
            return;
        }
        double outer = radius * shape.radiusScale;
        BufferBuilder buffer = Draw.begin(Draw.LINE_LOOP);
        for (int i = 0; i < shape.sides; i++) {
            double angle = shape.rotation + 2 * Math.PI * i / shape.sides;
            double r = outer * shape.pointScale(i);
            Draw.vertex(buffer, x + r * Math.cos(angle), y + r * Math.sin(angle), 0, rgb[0], rgb[1], rgb[2], a);
        }
        Draw.endLoop(buffer);
    }

    /** {disk, ring, center} を返す（中心ほど白に寄せる）。 */
    static float[][] lookOf(int rgb) {
        return new float[][] {MarkerColor.towardWhite(rgb, 0), MarkerColor.towardWhite(rgb, 0.5F),
                MarkerColor.towardWhite(rgb, 0.8F)};
    }

    /** look は {disk, ring, center}。 */
    static void drawMarker(double x, double y, double radius, MarkerShape shape, float[][] look, float diskAlpha,
                           float outlineAlpha) {
        fillShape(x, y, radius, shape, look[0], diskAlpha);
        outlineShape(x, y, radius, shape, look[1], outlineAlpha);
        if (shape.hasCenterDot()) {
            fill(x, y, radius * 0.3, look[2], outlineAlpha);
        }
    }

    /** 片眼鏡の予告: 輪郭だけ。 */
    static void drawForecast(double x, double y, double radius, MarkerShape shape, float[][] look) {
        outlineShape(x, y, radius, shape, look[0], ItemRules.FORECAST_ALPHA);
    }

    /** a は 0〜1。 */
    static void drawAfterimage(double x, double y, double radius, MarkerShape shape, float[][] look, float a) {
        fillShape(x, y, radius, shape, look[0], 0.35F * a);
        outlineShape(x, y, radius, shape, look[1], 0.5F * a);
    }

    /** head は 0〜1。 */
    static void drawHeadHighlight(double x, double y, double radius, MarkerShape shape, double head) {
        if (head > 0) {
            fillShape(x, y, radius, shape, new float[] {1, 1, 1}, (float) (0.5 * head));
        }
    }

    static void rect(double x0, double y0, double x1, double y1, float[] rgba) {
        BufferBuilder buffer = Draw.begin(Draw.QUADS);
        Draw.vertex(buffer, x0, y1, 0, rgba[0], rgba[1], rgba[2], rgba[3]);
        Draw.vertex(buffer, x1, y1, 0, rgba[0], rgba[1], rgba[2], rgba[3]);
        Draw.vertex(buffer, x1, y0, 0, rgba[0], rgba[1], rgba[2], rgba[3]);
        Draw.vertex(buffer, x0, y0, 0, rgba[0], rgba[1], rgba[2], rgba[3]);
        Draw.end(buffer);
    }

    /** 距離は、画面の短い辺に対する比率。 */
    private static final double ARROW_SIZE = 7;
    private static final double ARROW_RADIUS = 0.36;

    /** dx, dy は度（右・下が正）。beginOverlay と endOverlay の間で呼ぶ。 */
    static void edgeArrow(int width, int height, double dx, double dy, float[] c, float[] edge) {
        double len = Math.hypot(dx, dy);
        if (len < 1e-6) {
            return;
        }
        dx /= len;
        dy /= len;
        double r = Math.min(width, height) * ARROW_RADIUS;
        double cx = width / 2.0 + dx * r;
        double cy = height / 2.0 + dy * r;
        double tipX = cx + dx * ARROW_SIZE;
        double tipY = cy + dy * ARROW_SIZE;
        double backX = cx - dx * ARROW_SIZE * 0.6;
        double backY = cy - dy * ARROW_SIZE * 0.6;
        double sideX = -dy * ARROW_SIZE * 0.7;
        double sideY = dx * ARROW_SIZE * 0.7;
        BufferBuilder buffer = Draw.begin(Draw.TRIANGLES);
        Draw.vertex(buffer, tipX, tipY, 0, c[0], c[1], c[2], 0.95F);
        Draw.vertex(buffer, backX + sideX, backY + sideY, 0, c[0], c[1], c[2], 0.95F);
        Draw.vertex(buffer, backX - sideX, backY - sideY, 0, c[0], c[1], c[2], 0.95F);
        Draw.end(buffer);
        buffer = Draw.begin(Draw.LINE_LOOP);
        Draw.vertex(buffer, tipX, tipY, 0, edge[0], edge[1], edge[2], 0.95F);
        Draw.vertex(buffer, backX + sideX, backY + sideY, 0, edge[0], edge[1], edge[2], 0.95F);
        Draw.vertex(buffer, backX - sideX, backY - sideY, 0, edge[0], edge[1], edge[2], 0.95F);
        Draw.endLoop(buffer);
    }
}
