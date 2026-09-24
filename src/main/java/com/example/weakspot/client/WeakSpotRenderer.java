package com.example.weakspot.client;

import com.example.weakspot.common.MarkerColor;
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

/** 弱点の円と、ヒット時に広がって消えるリングを描く。他のプレイヤーのマークは、設定の色と濃さで同じ形に描く。 */
final class WeakSpotRenderer {

    private static final int SEGMENTS = 32;
    private static final double LIFT = 0.003;
    private static final int FADE_TICKS = 10;
    private static final int FLASH_TICKS = 6;

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

    static void render(Minecraft mc, WeakSpot spot, long tick, float partialTicks) {
        Entity camera = mc.getRenderViewEntity();
        if (camera == null) {
            return;
        }
        List<WeakSpot> others = WeakSpotConfig.otherMarkerEnabled && WeakSpotConfig.otherMarkerAlpha > 0
                ? OtherMarkers.visible(camera)
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

        if (!others.isEmpty()) {
            int rgb = MarkerColor.parse(WeakSpotConfig.otherMarkerColor, DEFAULT_OTHER_COLOR);
            float a = (float) WeakSpotConfig.otherMarkerAlpha;
            float[] ring = MarkerColor.towardWhite(rgb, 0.5F);
            float[] center = MarkerColor.towardWhite(rgb, 0.75F);
            for (WeakSpot other : others) {
                drawDisk(other, other.u, other.v, other.radius, cx, cy, cz,
                        (rgb >> 16 & 0xFF) / 255F, (rgb >> 8 & 0xFF) / 255F, (rgb & 0xFF) / 255F, 0.45F * a);
                drawRing(other, other.u, other.v, other.radius, cx, cy, cz, ring[0], ring[1], ring[2], 0.9F * a);
                drawDisk(other, other.u, other.v, other.radius * 0.3, cx, cy, cz,
                        center[0], center[1], center[2], 0.9F * a);
            }
        }
        if (spot != null) {
            float alpha = spotAlpha(spot, tick, partialTicks);
            if (alpha > 0) {
                drawDisk(spot, spot.u, spot.v, spot.radius, cx, cy, cz, 1.0F, 0.35F, 0.15F, 0.45F * alpha);
                drawRing(spot, spot.u, spot.v, spot.radius, cx, cy, cz, 1.0F, 0.9F, 0.4F, 0.9F * alpha);
                drawDisk(spot, spot.u, spot.v, spot.radius * 0.3, cx, cy, cz, 1.0F, 0.95F, 0.7F, 0.9F * alpha);
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
        }

        GlStateManager.glLineWidth(1.0F);
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
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
