package com.example.weakspot.client;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import com.example.weakspot.common.MarkerColor;
import com.example.weakspot.common.MarkerShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;

/**
 * ワールドの点を、画面上の位置に変換する（HUD に画面上で一定の大きさの弱点を描くため。釣りと弓の弱点が使う）。
 * RenderWorldLastEvent の時点の OpenGL の行列（モデルビュー、射影、ビューポート）とカメラの位置を capture で覚え、
 * そのフレームの HUD の描画と当たり判定に使う。HUD の図形（円、輪、矩形）の描き方も持つ。
 */
final class ScreenProjection {

    private static final int SEGMENTS = 32;

    private final FloatBuffer modelview = GLAllocation.createDirectFloatBuffer(16);
    private final FloatBuffer projection = GLAllocation.createDirectFloatBuffer(16);
    private final IntBuffer viewport = GLAllocation.createDirectIntBuffer(16);
    private final float[] mv = new float[16];
    private final float[] proj = new float[16];
    private final int[] vp = new int[4];
    private double camX;
    private double camY;
    private double camZ;
    /** 最後に capture したフレームの行列などが使えるか。 */
    private boolean valid;

    /** このフレームの行列とカメラの位置（足元。目の高さはモデルビューに入っている）を覚える。使えれば true。 */
    boolean capture(Minecraft mc, float partialTicks) {
        valid = false;
        Entity camera = mc.getRenderViewEntity();
        if (camera == null) {
            return false;
        }
        camX = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * partialTicks;
        camY = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * partialTicks;
        camZ = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * partialTicks;
        modelview.clear();
        projection.clear();
        viewport.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
        modelview.get(mv).rewind();
        projection.get(proj).rewind();
        for (int i = 0; i < 4; i++) {
            vp[i] = viewport.get(i);
        }
        valid = vp[3] > 0 && proj[5] != 0;
        return valid;
    }

    boolean isValid() {
        return valid;
    }

    void invalidate() {
        valid = false;
    }

    /** 画面の高さ（実際のウィンドウのピクセル）。 */
    int viewportHeight() {
        return vp[3];
    }

    /**
     * ワールドの点を画面上の位置に変換する。返すのは {画面の左端からのピクセル, 上端からのピクセル, 視線との角度（ラジアン）}
     * （ピクセルは実際のウィンドウのピクセル）。カメラの後ろ、画面の外なら null。
     */
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

    /** 縦の視野角（度）。射影行列の [1][1] が 1 / tan(fov / 2)。弓を引くと狭くなるので、フレームごとに求める。 */
    double fovDegrees() {
        return Math.toDegrees(2 * Math.atan(1.0 / proj[5]));
    }

    static void fill(double x, double y, double radius, float[] rgb, float a) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x, y, 0).color(rgb[0], rgb[1], rgb[2], a).endVertex();
        for (int i = 0; i <= SEGMENTS; i++) {
            double angle = 2 * Math.PI * i / SEGMENTS;
            buffer.pos(x + radius * Math.cos(angle), y + radius * Math.sin(angle), 0)
                    .color(rgb[0], rgb[1], rgb[2], a).endVertex();
        }
        tessellator.draw();
    }

    static void outline(double x, double y, double radius, float[] rgb, float a) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < SEGMENTS; i++) {
            double angle = 2 * Math.PI * i / SEGMENTS;
            buffer.pos(x + radius * Math.cos(angle), y + radius * Math.sin(angle), 0)
                    .color(rgb[0], rgb[1], rgb[2], a).endVertex();
        }
        tessellator.draw();
    }

    /**
     * 形 shape の中を塗る（1.7.0。自分の弱点の形）。RING は輪の部分だけ塗る。大きさは弱点の半径に合わせる
     * （ブロックの弱点の WeakSpotRenderer と同じ比率）。
     */
    static void fillShape(double x, double y, double radius, MarkerShape shape, float[] rgb, float a) {
        if (shape == MarkerShape.CIRCLE) {
            fill(x, y, radius, rgb, a);
            return;
        }
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        double outer = radius * shape.radiusScale;
        if (shape.filled) {
            buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(x, y, 0).color(rgb[0], rgb[1], rgb[2], a).endVertex();
            for (int i = 0; i <= shape.sides; i++) {
                double angle = shape.rotation + 2 * Math.PI * i / shape.sides;
                buffer.pos(x + outer * Math.cos(angle), y + outer * Math.sin(angle), 0)
                        .color(rgb[0], rgb[1], rgb[2], a).endVertex();
            }
        } else {
            double inner = outer * MarkerShape.RING_INNER_RATIO;
            buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i <= shape.sides; i++) {
                double angle = shape.rotation + 2 * Math.PI * i / shape.sides;
                buffer.pos(x + outer * Math.cos(angle), y + outer * Math.sin(angle), 0)
                        .color(rgb[0], rgb[1], rgb[2], a).endVertex();
                buffer.pos(x + inner * Math.cos(angle), y + inner * Math.sin(angle), 0)
                        .color(rgb[0], rgb[1], rgb[2], a).endVertex();
            }
        }
        tessellator.draw();
    }

    /** 形 shape の輪郭（1.7.0）。 */
    static void outlineShape(double x, double y, double radius, MarkerShape shape, float[] rgb, float a) {
        if (shape == MarkerShape.CIRCLE) {
            outline(x, y, radius, rgb, a);
            return;
        }
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        double outer = radius * shape.radiusScale;
        buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < shape.sides; i++) {
            double angle = shape.rotation + 2 * Math.PI * i / shape.sides;
            buffer.pos(x + outer * Math.cos(angle), y + outer * Math.sin(angle), 0)
                    .color(rgb[0], rgb[1], rgb[2], a).endVertex();
        }
        tessellator.draw();
    }

    /**
     * 画面上のマーカーを 1 つ描く（1.7.0。HUD の弱点と、寝ている間・エンチャントの画面のマーカーで共通）。
     * 形と色は種類ごとの設定（MarkerLook）。alphaScale は全体の濃さ。
     */
    static void marker(double x, double y, double radius, MarkerShape shape, int rgb, float diskAlpha,
                       float alphaScale) {
        float[] disk = MarkerColor.towardWhite(rgb, 0);
        float[] ring = MarkerColor.towardWhite(rgb, 0.5F);
        float[] center = MarkerColor.towardWhite(rgb, 0.8F);
        fillShape(x, y, radius, shape, disk, diskAlpha * alphaScale);
        outlineShape(x, y, radius, shape, ring, 0.9F * alphaScale);
        if (shape.hasCenterDot()) {
            fill(x, y, radius * 0.3, center, 0.9F * alphaScale);
        }
    }

    /** 残像（薄い形と輪郭）。 */
    static void afterimage(double x, double y, double radius, MarkerShape shape, int rgb, float a) {
        fillShape(x, y, radius, shape, MarkerColor.towardWhite(rgb, 0), 0.35F * a);
        outlineShape(x, y, radius, shape, MarkerColor.towardWhite(rgb, 0.5F), 0.5F * a);
    }

    static void rect(double x0, double y0, double x1, double y1, float[] rgba) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x0, y1, 0).color(rgba[0], rgba[1], rgba[2], rgba[3]).endVertex();
        buffer.pos(x1, y1, 0).color(rgba[0], rgba[1], rgba[2], rgba[3]).endVertex();
        buffer.pos(x1, y0, 0).color(rgba[0], rgba[1], rgba[2], rgba[3]).endVertex();
        buffer.pos(x0, y0, 0).color(rgba[0], rgba[1], rgba[2], rgba[3]).endVertex();
        tessellator.draw();
    }
}
