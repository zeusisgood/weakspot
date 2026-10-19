package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.SleepSpotArea;
import io.github.zeusisgood.weakspot.common.SleepTime;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSleepMP;
import net.minecraft.client.multiplayer.WorldClient;

/** 寝ている画面（GuiSleepMP）にはマウスのカーソルが出ているので、その上にマーカーを出す。当てると、サーバーが時刻を進める（SleepHits）。 */
final class SleepSpot extends ScreenSpotKind {

    private static final float[] DISK = {0xFF / 255F, 0xF1 / 255F, 0xA8 / 255F};
    private static final float[] RING = {1.0F, 0.98F, 0.85F};
    private static final float[] CENTER = {1.0F, 1.0F, 0.95F};

    SleepSpot() {
        super(HitKind.SLEEP);
    }

    @Override
    boolean hitsOnHover() {
        return true;
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

    /** 画面の中央の範囲（画面全体だと、次のマーカーが遠すぎるため）。 */
    @Override
    boolean place(GuiScreen gui, double prevX, double prevY) {
        double[] area = SleepSpotArea.area(gui.width, gui.height, radius());
        double minMove = SleepSpotArea.minMove(gui.width, gui.height, radius());
        // 除く所がないので、必ず置ける
        return placeRandom(area[0], area[1], area[2], area[3], null, 40, minMove, prevX, prevY);
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
