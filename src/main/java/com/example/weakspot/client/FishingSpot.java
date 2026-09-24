package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.FishingMath;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.MarkerMotion;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.network.FishingQueryMessage;
import com.example.weakspot.network.HitMessage;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
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
import org.lwjgl.opengl.GL11;

/**
 * 釣りの弱点（自分だけ。他のプレイヤーには見せない）。浮きが水に入って魚を待っている間、浮きのまわり（半径1ブロック）の
 * 水面の上の点に弱点を出し、釣り竿を持って、照準が重なっているときに左クリックで叩く。
 *
 * 弱点はワールドの1点（浮きからの水平の差 dx, dz。浮きが揺れても追従する）で、描くときに画面上の位置へ変換して、
 * 画面上で一定の大きさ（半径 12 GUI ピクセル）の円として描く（浮きが遠くても近くても同じ大きさ）。
 * 当たり判定は、視線と「目から弱点への向き」の角度の差が、画面上の円の半径・視野角・画面の高さから求めた角度より小さいこと。
 * 変換に使う行列は、RenderWorldLastEvent の時点の OpenGL の行列（視野角は射影行列から求める）。
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
    private static final int SEGMENTS = 32;

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
    private static final FloatBuffer MODELVIEW = GLAllocation.createDirectFloatBuffer(16);
    private static final FloatBuffer PROJECTION = GLAllocation.createDirectFloatBuffer(16);
    private static final IntBuffer VIEWPORT = GLAllocation.createDirectIntBuffer(16);
    private static final float[] MV = new float[16];
    private static final float[] PROJ = new float[16];
    private static final int[] VP = new int[4];
    private static double camX;
    private static double camY;
    private static double camZ;
    private static double hookX;
    private static double hookY;
    private static double hookZ;
    /** このフレームの行列などが、今の弱点のもので、使えるか。 */
    private static boolean frameValid;
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
        frameValid = false;
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

    /**
     * ワールドの点を画面上の位置に変換する。返すのは {画面の左端からのピクセル, 上端からのピクセル, 視線との角度（ラジアン）}
     * （ピクセルは実際のウィンドウのピクセル）。カメラの後ろ、画面の外なら null。
     */
    private static double[] project(double wx, double wy, double wz) {
        double x = wx - camX;
        double y = wy - camY;
        double z = wz - camZ;
        double ex = MV[0] * x + MV[4] * y + MV[8] * z + MV[12];
        double ey = MV[1] * x + MV[5] * y + MV[9] * z + MV[13];
        double ez = MV[2] * x + MV[6] * y + MV[10] * z + MV[14];
        if (ez >= -0.05) {
            return null;
        }
        double cx = PROJ[0] * ex + PROJ[4] * ey + PROJ[8] * ez + PROJ[12];
        double cy = PROJ[1] * ex + PROJ[5] * ey + PROJ[9] * ez + PROJ[13];
        double cw = PROJ[3] * ex + PROJ[7] * ey + PROJ[11] * ez + PROJ[15];
        if (cw <= 0) {
            return null;
        }
        double px = (cx / cw + 1) / 2 * VP[2];
        double py = VP[3] - (cy / cw + 1) / 2 * VP[3];
        if (px < 0 || px > VP[2] || py < 0 || py > VP[3]) {
            return null;
        }
        double angle = Math.acos(Math.max(-1, Math.min(1, -ez / Math.sqrt(ex * ex + ey * ey + ez * ez))));
        return new double[] {px, py, angle};
    }

    /** 縦の視野角（度）。射影行列の [1][1] が 1 / tan(fov / 2)。 */
    private static double fovDegrees() {
        return Math.toDegrees(2 * Math.atan(1.0 / PROJ[5]));
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        frameValid = false;
        aimed = false;
        if (!shown(mc) || mc.getRenderViewEntity() == null || mc.gameSettings.hideGUI || spotHook == null) {
            return;
        }
        float pt = event.getPartialTicks();
        net.minecraft.entity.Entity camera = mc.getRenderViewEntity();
        camX = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * pt;
        camY = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * pt;
        camZ = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * pt;
        hookX = spotHook.lastTickPosX + (spotHook.posX - spotHook.lastTickPosX) * pt;
        hookY = spotHook.lastTickPosY + (spotHook.posY - spotHook.lastTickPosY) * pt;
        hookZ = spotHook.lastTickPosZ + (spotHook.posZ - spotHook.lastTickPosZ) * pt;

        MODELVIEW.clear();
        PROJECTION.clear();
        VIEWPORT.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODELVIEW);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION);
        GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT);
        MODELVIEW.get(MV).rewind();
        PROJECTION.get(PROJ).rewind();
        for (int i = 0; i < 4; i++) {
            VP[i] = VIEWPORT.get(i);
        }
        frameValid = VP[3] > 0 && PROJ[5] != 0;
        if (!frameValid) {
            return;
        }
        double[] p = project(hookX + dx, hookY + LIFT, hookZ + dz);
        if (p != null && mc.currentScreen == null) {
            double scale = new ScaledResolution(mc).getScaleFactor();
            double allowed = FishingMath.allowedAngle(FishingMath.SPOT_SCREEN_RADIUS * scale, fovDegrees(), VP[3]);
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
        WeakSpotMod.network.sendToServer(HitMessage.fishing(streak));
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
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || !frameValid) {
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
                double[] p = project(hookX + image.u, hookY + LIFT, hookZ + image.v);
                if (p != null) {
                    float a = (float) image.alpha(nowMs);
                    fill(p[0] / scale, p[1] / scale, radius, DISK, 0.35F * a);
                    outline(p[0] / scale, p[1] / scale, radius, RING, 0.5F * a);
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
        double[] p = project(hookX + u, hookY + LIFT, hookZ + v);
        if (p != null) {
            double gx = p[0] / scale;
            double gy = p[1] / scale;
            fill(gx, gy, radius, DISK, 0.45F);
            outline(gx, gy, radius, RING, 0.9F);
            fill(gx, gy, radius * 0.3, new float[] {1.0F, 0.95F, 0.7F}, 0.9F);
            double head = trail ? MOTION.headHighlight(nowMs) : 0;
            if (head > 0) {
                fill(gx, gy, radius, new float[] {1, 1, 1}, (float) (0.5 * head));
            }
        }
        // 浮きの下の、魚が寄ってくるまでの進み具合のバー
        double[] hook = project(hookX, hookY, hookZ);
        if (hook != null) {
            double bx = hook[0] / scale;
            double by = hook[1] / scale + BAR_OFFSET;
            rect(bx - BAR_WIDTH / 2.0, by - BAR_HEIGHT / 2.0, bx + BAR_WIDTH / 2.0, by + BAR_HEIGHT / 2.0, BACK);
            double f = Math.max(0, Math.min(1, progress));
            if (f > 0) {
                rect(bx - BAR_WIDTH / 2.0, by - BAR_HEIGHT / 2.0, bx - BAR_WIDTH / 2.0 + BAR_WIDTH * f,
                        by + BAR_HEIGHT / 2.0, FILL);
            }
        }

        GlStateManager.glLineWidth(1.0F);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }

    private static void fill(double x, double y, double radius, float[] rgb, float a) {
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

    private static void outline(double x, double y, double radius, float[] rgb, float a) {
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

    private static void rect(double x0, double y0, double x1, double y1, float[] rgba) {
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
