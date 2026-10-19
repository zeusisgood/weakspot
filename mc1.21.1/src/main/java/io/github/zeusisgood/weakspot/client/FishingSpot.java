package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.PlayerRules;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.FishingMath;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.MarkerMotion;
import io.github.zeusisgood.weakspot.common.MarkerShape;
import io.github.zeusisgood.weakspot.common.QueryThrottle;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import io.github.zeusisgood.weakspot.network.HitMessage;
import io.github.zeusisgood.weakspot.network.QueryMessage;
import java.util.Random;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 釣りの弱点（自分だけ。他のプレイヤーには見せない）。左クリックは止めず、通常どおり掘る・攻撃する。
 * 弱点はワールドの 1 点（浮きからの水平の差。浮きが揺れても追従する）で、画面上で一定の大きさの円として描く
 * （浮きが遠くても近くても同じ大きさ）。待ち時間のタイマーはサーバーだけが持つので、浮きが水にある間、問い合わせる。
 */
@EventBusSubscriber(modid = WeakSpotMod.MODID, value = Dist.CLIENT)
final class FishingSpot {

    private static final Random RANDOM = new Random();
    private static final int QUERY_INTERVAL_TICKS = 5;
    /** これより前の返事は古いので使わない。 */
    private static final int STALE_TICKS = 30;
    /** ブロック。 */
    private static final double LIFT = 0.05;

    private static final int BAR_WIDTH = 40;
    private static final int BAR_HEIGHT = 4;
    /** GUI ピクセル。 */
    private static final int BAR_OFFSET = 14;

    private static final float[] DISK = {1.0F, 0.35F, 0.15F};
    private static final float[] RING = {1.0F, 0.9F, 0.4F};
    private static final float[] FILL = {0xFF / 255F, 0xD2 / 255F, 0x3F / 255F, 1.0F};
    private static final float[] BACK = {0x1E / 255F, 0x1E / 255F, 0x1E / 255F, 0.5F};

    // サーバーの返事。問い合わせの相手は自分の浮きだけなので、相手は区別しない
    private static boolean waiting;
    private static float progress;
    private static long receivedTick = Long.MIN_VALUE / 2;
    private static double radius() {
        return FishingMath.SPOT_SCREEN_RADIUS * ClientSettings.get().spotSize(HitKind.FISHING);
    }

    private static final QueryThrottle<Boolean> QUERIES = new QueryThrottle<>(QUERY_INTERVAL_TICKS, false);

    private static FishingHook spotHook;
    private static boolean hasSpot;
    private static double dx;
    private static double dz;
    private static final MarkerMotion MOTION = new MarkerMotion(0, 0);

    // HUD の描画と、左クリックの判定で使う
    private static final ScreenProjection SCREEN = new ScreenProjection();
    private static double hookX;
    private static double hookY;
    private static double hookZ;
    private static boolean aimed;

    private FishingSpot() {
    }

    /** クライアントのスレッドで呼ぶ。 */
    static void receive(boolean isWaiting, float newProgress) {
        waiting = isWaiting;
        progress = newProgress;
        receivedTick = ClientWeakSpotHandler.clientTick;
    }

    static void clear() {
        waiting = false;
        hasSpot = false;
        spotHook = null;
        SCREEN.invalidate();
        aimed = false;
        QUERIES.reset();
    }

    /** なければ null。 */
    private static FishingHook activeHook(Minecraft mc) {
        if (mc.player == null || mc.level == null || !KindSwitches.isEnabled(HitKind.FISHING)
                || !PlayerRules.canUse(mc.player)) {
            return null;
        }
        SyncedSettings settings = ClientSettings.get();
        if (!settings.enabled(HitKind.FISHING) || settings.fishingHits <= 0) {
            return null;
        }
        FishingHook hook = mc.player.fishing;
        boolean holdsRod = mc.player.getMainHandItem().getItem() instanceof FishingRodItem
                || mc.player.getOffhandItem().getItem() instanceof FishingRodItem;
        return hook != null && !hook.isRemoved() && hook.isInWater() && holdsRod ? hook : null;
    }

    private static boolean fresh() {
        return ClientWeakSpotHandler.clientTick - receivedTick <= STALE_TICKS;
    }

    /** 浮きが待ち時間の段階だと、サーバーが最近返したか。 */
    private static boolean shown(Minecraft mc) {
        return activeHook(mc) != null && hasSpot && waiting && fresh();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        FishingHook hook = activeHook(mc);
        if (hook == null || mc.isPaused()) {
            if (hook == null) {
                clear();
            }
            return;
        }
        long tick = ClientWeakSpotHandler.clientTick;
        if (spotHook != hook) {
            spotHook = hook;
            hasSpot = false;
            waiting = false;
            QUERIES.reset();
        }
        query(tick, false);
        if (waiting && fresh() && !hasSpot) {
            double[] p = FishingMath.nextSpot(0, 0, RANDOM);
            dx = p[0];
            dz = p[1];
            MOTION.jumpTo(dx, dz);
            hasSpot = true;
        } else if ((!waiting || !fresh()) && hasSpot) {
            hasSpot = false;
        }
    }

