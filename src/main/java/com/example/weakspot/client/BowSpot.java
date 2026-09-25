package com.example.weakspot.client;

import com.example.weakspot.common.MarkerShape;
import com.example.weakspot.BowDraw;
import com.example.weakspot.VehicleTargets;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.BowMath;
import com.example.weakspot.common.FishingMath;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.MarkerMotion;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.network.HitMessage;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 弓の弱点（自分だけ。他のプレイヤーには見せない）と、引きゲージ。弓を引いている間、照準の近く（視線から 3〜8 度）に
 * 弱点を出し、照準を合わせるだけでヒットにする（クリックは要らない）。ヒットすると、弓の引きが進む（BowDraw）。
 * 引き切ったあとは、過剰チャージ（矢のダメージ +10%、上限 5 回。1.3.4）が上限に届くまで弱点を出す。
 *
 * 弱点は向き（yaw / pitch）で持ち、目からその向きの先の点を、釣りと同じく画面上の位置に変換して、画面上で一定の大きさ
 * （半径 12 GUI ピクセル）の円として描く（ScreenProjection）。当たり判定も釣りと同じ（FishingMath.allowedAngle）。
 * 引きゲージは、弓の弱点や一時オフに関係なく、bowDrawBarEnabled なら照準の下に出す。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
final class BowSpot {

    private static final Random RANDOM = new Random();
    /** 弱点の点を置く、目からの距離（ブロック）。向きだけが意味を持つ。 */
    private static final double SPOT_DISTANCE = 16.0;

    private static final int BAR_WIDTH = 40;
    private static final int BAR_HEIGHT = 3;
    /** 照準の中心から、ゲージの中心までの下向きの距離（GUI ピクセル）。 */
    private static final int BAR_OFFSET = 12;

    private static final float[] DISK = {1.0F, 0.35F, 0.15F};
    private static final float[] RING = {1.0F, 0.9F, 0.4F};
    private static final float[] CENTER = {1.0F, 0.95F, 0.7F};
    private static final float[] WHITE = {1.0F, 1.0F, 1.0F};
    /** 引いている途中 #FF8C42、引き切った #FFD23F、背景 #1E1E1E 半透明。 */
    private static final float[] BAR_DRAWING = {0xFF / 255F, 0x8C / 255F, 0x42 / 255F, 1.0F};
    private static final float[] BAR_FULL = {0xFF / 255F, 0xD2 / 255F, 0x3F / 255F, 1.0F};
    private static final float[] BAR_BACK = {0x1E / 255F, 0x1E / 255F, 0x1E / 255F, 0.5F};
    /** 過剰チャージの目盛り #FF4D4D。ゲージのすぐ下に、上限の数だけ並べる。 */
    private static final float[] OVERCHARGE = {0xFF / 255F, 0x4D / 255F, 0x4D / 255F, 1.0F};
    private static final int OVERCHARGE_HEIGHT = 2;
    private static final int OVERCHARGE_GAP = 1;

    private static final ScreenProjection SCREEN = new ScreenProjection();

    // 弱点の向き（当たり判定の位置）と、表示の位置 motion（u = yaw, v = pitch）
    private static boolean hasSpot;
    private static double yaw;
    private static double pitch;
    private static final MarkerMotion MOTION = new MarkerMotion(0, 0);
    /** このフレームの、目の位置。 */
    private static double eyeX;
    private static double eyeY;
    private static double eyeZ;

    private BowSpot() {
    }

    /** 弱点の一時オフ、ワールドを出たとき。 */
    static void clear() {
        hasSpot = false;
        SCREEN.invalidate();
    }

