package com.example.weakspot.client;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * ワールドの中に浮かべる、プレイヤーの方を向いた横長のバー。位置（中心）と値を渡せば描ける汎用の部品。
 * 作物の成長バー（GrowthBar）が使い、1.2.0 の動物の足元のバーにも使う。
 *
 * バーは垂直な平面に描き、水平にプレイヤーの方を向く。値は、プレイヤーから見て左から伸びる。
 * 描く側は、テクスチャ・ライティング・カリングを切り、ブレンドを有効にしてから呼ぶ。
 * 他のものに隠れないように、この中で深度テストを切って、戻す。
 */
final class WorldBar {

    private WorldBar() {
    }

    /**
     * 中心 (x, y, z)（ワールド座標）に、幅 width・太さ thickness のバーを描く。
     * fraction は 0〜1（進み具合の色を塗る割合）。カメラ (cx, cy, cz) の方を向く。
     * fill と back は {r, g, b, a}。
     */
    static void draw(double x, double y, double z, double width, double thickness, double fraction,
                     float[] fill, float[] back, double cx, double cy, double cz) {
        double dx = cx - x;
        double dz = cz - z;
        double length = Math.hypot(dx, dz);
        if (length < 1e-6) {
            return;
        }
        // プレイヤーから見た右向き（視線 = カメラ → バーの方向 = (-dx, -dz)。Minecraft では視線 (fx, fz) の右は (-fz, fx)）
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

    /** 中心から右向きに from〜to の範囲、上下に thickness / 2 の矩形を塗る。 */
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
