package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.SleepSpotArea;
import io.github.zeusisgood.weakspot.common.SleepTime;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.GameRules;

/** 寝ている画面（InBedChatScreen）にはマウスのカーソルが出ているので、その上にマーカーを出す。当てると、サーバーが時刻を進める（SleepHits）。 */
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
    boolean eligible(Minecraft mc, Screen gui) {
        if (!(gui instanceof InBedChatScreen) || mc.player == null || !mc.player.isSleeping()
                || !KindSwitches.isEnabled(HitKind.SLEEP) || mc.player.isSpectator()) {
            return false;
        }
        SyncedSettings settings = ClientSettings.get();
        ClientLevel world = mc.level;
        return settings.enabled(HitKind.SLEEP) && world != null
                && world.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT) && SleepTime.isNight(world.getDayTime());
    }

    /** 画面の中央の範囲（画面全体だと、次のマーカーが遠すぎるため）。 */
    @Override
    boolean place(Screen gui, double prevX, double prevY) {
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
