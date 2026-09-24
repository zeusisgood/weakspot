package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.FishingMath;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.MarkerMotion;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.network.FishingQueryMessage;
import com.example.weakspot.network.HitMessage;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.item.ItemFishingRod;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 釣りの弱点（自分だけ。他のプレイヤーには見せない）。浮きが水に入って魚を待っている間、浮きのまわり（半径1ブロック）の
 * 水面の上の点に弱点を出し、釣り竿を持って、照準が重なっているときに左クリックで叩く。
 *
 * 弱点はワールドの1点（浮きからの水平の差 dx, dz。浮きが揺れても追従する）で、描くときに画面上の位置へ変換して、
 * 画面上で一定の大きさ（半径 12 GUI ピクセル）の円として描く（浮きが遠くても近くても同じ大きさ）。
 * 当たり判定は、視線と「目から弱点への向き」の角度の差が、画面上の円の半径・視野角・画面の高さから求めた角度より小さいこと。
 * 変換に使う行列は、RenderWorldLastEvent の時点の OpenGL の行列（視野角は射影行列から求める。ScreenProjection）。
 * 待ち時間のタイマーはサーバーだけが持つので、浮きが水にある間、サーバーに状態を問い合わせる（FishingQueryMessage）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
final class FishingSpot {

    private static final Random RANDOM = new Random();
    /** 問い合わせの間隔（tick）。 */
    private static final int QUERY_INTERVAL_TICKS = 5;
    /** この tick 以上前の返事は古いので使わない。 */
    private static final int STALE_TICKS = 30;
    /** 弱点を水面から少し浮かせる（ブロック）。 */
    private static final double LIFT = 0.05;

    private static final int BAR_WIDTH = 40;
    private static final int BAR_HEIGHT = 4;
    /** 浮きの画面上の位置から、バーの中心までの下向きの距離（GUI ピクセル）。 */
    private static final int BAR_OFFSET = 14;

    private static final float[] DISK = {1.0F, 0.35F, 0.15F};
    private static final float[] RING = {1.0F, 0.9F, 0.4F};
    private static final float[] FILL = {0xFF / 255F, 0xD2 / 255F, 0x3F / 255F, 1.0F};
    private static final float[] BACK = {0x1E / 255F, 0x1E / 255F, 0x1E / 255F, 0.5F};

    // サーバーの返事
    private static boolean waiting;
    private static float progress;
    private static long receivedTick = Long.MIN_VALUE / 2;
    private static long lastQueryTick = Long.MIN_VALUE / 2;

    // 弱点（浮きからの水平の差。当たり判定の位置と、表示の位置 motion）
    private static EntityFishHook spotHook;
    private static boolean hasSpot;
    private static double dx;
    private static double dz;
    private static final MarkerMotion MOTION = new MarkerMotion(0, 0);

    // 最後に描いたフレームの行列など（HUD の描画と、左クリックの判定で使う）
    private static final ScreenProjection SCREEN = new ScreenProjection();
    private static double hookX;
    private static double hookY;
    private static double hookZ;
    /** このフレーム、照準が弱点に重なっているか。 */
    private static boolean aimed;

    private FishingSpot() {
    }

    /** サーバーの返事が届いた（クライアントのスレッドで呼ぶ）。 */
    static void receive(boolean isWaiting, float newProgress) {
        waiting = isWaiting;
        progress = newProgress;
        receivedTick = ClientWeakSpotHandler.clientTick;
    }

    /** 弱点の一時オフ、ワールドを出たとき。 */
    static void clear() {
        waiting = false;
        hasSpot = false;
        spotHook = null;
        SCREEN.invalidate();
        aimed = false;
        lastQueryTick = Long.MIN_VALUE / 2;
    }

    /** 自分の、弱点を出せる浮き（釣り竿を持っていて、水に浮いている）。なければ null。 */
    private static EntityFishHook activeHook(Minecraft mc) {
        if (mc.player == null || mc.world == null || !WeakSpotConfig.weakSpotsEnabled
                || mc.player.capabilities.isCreativeMode || mc.player.isSpectator()) {
            return null;
        }
        SyncedSettings settings = ClientSettings.get();
        if (!settings.fishingWeakSpotEnabled || settings.fishingHits <= 0) {
            return null;
        }
        EntityFishHook hook = mc.player.fishEntity;
        boolean holdsRod = mc.player.getHeldItemMainhand().getItem() instanceof ItemFishingRod
                || mc.player.getHeldItemOffhand().getItem() instanceof ItemFishingRod;
        return hook != null && !hook.isDead && hook.isInWater() && holdsRod ? hook : null;
    }

    private static boolean fresh() {
        return ClientWeakSpotHandler.clientTick - receivedTick <= STALE_TICKS;
    }

