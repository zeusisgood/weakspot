package com.example.weakspot.client;

import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.SleepSpotArea;
import com.example.weakspot.common.SleepTime;
import com.example.weakspot.config.SyncedSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSleepMP;
import net.minecraft.client.multiplayer.WorldClient;

/**
 * 寝ている間の弱点（1.6.0）。夜にベッドで寝ている間は、寝ている画面（GuiSleepMP）が開いていて、マウスのカーソルが
 * 出ている。その画面の上に月の淡い黄のマーカーを出し、クリックで当てる（描く・当てる流れは ScreenSpots。1.8.8）。
 * 当てると、サーバーがワールドの時刻を進める（SleepHits）。
 */
final class SleepSpot extends ScreenSpotKind {

    /** 月の淡い黄 #FFF1A8。 */
    private static final float[] DISK = {0xFF / 255F, 0xF1 / 255F, 0xA8 / 255F};
    private static final float[] RING = {1.0F, 0.98F, 0.85F};
    private static final float[] CENTER = {1.0F, 1.0F, 0.95F};

    SleepSpot() {
        super(HitKind.SLEEP);
    }

    @Override
    boolean eligible(Minecraft mc, GuiScreen gui) {
        if (!(gui instanceof GuiSleepMP) || mc.player == null || !mc.player.isPlayerSleeping()
                || !KindSwitches.isEnabled(HitKind.SLEEP) || mc.player.isSpectator()) {
            return false;
        }
        SyncedSettings settings = ClientSettings.get();
        WorldClient world = mc.world;
        return settings.enabled(HitKind.SLEEP) && world != null
                && world.getGameRules().getBoolean("doDaylightCycle") && SleepTime.isNight(world.getWorldTime());
    }


    /**
     * 画面の中央の範囲（1.6.2。SleepSpotArea。画面全体だと、次のマーカーが遠すぎるため）のランダムな位置。
     * 前の位置から SleepSpotArea.minMove 以上離す（取れなければ、一番離れた位置）。
     */
    @Override
    boolean place(GuiScreen gui, double prevX, double prevY) {
        double[] area = SleepSpotArea.area(gui.width, gui.height, RADIUS);
        double minMove = SleepSpotArea.minMove(gui.width, gui.height, RADIUS);
        double bestX = x;
        double bestY = y;
        double bestMove = -1;
        for (int i = 0; i < 40; i++) {
            double nx = area[0] + random.nextDouble() * (area[1] - area[0]);
            double ny = area[2] + random.nextDouble() * (area[3] - area[2]);
            double move = Math.hypot(nx - prevX, ny - prevY);
            if (move >= minMove) {
                x = nx;
                y = ny;
                return true;
            }
            if (move > bestMove) {
                bestMove = move;
                bestX = nx;
                bestY = ny;
            }
        }
        x = bestX;
        y = bestY;
        return true;
    }

    @Override
    float[][] look() {
        return MarkerLook.palette(HitKind.SLEEP, DISK, RING, CENTER);
    }

    @Override
    float outlineAlpha() {
        return 0.95F;
    }
}
