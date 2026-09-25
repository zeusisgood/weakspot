package com.example.weakspot.client;

import com.example.weakspot.common.BlockHealthBar;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.MarkerColor;
import com.example.weakspot.common.MarkerMotion;
import com.example.weakspot.common.MarkerShape;
import com.example.weakspot.config.WeakSpotConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;

/**
 * 弱点の円と、ヒット時に広がって消えるリングを描く。他のプレイヤーのマークは、設定の色と濃さで同じ形に描く。
 * マーカーは表示位置（WeakSpot.motion。移動の演出と残像）に描き、当たり判定の位置（u, v）とは分けている。
 * 掘っているブロックの残りの耐久バーは、自分の採掘の弱点と同じ面の下の余白に、マーカーより先に（下に）描く。
 * 自分の動物の弱点は、体に隠れた部分も薄く透かして描く（animalSpotSeeThrough）。
 */
final class WeakSpotRenderer {

    private static final int SEGMENTS = 32;
    private static final double LIFT = 0.003;
    private static final int FADE_TICKS = 10;
    private static final int FLASH_TICKS = 6;

    private static final float[] OWN_DISK = {1.0F, 0.35F, 0.15F};
    private static final float[] OWN_RING = {1.0F, 0.9F, 0.4F};
    private static final float[] OWN_CENTER = {1.0F, 0.95F, 0.7F};
    /** 動いている最中のマーカーに重ねる白の濃さ（動き始め）。 */
    private static final double HEAD_WHITE = 0.5;
    /** 動物の弱点の、体に隠れた部分を透かして描くときの濃さ（通常の濃さに掛ける）。 */
    private static final float SEE_THROUGH_ALPHA = 0.35F;

    /** 耐久バーの、残りの耐久（緑 #3DDC84）と、バーの全体の背景（黒 #1E1E1E、半透明）。 */
    private static final float[] HEALTH_FILL = {0x3D / 255F, 0xDC / 255F, 0x84 / 255F, 1.0F};
    private static final float[] HEALTH_BACK = {0x1E / 255F, 0x1E / 255F, 0x1E / 255F, 0.5F};
    /** 耐久バーはマーカー（LIFT）より少し低く浮かせる。 */
    private static final double HEALTH_LIFT = 0.002;

    /** 他のプレイヤーのマークの色が読めないときの色（水色 #3FA9FF）。 */
    private static final int DEFAULT_OTHER_COLOR = 0x3FA9FF;

    private static final List<Flash> FLASHES = new ArrayList<>();

    private WeakSpotRenderer() {
    }

    /** ヒットした瞬間の位置を覚えておく演出。 */
    private static final class Flash {
        final WeakSpot spot;
        final double u;
        final double v;
        final long startTick;

        Flash(WeakSpot spot, long startTick) {
            this.spot = spot;
            this.u = spot.u;
            this.v = spot.v;
            this.startTick = startTick;
        }
    }

    static void addFlash(WeakSpot spot, long tick) {
        FLASHES.add(new Flash(spot, tick));
    }

    static void expireFlashes(long tick) {
        FLASHES.removeIf(f -> tick - f.startTick > FLASH_TICKS);
    }

    static void clearFlashes() {
        FLASHES.clear();
    }