    /** 弱点が今出ているか（浮きが待ち時間の段階だと、サーバーが最近返した）。 */
    private static boolean shown(Minecraft mc) {
        return activeHook(mc) != null && hasSpot && waiting && fresh();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityFishHook hook = activeHook(mc);
        if (hook == null || mc.isGamePaused()) {
            if (hook == null) {
                clear();
            }
            return;
        }
        long tick = ClientWeakSpotHandler.clientTick;
        if (spotHook != hook) {
            // 新しく投げた浮き
            spotHook = hook;
            hasSpot = false;
            waiting = false;
            lastQueryTick = Long.MIN_VALUE / 2;
        }
        if (tick - lastQueryTick >= QUERY_INTERVAL_TICKS) {
            query(tick);
        }
        if (waiting && fresh() && !hasSpot) {
            // 待ち時間の段階に入った。浮きから離れた水面の点に出す
            double[] p = FishingMath.nextSpot(0, 0, RANDOM);
            dx = p[0];
            dz = p[1];
            MOTION.jumpTo(dx, dz);
            hasSpot = true;
        } else if ((!waiting || !fresh()) && hasSpot) {
            hasSpot = false;
        }
    }

    private static void query(long tick) {
        lastQueryTick = tick;
        WeakSpotMod.network.sendToServer(new FishingQueryMessage());
    }

    /** 弱点の点のワールド座標（浮きのまわり。水面の少し上）。 */
    private static double[] worldPoint(double pdx, double pdz) {
        return new double[] {hookX + pdx, hookY + LIFT, hookZ + pdz};
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        SCREEN.invalidate();
        aimed = false;
        if (!shown(mc) || mc.getRenderViewEntity() == null || mc.gameSettings.hideGUI || spotHook == null) {
            return;
        }
        float pt = event.getPartialTicks();
        hookX = spotHook.lastTickPosX + (spotHook.posX - spotHook.lastTickPosX) * pt;
        hookY = spotHook.lastTickPosY + (spotHook.posY - spotHook.lastTickPosY) * pt;
        hookZ = spotHook.lastTickPosZ + (spotHook.posZ - spotHook.lastTickPosZ) * pt;
        if (!SCREEN.capture(mc, pt)) {
            return;
        }
        double[] p = SCREEN.project(hookX + dx, hookY + LIFT, hookZ + dz);
        if (p != null && mc.currentScreen == null) {
            double scale = new ScaledResolution(mc).getScaleFactor();
            double allowed = FishingMath.allowedAngle(FishingMath.SPOT_SCREEN_RADIUS * scale, SCREEN.fovDegrees(),
                    SCREEN.viewportHeight());
            aimed = FishingMath.isAimed(p[2], allowed);
        }
    }

    /**
     * 弱点に照準が重なっているときの左クリックは、弱点のヒットだけにして、Minecraft の通常の左クリック
     * （照準の先のブロックを掘る、動物を攻撃する）は止める。MouseEvent をキャンセルすると、攻撃のキーが押された扱いにならない。
     * 弱点から外れているときは、今までどおり。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouse(MouseEvent event) {
        if (event.getButton() != 0 || !event.isButtonstate()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null || !aimed || !shown(mc)) {
            return;
        }
        event.setCanceled(true);
        if (ClientWeakSpotHandler.canHitNow(HitKind.FISHING, ClientSettings.get().fishingMinHitIntervalTicks)) {
            onHit();
        }
    }

    private static void onHit() {
        int streak = ClientWeakSpotHandler.registerHit(HitKind.FISHING);
        WeakSpotMod.network.sendToServer(HitMessage.withoutTarget(HitKind.FISHING, streak));
        double[] next = FishingMath.nextSpot(dx, dz, RANDOM);
        dx = next[0];
        dz = next[1];
        if (WeakSpotConfig.weakSpotTrailEnabled) {
            MOTION.moveTo(dx, dz, Minecraft.getSystemTime());
        } else {
            MOTION.jumpTo(dx, dz);
        }
        // 待ち時間が縮んだ分の進み具合を、すぐに見に行く
        query(ClientWeakSpotHandler.clientTick);
        aimed = false;
    }

    @SubscribeEvent
    public static void onOverlayPost(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || !SCREEN.isValid()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (!shown(mc) || mc.gameSettings.hideGUI) {
            return;
        }
        double scale = new ScaledResolution(mc).getScaleFactor();
        long nowMs = Minecraft.getSystemTime();
        double radius = FishingMath.SPOT_SCREEN_RADIUS;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableDepth();
        GlStateManager.glLineWidth(2.0F);

        boolean trail = WeakSpotConfig.weakSpotTrailEnabled;
        if (trail) {
            for (MarkerMotion.Afterimage image : MOTION.afterimages(nowMs)) {
                double[] p = SCREEN.project(hookX + image.u, hookY + LIFT, hookZ + image.v);
                if (p != null) {
                    float a = (float) image.alpha(nowMs);
                    ScreenProjection.fill(p[0] / scale, p[1] / scale, radius, DISK, 0.35F * a);
                    ScreenProjection.outline(p[0] / scale, p[1] / scale, radius, RING, 0.5F * a);
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
            ScreenProjection.fill(gx, gy, radius, DISK, 0.45F);
            ScreenProjection.outline(gx, gy, radius, RING, 0.9F);
            ScreenProjection.fill(gx, gy, radius * 0.3, new float[] {1.0F, 0.95F, 0.7F}, 0.9F);
            double head = trail ? MOTION.headHighlight(nowMs) : 0;
            if (head > 0) {
                ScreenProjection.fill(gx, gy, radius, new float[] {1, 1, 1}, (float) (0.5 * head));
            }
        }
        // 浮きの下の、魚が寄ってくるまでの進み具合のバー
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

        GlStateManager.glLineWidth(1.0F);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }
}
