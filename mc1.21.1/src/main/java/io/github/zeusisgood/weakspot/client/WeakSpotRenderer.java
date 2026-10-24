package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.Trinkets;
import io.github.zeusisgood.weakspot.common.BlockHealthBar;
import io.github.zeusisgood.weakspot.common.ItemRules;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.MarkerColor;
import io.github.zeusisgood.weakspot.common.MarkerMotion;
import io.github.zeusisgood.weakspot.common.MarkerShape;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

/** マーカーは表示位置（WeakSpot.motion）に描き、当たり判定の位置（u, v）とは分けている。 */
final class WeakSpotRenderer {

    private static final int SEGMENTS = 32;
    private static final double LIFT = 0.003;
    private static final int FADE_TICKS = 10;
    private static final int FLASH_TICKS = 6;

    private static final float[] OWN_DISK = {1.0F, 0.35F, 0.15F};
    private static final float[] OWN_RING = {1.0F, 0.9F, 0.4F};
    private static final float[] OWN_CENTER = {1.0F, 0.95F, 0.7F};
    /** 動き始めの濃さ。 */
    private static final double HEAD_WHITE = 0.5;
    /**
    * 自分の動物の弱点は、体の模型が当たり判定の箱より外に出ていると（ニワトリ、ゾンビの腕など）体に隠れるので、
    * 深度テストを切って、この濃さでもう一度描く。他のプレイヤーのマークは、壁越しに見えてしまうので透かさない。
    */
    private static final float SEE_THROUGH_ALPHA = 0.35F;

    private static final float[] HEALTH_FILL = {0x3D / 255F, 0xDC / 255F, 0x84 / 255F, 1.0F};
    private static final float[] HEALTH_BACK = {0x1E / 255F, 0x1E / 255F, 0x1E / 255F, 0.5F};
    /** マーカー（LIFT）より少し低く。 */
    private static final double HEALTH_LIFT = 0.002;

    /** 色が読めないとき。 */
    private static final int DEFAULT_OTHER_COLOR = 0x3FA9FF;

    private static final List<Flash> FLASHES = new ArrayList<>();

    private WeakSpotRenderer() {
    }

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

