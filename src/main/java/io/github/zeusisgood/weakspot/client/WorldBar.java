package io.github.zeusisgood.weakspot.client;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
* プレイヤーの方を向いた、ワールドの中の横長のバー。値は、プレイヤーから見て左から伸びる。
* 描く側は、テクスチャ・ライティング・カリングを切り、ブレンドを有効にしてから呼ぶ。深度テストはこの中で切って、戻す。
*/
final class WorldBar {

    private WorldBar() {
    }

    /** fraction は 0〜1。fill と back は {r, g, b, a}。 */
    static void draw(double x, double y, double z, double width, double thickness, double fraction,
                     float[] fill, float[] back, double cx, double cy, double cz) {
        double dx = cx - x;
        double dz = cz - z;
        double length = Math.hypot(dx, dz);
        if (length < 1e-6) {
            return;
        }
        // 視線 = カメラ → バーの方向 = (-dx, -dz)。Minecraft では視線 (fx, fz) の右は (-fz, fx)
        double rightX = dz / length;
        double rightZ = -dx / length;

        GlStateManager.disableDepth();
        quad(x, y, z, rightX, rightZ, -width / 2, width / 2, thickness, back, cx, cy, cz);
        double f = Math.max(0, Math.min(1, fraction));
        if (f > 0) {
            quad(x, y, z, rightX, rightZ, -width / 2, -width / 2 + width * f, thickness, fill, cx, cy, cz);
        }
        GlStateManager.enableDepth();
    }

    private static void quad(double x, double y, double z, double rightX, double rightZ, double from, double to,
                             double thickness, float[] rgba, double cx, double cy, double cz) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        double lowY = y - thickness / 2;
        double highY = y + thickness / 2;
        vertex(buffer, x + rightX * from, lowY, z + rightZ * from, rgba, cx, cy, cz);
        vertex(buffer, x + rightX * to, lowY, z + rightZ * to, rgba, cx, cy, cz);
        vertex(buffer, x + rightX * to, highY, z + rightZ * to, rgba, cx, cy, cz);
        vertex(buffer, x + rightX * from, highY, z + rightZ * from, rgba, cx, cy, cz);
        tessellator.draw();
    }

    /** カメラからの相対座標にしてから渡す（ワールド座標が大きいときの float 精度落ちを避ける）。 */
    private static void vertex(BufferBuilder buffer, double x, double y, double z, float[] rgba,
                               double cx, double cy, double cz) {
        buffer.pos(x - cx, y - cy, z - cz).color(rgba[0], rgba[1], rgba[2], rgba[3]).endVertex();
    }
}
