package io.github.zeusisgood.weakspot.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;

/**
 * プレイヤーの方を向いた、ワールドの中の横長のバー。値は、プレイヤーから見て左から伸びる。
 * 描く側は、カリングを切り、ブレンドを有効にしてから呼ぶ。深度テストはこの中で切って、戻す。
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

        RenderSystem.disableDepthTest();
        quad(x, y, z, rightX, rightZ, -width / 2, width / 2, thickness, back, cx, cy, cz);
        double f = Math.max(0, Math.min(1, fraction));
        if (f > 0) {
            quad(x, y, z, rightX, rightZ, -width / 2, -width / 2 + width * f, thickness, fill, cx, cy, cz);
        }
        RenderSystem.enableDepthTest();
    }

    private static void quad(double x, double y, double z, double rightX, double rightZ, double from, double to,
                             double thickness, float[] rgba, double cx, double cy, double cz) {
        BufferBuilder buffer = Draw.begin(Draw.QUADS);
        double lowY = y - thickness / 2;
        double highY = y + thickness / 2;
        vertex(buffer, x + rightX * from, lowY, z + rightZ * from, rgba, cx, cy, cz);
        vertex(buffer, x + rightX * to, lowY, z + rightZ * to, rgba, cx, cy, cz);
        vertex(buffer, x + rightX * to, highY, z + rightZ * to, rgba, cx, cy, cz);
        vertex(buffer, x + rightX * from, highY, z + rightZ * from, rgba, cx, cy, cz);
        Draw.end(buffer);
    }

    /** カメラからの相対座標にしてから渡す（ワールド座標が大きいときの float 精度落ちを避ける）。 */
    private static void vertex(BufferBuilder buffer, double x, double y, double z, float[] rgba,
                               double cx, double cy, double cz) {
        Draw.vertex(buffer, x - cx, y - cy, z - cz, rgba[0], rgba[1], rgba[2], rgba[3]);
    }
}
