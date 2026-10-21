package io.github.zeusisgood.weakspot.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/**
 * 色だけの図形を描く（1.12.2 の Tessellator と GL11 の形に近づけた、1.20.1・1.21.1 版の部品）。
 * 1.17 からは GL_LINE_LOOP がないので、線の輪は LINE_LOOP で始めて endLoop で最初の点に戻す。
 * GUI の頂点は GuiGraphics#pose を掛けてから渡す。ワールドでは null（AFTER_WEATHER ではカメラの向きがモデルビューにもう入っているので、
 * カメラからの相対座標をそのまま）。
 */
final class Draw {

    static final VertexFormat.Mode TRIANGLE_FAN = VertexFormat.Mode.TRIANGLE_FAN;
    static final VertexFormat.Mode TRIANGLE_STRIP = VertexFormat.Mode.TRIANGLE_STRIP;
    static final VertexFormat.Mode TRIANGLES = VertexFormat.Mode.TRIANGLES;
    static final VertexFormat.Mode QUADS = VertexFormat.Mode.QUADS;
    static final VertexFormat.Mode LINE_LOOP = VertexFormat.Mode.DEBUG_LINE_STRIP;
    static final VertexFormat.Mode LINES = VertexFormat.Mode.DEBUG_LINES;
    static final VertexFormat.Mode LINE_STRIP = VertexFormat.Mode.DEBUG_LINE_STRIP;

    private static Matrix4f pose;
    private static double firstX;
    private static double firstY;
    private static double firstZ;
    private static float[] firstColor;

    private Draw() {
    }

    /** 描く前に pose を渡し、描き終えたら null に戻す。 */
    static void setPose(Matrix4f matrix) {
        pose = matrix;
    }

    static BufferBuilder begin(VertexFormat.Mode mode) {
        firstColor = null;
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(mode, DefaultVertexFormat.POSITION_COLOR);
        return buffer;
    }

    static void vertex(BufferBuilder buffer, double x, double y, double z, float r, float g, float b, float a) {
        if (firstColor == null) {
            firstX = x;
            firstY = y;
            firstZ = z;
            firstColor = new float[] {r, g, b, a};
        }
        if (pose != null) {
            buffer.vertex(pose, (float) x, (float) y, (float) z).color(r, g, b, a).endVertex();
        } else {
            buffer.vertex(x, y, z).color(r, g, b, a).endVertex();
        }
    }

    static void end(BufferBuilder buffer) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(buffer.end());
    }

    /** LINE_LOOP で始めたとき。最初の点に戻してから描く。 */
    static void endLoop(BufferBuilder buffer) {
        if (firstColor != null) {
            float[] c = firstColor;
            vertex(buffer, firstX, firstY, firstZ, c[0], c[1], c[2], c[3]);
        }
        end(buffer);
    }

    /** 半透明の図形。depth は深度テスト（ワールドで使う）。 */
    static void beginShapes(boolean depth) {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        if (depth) {
            RenderSystem.enableDepthTest();
        } else {
            RenderSystem.disableDepthTest();
        }
        // HUD の図形は時計回りに頂点を並べるので、カリングを切らないと塗りが消える
        RenderSystem.disableCull();
        RenderSystem.lineWidth(2.0F);
    }

    static void endShapes() {
        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }
}