    /** force ならすぐに送る。 */
    private static void query(long tick, boolean force) {
        if (QUERIES.due(Boolean.TRUE, tick, force)) {
            WeakSpotMod.network.sendToServer(QueryMessage.fishing());
        }
    }

    private static double[] worldPoint(double pdx, double pdz) {
        return new double[] {hookX + pdx, hookY + LIFT, hookZ + pdz};
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        SCREEN.invalidate();
        aimed = false;
        if (!shown(mc) || mc.options.hideGui || spotHook == null) {
            return;
        }
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        hookX = Mth.lerp(pt, spotHook.xo, spotHook.getX());
        hookY = Mth.lerp(pt, spotHook.yo, spotHook.getY());
        hookZ = Mth.lerp(pt, spotHook.zo, spotHook.getZ());
        // ClientWeakSpotHandler と同じ段で、どちらが先に呼ばれてもこのフレームの行列を使う
        ScreenProjection.onFrame(event.getModelViewMatrix(), event.getProjectionMatrix(),
                event.getCamera().getPosition());
        if (!SCREEN.capture(mc, pt)) {
            return;
        }
        double[] p = SCREEN.project(hookX + dx, hookY + LIFT, hookZ + dz);
        if (p != null && mc.screen == null) {
            double scale = mc.getWindow().getGuiScale();
            double allowed = FishingMath.allowedAngle(radius() * scale, SCREEN.fovDegrees(),
                    SCREEN.viewportHeight());
            aimed = FishingMath.isAimed(p[2], allowed);
            if (aimed && OwnHits.canHit(HitKind.FISHING, ClientSettings.get().minHitInterval(HitKind.FISHING))) {
                onHit();
            }
        }
    }

    private static void onHit() {
        int streak = OwnHits.register(HitKind.FISHING);
        WeakSpotMod.network.sendToServer(HitMessage.withoutTarget(HitKind.FISHING, streak));
        double[] next = FishingMath.nextSpot(dx, dz, RANDOM);
        dx = next[0];
        dz = next[1];
        if (WeakSpotConfig.client.markers.weakSpotTrailEnabled) {
            MOTION.moveTo(dx, dz, Util.getMillis());
        } else {
            MOTION.jumpTo(dx, dz);
        }
        // 待ち時間が縮んだ分の進み具合を、すぐに見に行く
        query(ClientWeakSpotHandler.clientTick, true);
        aimed = false;
    }

    @SubscribeEvent
    public static void onOverlayPost(RenderGuiEvent.Post event) {
        if (!SCREEN.isValid()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!shown(mc) || mc.options.hideGui) {
            return;
        }
        double scale = mc.getWindow().getGuiScale();
        long nowMs = Util.getMillis();
        double radius = radius();

        Draw.setPose(event.getGuiGraphics().pose().last().pose());
        HudSpot.beginOverlay();

        boolean trail = WeakSpotConfig.client.markers.weakSpotTrailEnabled;
        float[][] look = MarkerLook.palette(HitKind.FISHING, DISK, RING, new float[] {1.0F, 0.95F, 0.7F});
        MarkerShape shape = MarkerLook.shape(HitKind.FISHING);
        if (trail) {
            for (MarkerMotion.Afterimage image : MOTION.afterimages(nowMs)) {
                double[] p = SCREEN.project(hookX + image.u, hookY + LIFT, hookZ + image.v);
                if (p != null) {
                    ScreenProjection.drawAfterimage(p[0] / scale, p[1] / scale, radius, shape, look,
                            (float) image.alpha(nowMs));
                }
            }
        }
        double u = dx;
        double v = dz;
        if (trail) {
            double[] m = MOTION.position(nowMs);
            u = m[0];
            v = m[1];
        }
        double[] p = SCREEN.project(hookX + u, hookY + LIFT, hookZ + v);
        if (p != null) {
            double gx = p[0] / scale;
            double gy = p[1] / scale;
            ScreenProjection.drawMarker(gx, gy, radius, shape, look, 0.45F, 0.9F);
            ScreenProjection.drawHeadHighlight(gx, gy, radius, shape, trail ? MOTION.headHighlight(nowMs) : 0);
        }
        double[] hook = SCREEN.project(hookX, hookY, hookZ);
        if (hook != null) {
            double bx = hook[0] / scale;
            double by = hook[1] / scale + BAR_OFFSET;
            ScreenProjection.rect(bx - BAR_WIDTH / 2.0, by - BAR_HEIGHT / 2.0, bx + BAR_WIDTH / 2.0,
                    by + BAR_HEIGHT / 2.0, BACK);
            double f = Math.max(0, Math.min(1, progress));
            if (f > 0) {
                ScreenProjection.rect(bx - BAR_WIDTH / 2.0, by - BAR_HEIGHT / 2.0,
                        bx - BAR_WIDTH / 2.0 + BAR_WIDTH * f, by + BAR_HEIGHT / 2.0, FILL);
            }
        }

        HudSpot.endOverlay();
        Draw.setPose(null);
    }
}