    /** health・growth・animal は 0〜1。負ならそのバーを描かない。 */
    static void render(Minecraft mc, WeakSpot spot, double health, double growth, double animal, boolean machineBar,
                       long tick, float partialTicks) {
        Camera camera = mc.gameRenderer.getMainCamera();
        List<WeakSpot> others = WeakSpotConfig.client.markers.otherMarkerEnabled && WeakSpotConfig.client.markers.otherMarkerAlpha > 0
                ? OtherMarkers.visible(camera.getEntity(), partialTicks)
                : Collections.emptyList();
        if (spot == null && FLASHES.isEmpty() && others.isEmpty()) {
            return;
        }
        // ワールドの描画の行列は、カメラ（目）の位置からの相対座標
        double cx = camera.getPosition().x;
        double cy = camera.getPosition().y;
        double cz = camera.getPosition().z;

        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.lineWidth(2.0F);

        if (spot != null && spot.entity == null && health >= 0) {
            drawHealthBar(spot, health, cx, cy, cz);
        }
        long nowMs = Util.getMillis();
        if (!others.isEmpty()) {
            int rgb = MarkerColor.parse(WeakSpotConfig.client.markers.otherMarkerColor, DEFAULT_OTHER_COLOR);
            float[] disk = {(rgb >> 16 & 0xFF) / 255F, (rgb >> 8 & 0xFF) / 255F, (rgb & 0xFF) / 255F};
            float[] ring = MarkerColor.towardWhite(rgb, 0.5F);
            float[] center = MarkerColor.towardWhite(rgb, 0.75F);
            MarkerShape shape = WeakSpotConfig.client.markers.otherMarkerShape == null ? MarkerShape.RING
                    : WeakSpotConfig.client.markers.otherMarkerShape;
            for (WeakSpot other : others) {
                drawMarker(other, shape, disk, ring, center, (float) WeakSpotConfig.client.markers.otherMarkerAlpha, nowMs,
                        cx, cy, cz);
            }
        }
        if (spot != null) {
            float alpha = spotAlpha(spot, tick, partialTicks) * (spot.preview ? PREVIEW_ALPHA : 1);
            if (alpha > 0) {
                float[][] look = MarkerLook.palette(spot.kind, OWN_DISK, OWN_RING, OWN_CENTER);
                MarkerShape shape = MarkerLook.shape(spot.kind);
                if (behindLiquid(mc, spot, partialTicks)) {
                    // 水の面も深度を書くので、そのままでは隠れる
                    RenderSystem.disableDepthTest();
                    drawMarker(spot, shape, look[0], look[1], look[2], alpha, nowMs, cx, cy, cz);
                    RenderSystem.enableDepthTest();
                } else {
                    drawMarker(spot, shape, look[0], look[1], look[2], alpha, nowMs, cx, cy, cz);
                }
                if (seeThrough(spot)) {
                    RenderSystem.disableDepthTest();
                    drawMarker(spot, shape, look[0], look[1], look[2],
                            alpha * SEE_THROUGH_ALPHA, nowMs, cx, cy, cz);
                    RenderSystem.enableDepthTest();
                }
                drawForecast(mc, spot, shape, look[0], alpha, nowMs, cx, cy, cz);
            }
        }
        for (Iterator<Flash> it = FLASHES.iterator(); it.hasNext(); ) {
            Flash flash = it.next();
            float progress = (tick - flash.startTick + partialTicks) / FLASH_TICKS;
            if (progress >= 1) {
                continue;
            }
            double radius = flash.spot.radius * (1 + progress * 1.2);
            boolean liquid = behindLiquid(mc, flash.spot, partialTicks);
            if (liquid) {
                RenderSystem.disableDepthTest();
            }
            drawRing(flash.spot, flash.u, flash.v, radius, cx, cy, cz, 1.0F, 1.0F, 1.0F, 1 - progress);
            if (liquid) {
                RenderSystem.enableDepthTest();
            }
            if (seeThrough(flash.spot)) {
                RenderSystem.disableDepthTest();
                drawRing(flash.spot, flash.u, flash.v, radius, cx, cy, cz, 1.0F, 1.0F, 1.0F,
                        (1 - progress) * SEE_THROUGH_ALPHA);
                RenderSystem.enableDepthTest();
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

        RenderSystem.lineWidth(1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    private static void drawMarker(WeakSpot spot, MarkerShape shape, float[] disk, float[] ring, float[] center,
                                   float alpha, long nowMs, double cx, double cy, double cz) {
        double u = spot.u;
        double v = spot.v;
        if (WeakSpotConfig.client.markers.weakSpotTrailEnabled) {
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
        double head = WeakSpotConfig.client.markers.weakSpotTrailEnabled ? spot.motion.headHighlight(nowMs) : 0;
        if (head > 0) {
            drawFill(spot, shape, u, v, spot.radius, cx, cy, cz, WHITE, (float) (HEAD_WHITE * head) * alpha);
        }
    }

    private static final float[] WHITE = {1.0F, 1.0F, 1.0F};

    /** 片眼鏡の予告: 次に移る位置の輪郭。移動している間は出さない（どちらが今の弱点か紛れないように）。 */
    private static void drawForecast(Minecraft mc, WeakSpot spot, MarkerShape shape, float[] rgb, float alpha,
                                     long nowMs, double cx, double cy, double cz) {
        if (spot.next == null || mc.player == null || !Trinkets.monocle(mc.player) || spot.motion.isMoving(nowMs)) {
            return;
        }
        float a = ItemRules.FORECAST_ALPHA * alpha;
        boolean throughDepth = behindLiquid(mc, spot, 1) || seeThrough(spot);
        if (throughDepth) {
            RenderSystem.disableDepthTest();
        }
        drawOutline(spot, shape, spot.next[0], spot.next[1], spot.radius, cx, cy, cz, rgb, a);
        if (throughDepth) {
            RenderSystem.enableDepthTest();
        }
    }

    /** ブロック。 */
    private static final double LIQUID_STEP = 0.2;

    /**
    * 水の面も深度を書くので、そのまま描くと水面の後ろの弱点が隠れる。照準の先のブロックは、液体のほかに
    * さえぎる物がないことがバニラの判定（objectMouseOver）で決まっているので、このときは深度テストを切って描いてよい。
    */
    private static boolean behindLiquid(Minecraft mc, WeakSpot spot, float partialTicks) {
        if (spot.entity != null || mc.level == null || mc.player == null) {
            return false;
        }
        Vec3 eye = mc.player.getEyePosition(partialTicks);
        double[] p = spot.worldPoint(spot.u, spot.v, 0);
        double dx = p[0] - eye.x;
        double dy = p[1] - eye.y;
        double dz = p[2] - eye.z;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int steps = (int) Math.ceil(length / LIQUID_STEP);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < steps; i++) {
            double t = i / (double) steps;
            pos.set(eye.x + dx * t, eye.y + dy * t, eye.z + dz * t);
            if (!pos.equals(spot.pos) && !mc.level.getFluidState(pos).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean seeThrough(WeakSpot spot) {
        return spot.entity != null && WeakSpotConfig.client.markers.animalSpotSeeThrough;
    }

    private static void drawFill(WeakSpot spot, MarkerShape shape, double u, double v, double radius,
                                 double cx, double cy, double cz, float[] rgb, float a) {
        double outer = radius * shape.radiusScale;
        BufferBuilder buffer;
        if (shape.filled) {
            buffer = Draw.begin(Draw.TRIANGLE_FAN);
            vertex(buffer, spot.worldPoint(u, v, LIFT), cx, cy, cz, rgb[0], rgb[1], rgb[2], a);
            for (int i = 0; i <= shape.sides; i++) {
                double[] p = shapePoint(spot, shape, u, v, outer, i);
                vertex(buffer, p, cx, cy, cz, rgb[0], rgb[1], rgb[2], a);
            }
        } else {
            double inner = outer * MarkerShape.RING_INNER_RATIO;
            buffer = Draw.begin(Draw.TRIANGLE_STRIP);
            for (int i = 0; i <= shape.sides; i++) {
                vertex(buffer, shapePoint(spot, shape, u, v, outer, i), cx, cy, cz, rgb[0], rgb[1], rgb[2], a);
                vertex(buffer, shapePoint(spot, shape, u, v, inner, i), cx, cy, cz, rgb[0], rgb[1], rgb[2], a);
            }
        }
        Draw.end(buffer);
    }

    private static void drawOutline(WeakSpot spot, MarkerShape shape, double u, double v, double radius,
                                    double cx, double cy, double cz, float[] rgb, float a) {
        BufferBuilder buffer = Draw.begin(Draw.LINE_LOOP);
        for (int i = 0; i < shape.sides; i++) {
            vertex(buffer, shapePoint(spot, shape, u, v, radius * shape.radiusScale, i), cx, cy, cz,
                    rgb[0], rgb[1], rgb[2], a);
        }
        Draw.endLoop(buffer);
    }

    /** 面から少し浮かせる。 */
    private static double[] shapePoint(WeakSpot spot, MarkerShape shape, double u, double v, double distance, int i) {
        double angle = shape.rotation + 2 * Math.PI * i / shape.sides;
        distance *= shape.pointScale(i);
        return spot.worldPoint(u + distance * Math.cos(angle), v + distance * Math.sin(angle), LIFT * 1.2);
    }

    /** 上面・下面の「下」は、プレイヤーに一番近い辺（視点の x, z はカメラの位置と同じ）。 */
    private static void drawHealthBar(WeakSpot spot, double health, double cx, double cy, double cz) {
        int sign = spot.face.getAxisDirection().getStep();
        BlockHealthBar bar = BlockHealthBar.place(spot.axis, sign, spot.rect, cx, cz);
        drawQuad(spot, bar.rect(1), HEALTH_LIFT, cx, cy, cz, HEALTH_BACK);
        if (health > 0) {
            drawQuad(spot, bar.rect(health), HEALTH_LIFT * 1.25, cx, cy, cz, HEALTH_FILL);
        }
    }

    private static void drawQuad(WeakSpot spot, double[] r, double lift, double cx, double cy, double cz,
                                 float[] rgba) {
        BufferBuilder buffer = Draw.begin(Draw.QUADS);
        vertex(buffer, spot.worldPoint(r[0], r[1], lift), cx, cy, cz, rgba[0], rgba[1], rgba[2], rgba[3]);
        vertex(buffer, spot.worldPoint(r[2], r[1], lift), cx, cy, cz, rgba[0], rgba[1], rgba[2], rgba[3]);
        vertex(buffer, spot.worldPoint(r[2], r[3], lift), cx, cy, cz, rgba[0], rgba[1], rgba[2], rgba[3]);
        vertex(buffer, spot.worldPoint(r[0], r[3], lift), cx, cy, cz, rgba[0], rgba[1], rgba[2], rgba[3]);
        Draw.end(buffer);
    }

    /** いつも出ているので控えめに。 */
    private static final float PREVIEW_ALPHA = 0.4F;

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
        BufferBuilder buffer = Draw.begin(Draw.TRIANGLE_FAN);
        vertex(buffer, spot.worldPoint(u, v, LIFT), cx, cy, cz, r, g, b, a);
        for (int i = 0; i <= SEGMENTS; i++) {
            double angle = 2 * Math.PI * i / SEGMENTS;
            double[] p = spot.worldPoint(u + radius * Math.cos(angle), v + radius * Math.sin(angle), LIFT);
            vertex(buffer, p, cx, cy, cz, r, g, b, a);
        }
        Draw.end(buffer);
    }

    private static void drawRing(WeakSpot spot, double u, double v, double radius,
                                 double cx, double cy, double cz, float r, float g, float b, float a) {
        BufferBuilder buffer = Draw.begin(Draw.LINE_LOOP);
        for (int i = 0; i < SEGMENTS; i++) {
            double angle = 2 * Math.PI * i / SEGMENTS;
            double[] p = spot.worldPoint(u + radius * Math.cos(angle), v + radius * Math.sin(angle), LIFT * 1.5);
            vertex(buffer, p, cx, cy, cz, r, g, b, a);
        }
        Draw.endLoop(buffer);
    }

    /** カメラからの相対座標にしてから渡す（ワールド座標が大きいときの float 精度落ちを避ける）。 */
    private static void vertex(BufferBuilder buffer, double[] p, double cx, double cy, double cz,
                               float r, float g, float b, float a) {
        Draw.vertex(buffer, p[0] - cx, p[1] - cy, p[2] - cz, r, g, b, a);
    }
}