    /** 弱点を出せるか（弓を引いていて、設定がオンで、クリエイティブ・スペクテイターでない）。 */
    private static boolean eligible(Minecraft mc) {
        if (mc.player == null || mc.world == null || !KindSwitches.isEnabled(HitKind.BOW)
                || mc.player.capabilities.isCreativeMode || mc.player.isSpectator()) {
            return false;
        }
        SyncedSettings settings = ClientSettings.get();
        return settings.bowWeakSpotEnabled && settings.bowHitTicks > 0 && BowDraw.isDrawing(mc.player);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.isGamePaused()) {
            return;
        }
        if (!eligible(mc)) {
            clear();
            return;
        }
        EntityPlayerSP player = mc.player;
        if (!spotWanted(player)) {
            hasSpot = false;
        } else if (!hasSpot) {
            // 引き始めた。今の視線の近くに出す
            double[] next = next(player, player.rotationYaw, player.rotationPitch);
            yaw = next[0];
            pitch = next[1];
            MOTION.jumpTo(yaw, pitch);
            hasSpot = true;
        }
    }

    /**
     * 次の弱点の向き。馬・豚に乗っているときは、照準の真上か真下だけ（1.6.0。狙うたびに進む向きがぶれないように）。
     * 真上・真下の弱点の yaw は、出した時点の視線の yaw（弓を引いている短い間なので、向きを追いかけない）。
     */
    private static double[] next(EntityPlayerSP player, double prevYaw, double prevPitch) {
        if (VehicleTargets.isSteeredByLook(player)) {
            return new double[] {player.rotationYaw,
                    BowMath.nextVerticalPitch(player.rotationPitch, prevPitch, SCREEN.fovDegrees(), RANDOM)};
        }
        return BowMath.nextSpot(player.rotationYaw, player.rotationPitch, prevYaw, prevPitch, SCREEN.fovDegrees(),
                RANDOM);
    }

    /** 弱点を出すか。引き切る前と、引き切ったあとの過剰チャージが上限に届くまで。 */
    private static boolean spotWanted(EntityPlayerSP player) {
        return !BowMath.isFull(BowDraw.usedTicks(player)) || BowMath.canOvercharge(BowDraw.overcharge(player));
    }

    /** 向き (y, p) の弱点の点のワールド座標。 */
    private static double[] worldPoint(double y, double p) {
        double[] d = BowMath.vector(y, p);
        return new double[] {eyeX + d[0] * SPOT_DISTANCE, eyeY + d[1] * SPOT_DISTANCE, eyeZ + d[2] * SPOT_DISTANCE};
    }

    private static double[] project(double y, double p) {
        double[] w = worldPoint(y, p);
        return SCREEN.project(w[0], w[1], w[2]);
    }

    /** 当たり判定は毎フレーム行う（素早く照準を動かしたときに、tick 単位だと取りこぼすため）。 */
    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        SCREEN.invalidate();
        if (!hasSpot || !eligible(mc) || mc.gameSettings.hideGUI) {
            return;
        }
        float pt = event.getPartialTicks();
        EntityPlayerSP player = mc.player;
        eyeX = player.lastTickPosX + (player.posX - player.lastTickPosX) * pt;
        eyeY = player.lastTickPosY + (player.posY - player.lastTickPosY) * pt + player.getEyeHeight();
        eyeZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * pt;
        if (!SCREEN.capture(mc, pt) || mc.currentScreen != null) {
            return;
        }
        double[] p = project(yaw, pitch);
        if (p == null) {
            return;
        }
        double scale = new ScaledResolution(mc).getScaleFactor();
        double allowed = FishingMath.allowedAngle(FishingMath.SPOT_SCREEN_RADIUS * scale, SCREEN.fovDegrees(),
                SCREEN.viewportHeight());
        if (FishingMath.isAimed(p[2], allowed)
                && ClientWeakSpotHandler.canHitNow(HitKind.BOW, ClientSettings.get().bowMinHitIntervalTicks)) {
            onHit(player);
        }
    }

    private static void onHit(EntityPlayerSP player) {
        int streak = ClientWeakSpotHandler.registerHit(HitKind.BOW);
        // 乗り物に乗っていれば、加速も続ける（騎射。1.6.0）
        VehicleSpot.onRiderHit(streak);
        WeakSpotMod.network.sendToServer(HitMessage.withoutTarget(HitKind.BOW, streak));
        // サーバーの返事を待たずに、自分の側でも引きを進める（弓の見た目とゲージのため）。引き切ったあとは過剰チャージ
        if (BowMath.isFull(BowDraw.usedTicks(player))) {
            BowDraw.addOvercharge(player);
        } else {
            BowDraw.add(player, ClientSettings.get().bowHitTicks);
        }
        if (!spotWanted(player)) {
            hasSpot = false;
            return;
        }
        double[] next = next(player, yaw, pitch);
        yaw = next[0];
        pitch = next[1];
        if (WeakSpotConfig.weakSpotTrailEnabled) {
            MOTION.moveTo(yaw, pitch, Minecraft.getSystemTime());
        } else {
            MOTION.jumpTo(yaw, pitch);
        }
    }

    @SubscribeEvent
    public static void onOverlayPost(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.gameSettings.hideGUI) {
            return;
        }
        boolean spot = hasSpot && SCREEN.isValid() && eligible(mc);
        boolean bar = WeakSpotConfig.bowDrawBarEnabled && BowDraw.isDrawing(mc.player);
        if (!spot && !bar) {
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableDepth();
        // HUD の図形は時計回りに頂点を並べるので、カリングを切らないと塗りが消える（1.7.1）
        GlStateManager.disableCull();
        GlStateManager.glLineWidth(2.0F);

        if (spot) {
            drawSpot(mc);
        }
        if (bar) {
            drawBar(mc, event.getPartialTicks());
        }

        GlStateManager.glLineWidth(1.0F);
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }

    private static void drawSpot(Minecraft mc) {
        double scale = new ScaledResolution(mc).getScaleFactor();
        long nowMs = Minecraft.getSystemTime();
        double radius = FishingMath.SPOT_SCREEN_RADIUS;
        boolean trail = WeakSpotConfig.weakSpotTrailEnabled;
        float[][] look = MarkerLook.palette(HitKind.BOW, DISK, RING, CENTER);
        MarkerShape shape = MarkerLook.shape(HitKind.BOW);
        if (trail) {
            for (MarkerMotion.Afterimage image : MOTION.afterimages(nowMs)) {
                double[] p = project(image.u, image.v);
                if (p != null) {
                    float a = (float) image.alpha(nowMs);
                    ScreenProjection.fillShape(p[0] / scale, p[1] / scale, radius, shape, look[0], 0.35F * a);
                    ScreenProjection.outlineShape(p[0] / scale, p[1] / scale, radius, shape, look[1], 0.5F * a);
                }
            }
        }
        double u = yaw;
        double v = pitch;
        if (trail) {
            double[] m = MOTION.position(nowMs);
            u = m[0];
            v = m[1];
        }
        double[] p = project(u, v);
        if (p == null) {
            return;
        }
        double gx = p[0] / scale;
        double gy = p[1] / scale;
        ScreenProjection.fillShape(gx, gy, radius, shape, look[0], 0.45F);
        ScreenProjection.outlineShape(gx, gy, radius, shape, look[1], 0.9F);
        if (shape.hasCenterDot()) {
            ScreenProjection.fill(gx, gy, radius * 0.3, look[2], 0.9F);
        }
        double head = trail ? MOTION.headHighlight(nowMs) : 0;
        if (head > 0) {
            ScreenProjection.fillShape(gx, gy, radius, shape, WHITE, (float) (0.5 * head));
        }
    }

    /** 照準の下の、引き具合のゲージ。左から伸び、引き切ったら色が変わる。引き切ったあとは、過剰チャージの目盛りも出す。 */
    private static void drawBar(Minecraft mc, float partialTicks) {
        ScaledResolution res = new ScaledResolution(mc);
        int used = mc.player.getItemInUseMaxCount();
        boolean full = BowMath.isFull(used);
        double value = full ? 1.0 : BowMath.barValue(used + partialTicks);
        double x0 = res.getScaledWidth() / 2.0 - BAR_WIDTH / 2.0;
        double y0 = res.getScaledHeight() / 2.0 + BAR_OFFSET - BAR_HEIGHT / 2.0;
        ScreenProjection.rect(x0, y0, x0 + BAR_WIDTH, y0 + BAR_HEIGHT, BAR_BACK);
        if (value > 0) {
            ScreenProjection.rect(x0, y0, x0 + BAR_WIDTH * value, y0 + BAR_HEIGHT, full ? BAR_FULL : BAR_DRAWING);
        }
        int overcharge = BowDraw.overcharge(mc.player);
        if (full && (overcharge > 0 || hasSpot)) {
            // 過剰チャージの目盛り。達した分を赤く
            int count = BowMath.MAX_OVERCHARGE_HITS;
            double width = (BAR_WIDTH - OVERCHARGE_GAP * (count - 1)) / (double) count;
            double top = y0 + BAR_HEIGHT + OVERCHARGE_GAP;
            for (int i = 0; i < count; i++) {
                double left = x0 + i * (width + OVERCHARGE_GAP);
                ScreenProjection.rect(left, top, left + width, top + OVERCHARGE_HEIGHT,
                        i < overcharge ? OVERCHARGE : BAR_BACK);
            }
        }
    }
}