    /**
     * health は耐久バーの残りの耐久（0〜1。spot の面に描く）。負ならバーを描かない。
     * growth は作物の成長バーの進み具合（0〜1。spot のブロックの足元に描く）。負ならバーを描かない。
     * animal は動物の足元のバーの進み具合（0〜1。spot の動物の足元に描く）。負ならバーを描かない。
     */
    static void render(Minecraft mc, WeakSpot spot, double health, double growth, double animal, boolean machineBar,
                       long tick, float partialTicks) {
        Entity camera = mc.getRenderViewEntity();
        if (camera == null) {
            return;
        }
        List<WeakSpot> others = WeakSpotConfig.otherMarkerEnabled && WeakSpotConfig.otherMarkerAlpha > 0
                ? OtherMarkers.visible(camera, partialTicks)
                : Collections.emptyList();
        if (spot == null && FLASHES.isEmpty() && others.isEmpty()) {
            return;
        }
        double cx = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * partialTicks;
        double cy = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * partialTicks;
        double cz = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * partialTicks;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.depthMask(false);
        GlStateManager.glLineWidth(2.0F);

        if (spot != null && spot.entity == null && health >= 0) {
            drawHealthBar(spot, health, cx, cy, cz);
        }
        long nowMs = Minecraft.getSystemTime();
        if (!others.isEmpty()) {
            int rgb = MarkerColor.parse(WeakSpotConfig.otherMarkerColor, DEFAULT_OTHER_COLOR);
            float[] disk = {(rgb >> 16 & 0xFF) / 255F, (rgb >> 8 & 0xFF) / 255F, (rgb & 0xFF) / 255F};
            float[] ring = MarkerColor.towardWhite(rgb, 0.5F);
            float[] center = MarkerColor.towardWhite(rgb, 0.75F);
            MarkerShape shape = WeakSpotConfig.otherMarkerShape == null ? MarkerShape.RING
                    : WeakSpotConfig.otherMarkerShape;
            for (WeakSpot other : others) {
                drawMarker(other, shape, disk, ring, center, (float) WeakSpotConfig.otherMarkerAlpha, nowMs,
                        cx, cy, cz);
            }
        }
        if (spot != null) {
            float alpha = spotAlpha(spot, tick, partialTicks);
            if (alpha > 0) {
                // 自分の弱点の色と形は、種類ごとの設定（1.7.0。統計画面の「弱点」タブ）
                float[][] look = MarkerLook.palette(spot.kind, OWN_DISK, OWN_RING, OWN_CENTER);
                MarkerShape shape = MarkerLook.shape(spot.kind);
                drawMarker(spot, shape, look[0], look[1], look[2], alpha, nowMs, cx, cy, cz);
                if (seeThrough(spot)) {
                    GlStateManager.disableDepth();
                    drawMarker(spot, shape, look[0], look[1], look[2],
                            alpha * SEE_THROUGH_ALPHA, nowMs, cx, cy, cz);
                    GlStateManager.enableDepth();
                }
            }
        }
        for (Iterator<Flash> it = FLASHES.iterator(); it.hasNext(); ) {
            Flash flash = it.next();
            float progress = (tick - flash.startTick + partialTicks) / FLASH_TICKS;
            if (progress >= 1) {
                continue;
            }
            double radius = flash.spot.radius * (1 + progress * 1.2);
            drawRing(flash.spot, flash.u, flash.v, radius, cx, cy, cz, 1.0F, 1.0F, 1.0F, 1 - progress);
            if (seeThrough(flash.spot)) {
                GlStateManager.disableDepth();
                drawRing(flash.spot, flash.u, flash.v, radius, cx, cy, cz, 1.0F, 1.0F, 1.0F,
                        (1 - progress) * SEE_THROUGH_ALPHA);
                GlStateManager.enableDepth();
            }
        }

        if (spot != null && growth >= 0) {
            GrowthBar.draw(spot.pos, growth, cx, cy, cz);
        }
        if (spot != null && spot.entity != null && animal >= 0) {
            AnimalBar.draw(spot.entity, animal, partialTicks, cx, cy, cz);
        }
        if (spot != null && machineBar) {
            MachineBars.draw(spot.pos, tick + partialTicks, cx, cy, cz);
        }

        GlStateManager.glLineWidth(1.0F);
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    /**
     * マーカー（円、輪、中心）を表示位置に描く。移動の演出がオンなら、残像と、動いている最中の先頭の明るさも描く。
     * 当たり判定は spot.u, spot.v（移動先）のままで、ここでは見た目だけを動かす。
     */
    private static void drawMarker(WeakSpot spot, MarkerShape shape, float[] disk, float[] ring, float[] center,
                                   float alpha, long nowMs, double cx, double cy, double cz) {
        double u = spot.u;
        double v = spot.v;
        if (WeakSpotConfig.weakSpotTrailEnabled) {
            for (MarkerMotion.Afterimage image : spot.motion.afterimages(nowMs)) {
                float a = (float) image.alpha(nowMs) * alpha;
                drawFill(spot, shape, image.u, image.v, spot.radius, cx, cy, cz, disk, 0.35F * a);
                drawOutline(spot, shape, image.u, image.v, spot.radius, cx, cy, cz, ring, 0.5F * a);
            }
            double[] p = spot.motion.position(nowMs);
            u = p[0];
            v = p[1];
        }
        drawFill(spot, shape, u, v, spot.radius, cx, cy, cz, disk, 0.45F * alpha);
        drawOutline(spot, shape, u, v, spot.radius, cx, cy, cz, ring, 0.9F * alpha);
        if (shape.hasCenterDot()) {
            drawDisk(spot, u, v, spot.radius * 0.3, cx, cy, cz, center[0], center[1], center[2], 0.9F * alpha);
        }
        double head = WeakSpotConfig.weakSpotTrailEnabled ? spot.motion.headHighlight(nowMs) : 0;
        if (head > 0) {
            drawFill(spot, shape, u, v, spot.radius, cx, cy, cz, WHITE, (float) (HEAD_WHITE * head) * alpha);
        }
    }

    private static final float[] WHITE = {1.0F, 1.0F, 1.0F};

    /**
     * 自分の動物の弱点は、体の模型が当たり判定の箱より外に出ていると（ニワトリ、ゾンビの腕など）体に隠れるので、
     * 深度テストを切って薄くもう一度描く。他のプレイヤーのマークは、壁越しに見えてしまうので透かさない。
     */
    private static boolean seeThrough(WeakSpot spot) {
        return spot.entity != null && WeakSpotConfig.animalSpotSeeThrough;
    }

    /** 形の中を塗る。RING は輪の部分（内側の半径との間）だけ塗る。 */
    private static void drawFill(WeakSpot spot, MarkerShape shape, double u, double v, double radius,
                                 double cx, double cy, double cz, float[] rgb, float a) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        double outer = radius * shape.radiusScale;
        if (shape.filled) {
            buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
            vertex(buffer, spot.worldPoint(u, v, LIFT), cx, cy, cz, rgb[0], rgb[1], rgb[2], a);
            for (int i = 0; i <= shape.sides; i++) {
                double[] p = shapePoint(spot, shape, u, v, outer, i);
                vertex(buffer, p, cx, cy, cz, rgb[0], rgb[1], rgb[2], a);
            }
        } else {
            double inner = outer * MarkerShape.RING_INNER_RATIO;
            buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i <= shape.sides; i++) {
                vertex(buffer, shapePoint(spot, shape, u, v, outer, i), cx, cy, cz, rgb[0], rgb[1], rgb[2], a);
                vertex(buffer, shapePoint(spot, shape, u, v, inner, i), cx, cy, cz, rgb[0], rgb[1], rgb[2], a);
            }
        }
        tessellator.draw();
    }

    private static void drawOutline(WeakSpot spot, MarkerShape shape, double u, double v, double radius,
                                    double cx, double cy, double cz, float[] rgb, float a) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < shape.sides; i++) {
            vertex(buffer, shapePoint(spot, shape, u, v, radius * shape.radiusScale, i), cx, cy, cz,
                    rgb[0], rgb[1], rgb[2], a);
        }
        tessellator.draw();
    }

    /** 形の i 番目の頂点（面から少し浮かせる）。 */
    private static double[] shapePoint(WeakSpot spot, MarkerShape shape, double u, double v, double distance, int i) {
        double angle = shape.rotation + 2 * Math.PI * i / shape.sides;
        return spot.worldPoint(u + distance * Math.cos(angle), v + distance * Math.sin(angle), LIFT * 1.2);
    }

    /**
     * 面の「下」の辺に沿って、背景（バーの全体）と、左から残りの耐久の分の緑を描く。
     * 上面・下面の「下」は、プレイヤーに一番近い辺（視点の x, z はカメラの位置と同じ）。
     */
    private static void drawHealthBar(WeakSpot spot, double health, double cx, double cy, double cz) {
        int sign = spot.face.getAxisDirection().getOffset();
        BlockHealthBar bar = BlockHealthBar.place(spot.axis, sign, spot.rect, cx, cz);
        drawQuad(spot, bar.rect(1), HEALTH_LIFT, cx, cy, cz, HEALTH_BACK);
        if (health > 0) {
            drawQuad(spot, bar.rect(health), HEALTH_LIFT * 1.25, cx, cy, cz, HEALTH_FILL);
        }
    }

    /** 面の (u, v) の矩形 {minU, minV, maxU, maxV} を塗る。 */
    private static void drawQuad(WeakSpot spot, double[] r, double lift, double cx, double cy, double cz,
                                 float[] rgba) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        vertex(buffer, spot.worldPoint(r[0], r[1], lift), cx, cy, cz, rgba[0], rgba[1], rgba[2], rgba[3]);
        vertex(buffer, spot.worldPoint(r[2], r[1], lift), cx, cy, cz, rgba[0], rgba[1], rgba[2], rgba[3]);
        vertex(buffer, spot.worldPoint(r[2], r[3], lift), cx, cy, cz, rgba[0], rgba[1], rgba[2], rgba[3]);
        vertex(buffer, spot.worldPoint(r[0], r[3], lift), cx, cy, cz, rgba[0], rgba[1], rgba[2], rgba[3]);
        tessellator.draw();
    }

    /** 長押しをやめた後、残り FADE_TICKS で薄くする。 */
    private static float spotAlpha(WeakSpot spot, long tick, float partialTicks) {
        float idle = tick - spot.lastActiveTick + partialTicks;
        float remaining = ClientSettings.get().lingerTicks - idle;
        if (idle <= 1 || remaining >= FADE_TICKS) {
            return 1;
        }
        return Math.max(0, remaining / FADE_TICKS);
    }

    private static void drawDisk(WeakSpot spot, double u, double v, double radius,
                                 double cx, double cy, double cz, float r, float g, float b, float a) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        vertex(buffer, spot.worldPoint(u, v, LIFT), cx, cy, cz, r, g, b, a);
        for (int i = 0; i <= SEGMENTS; i++) {
            double angle = 2 * Math.PI * i / SEGMENTS;
            double[] p = spot.worldPoint(u + radius * Math.cos(angle), v + radius * Math.sin(angle), LIFT);
            vertex(buffer, p, cx, cy, cz, r, g, b, a);
        }
        tessellator.draw();
    }

    private static void drawRing(WeakSpot spot, double u, double v, double radius,
                                 double cx, double cy, double cz, float r, float g, float b, float a) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < SEGMENTS; i++) {
            double angle = 2 * Math.PI * i / SEGMENTS;
            double[] p = spot.worldPoint(u + radius * Math.cos(angle), v + radius * Math.sin(angle), LIFT * 1.5);
            vertex(buffer, p, cx, cy, cz, r, g, b, a);
        }
        tessellator.draw();
    }

    /** カメラからの相対座標にしてから渡す（ワールド座標が大きいときの float 精度落ちを避ける）。 */
    private static void vertex(BufferBuilder buffer, double[] p, double cx, double cy, double cz,
                               float r, float g, float b, float a) {
        buffer.pos(p[0] - cx, p[1] - cy, p[2] - cz).color(r, g, b, a).endVertex();
    }
}
