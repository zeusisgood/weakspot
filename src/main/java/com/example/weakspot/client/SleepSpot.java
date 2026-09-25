package com.example.weakspot.client;

import com.example.weakspot.common.MarkerShape;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.MarkerMotion;
import com.example.weakspot.common.SleepSpotArea;
import com.example.weakspot.common.SleepTime;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.network.HitMessage;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSleepMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Mouse;

/**
 * 寝ている間の弱点（1.6.0）。夜にベッドで寝ている間は、寝ている画面（GuiSleepMP）が開いていて、マウスのカーソルが
 * 出ている。その画面の上に月の淡い黄のマーカーを出し、クリックで当てる（当てたクリックは、ボタンやチャット欄に
 * 届かないようにキャンセルする）。当てると、サーバーがワールドの時刻を進める（SleepHits）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
final class SleepSpot {

    /** 月の淡い黄 #FFF1A8。 */
    private static final float[] DISK = {0xFF / 255F, 0xF1 / 255F, 0xA8 / 255F};
    private static final float[] RING = {1.0F, 0.98F, 0.85F};
    private static final float[] CENTER = {1.0F, 1.0F, 0.95F};
    private static final double RADIUS = 12;

    private static final Random RANDOM = new Random();
    private static final MarkerMotion MOTION = new MarkerMotion(0, 0);

    private static GuiScreen shownOn;
    private static double x;
    private static double y;

    private SleepSpot() {
    }

    private static boolean eligible(Minecraft mc, GuiScreen gui) {
        if (!(gui instanceof GuiSleepMP) || mc.player == null || !mc.player.isPlayerSleeping()
                || !KindSwitches.isEnabled(HitKind.SLEEP) || mc.player.isSpectator()) {
            return false;
        }
        SyncedSettings settings = ClientSettings.get();
        WorldClient world = mc.world;
        return settings.sleepWeakSpotEnabled && world != null
                && world.getGameRules().getBoolean("doDaylightCycle") && SleepTime.isNight(world.getWorldTime());
    }

    /** 画面が変わったら（開き直したら）、新しい位置に出す。 */
    private static void ensure(GuiScreen gui) {
        if (shownOn == gui) {
            return;
        }
        shownOn = gui;
        place(gui, gui.width / 2.0, gui.height / 2.0);
        MOTION.jumpTo(x, y);
    }

    /**
     * 画面の中央の範囲（1.6.2。SleepSpotArea。画面全体だと、次のマーカーが遠すぎるため）のランダムな位置。
     * 前の位置から SleepSpotArea.minMove 以上離す（取れなければ、一番離れた位置）。
     */
    private static void place(GuiScreen gui, double prevX, double prevY) {
        double[] area = SleepSpotArea.area(gui.width, gui.height, RADIUS);
        double minMove = SleepSpotArea.minMove(gui.width, gui.height, RADIUS);
        double bestX = x;
        double bestY = y;
        double bestMove = -1;
        for (int i = 0; i < 40; i++) {
            double nx = area[0] + RANDOM.nextDouble() * (area[1] - area[0]);
            double ny = area[2] + RANDOM.nextDouble() * (area[3] - area[2]);
            double move = Math.hypot(nx - prevX, ny - prevY);
            if (move >= minMove) {
                x = nx;
                y = ny;
                return;
            }
            if (move > bestMove) {
                bestMove = move;
                bestX = nx;
                bestY = ny;
            }
        }
        x = bestX;
        y = bestY;
    }

    @SubscribeEvent
    public static void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen gui = event.getGui();
        if (!eligible(mc, gui)) {
            shownOn = null;
            return;
        }
        ensure(gui);
        long nowMs = Minecraft.getSystemTime();
        boolean trail = WeakSpotConfig.weakSpotTrailEnabled;
        float[][] look = MarkerLook.palette(HitKind.SLEEP, DISK, RING, CENTER);
        MarkerShape shape = MarkerLook.shape(HitKind.SLEEP);
        HudSpot.beginOverlay();
        if (trail) {
            for (MarkerMotion.Afterimage image : MOTION.afterimages(nowMs)) {
                float a = (float) image.alpha(nowMs);
                ScreenProjection.fillShape(image.u, image.v, RADIUS, shape, look[0], 0.35F * a);
                ScreenProjection.outlineShape(image.u, image.v, RADIUS, shape, look[1], 0.5F * a);
            }
        }
        double[] p = trail ? MOTION.position(nowMs) : new double[] {x, y};
        ScreenProjection.fillShape(p[0], p[1], RADIUS, shape, look[0], 0.55F);
        ScreenProjection.outlineShape(p[0], p[1], RADIUS, shape, look[1], 0.95F);
        if (shape.hasCenterDot()) {
            ScreenProjection.fill(p[0], p[1], RADIUS * 0.3, look[2], 0.95F);
        }
        HudSpot.endOverlay();
    }

    @SubscribeEvent
    public static void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen gui = event.getGui();
        if (Mouse.getEventButton() != 0 || !Mouse.getEventButtonState() || !eligible(mc, gui) || shownOn != gui) {
            return;
        }
        double mouseX = Mouse.getEventX() * gui.width / (double) mc.displayWidth;
        double mouseY = gui.height - Mouse.getEventY() * gui.height / (double) mc.displayHeight - 1;
        if (Math.hypot(mouseX - x, mouseY - y) > RADIUS) {
            return;
        }
        // 当てたクリックは、ボタンやチャット欄に届かないようにする
        event.setCanceled(true);
        if (!ClientWeakSpotHandler.canHitNow(HitKind.SLEEP, ClientSettings.get().sleepMinHitIntervalTicks)) {
            return;
        }
        int streak = ClientWeakSpotHandler.registerHit(HitKind.SLEEP);
        WeakSpotMod.network.sendToServer(HitMessage.withoutTarget(HitKind.SLEEP, streak));
        place(gui, x, y);
        if (WeakSpotConfig.weakSpotTrailEnabled) {
            MOTION.moveTo(x, y, Minecraft.getSystemTime());
        } else {
            MOTION.jumpTo(x, y);
        }
    }
}
