package com.example.weakspot.client;

import com.example.weakspot.common.MiningStats;
import java.util.function.Function;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

/** 「今回」と「累計」の統計を並べて表示する画面。 */
final class StatsScreen extends GuiScreen {

    private static final int BUTTON_RESET = 0;
    private static final int BUTTON_DONE = 1;
    private static final int ROW_HEIGHT = 14;

    private GuiButton resetButton;
    private boolean confirmingReset;

    @Override
    public void initGui() {
        buttonList.clear();
        int y = Math.min(height - 28, height / 2 + 70);
        resetButton = new GuiButton(BUTTON_RESET, width / 2 - 154, y, 150, 20, "");
        buttonList.add(resetButton);
        buttonList.add(new GuiButton(BUTTON_DONE, width / 2 + 4, y, 150, 20, I18n.format("gui.done")));
        setConfirmingReset(false);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == BUTTON_RESET) {
            if (confirmingReset) {
                StatsManager.resetTotal();
                setConfirmingReset(false);
            } else {
                setConfirmingReset(true);
            }
        } else if (button.id == BUTTON_DONE) {
            mc.displayGuiScreen(null);
        }
    }

    /** 累計のリセットは2回押しで確定する（誤操作防止）。 */
    private void setConfirmingReset(boolean confirming) {
        confirmingReset = confirming;
        resetButton.displayString = I18n.format(confirming ? "weakspot.stats.reset.confirm" : "weakspot.stats.reset");
    }

    @Override
    public void onGuiClosed() {
        StatsManager.save();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        int top = Math.max(20, height / 2 - 90);
        drawCenteredString(fontRenderer, I18n.format("weakspot.stats.title"), width / 2, top, 0xFFFFFF);

        int labelX = width / 2 - 150;
        int sessionRight = width / 2 + 70;
        int totalRight = width / 2 + 150;
        int y = top + 24;
        drawRight(I18n.format("weakspot.stats.column.session"), sessionRight, y, 0xAAAAAA);
        drawRight(I18n.format("weakspot.stats.column.total"), totalRight, y, 0xAAAAAA);
        y += ROW_HEIGHT + 2;

        MiningStats session = StatsManager.session();
        MiningStats total = StatsManager.total();
        y = row("weakspot.stats.hits", s -> Long.toString(s.hits), session, total, labelX, sessionRight, totalRight, y);
        y = row("weakspot.stats.blocks", s -> Long.toString(s.blocksBroken), session, total, labelX, sessionRight, totalRight, y);
        y = row("weakspot.stats.blocksWithHit", s -> Long.toString(s.blocksBrokenWithHit), session, total, labelX, sessionRight, totalRight, y);
        y = row("weakspot.stats.averageHits", StatsScreen::formatAverage, session, total, labelX, sessionRight, totalRight, y);
        y = row("weakspot.stats.maxHits", s -> Long.toString(s.maxHitsOnBlock), session, total, labelX, sessionRight, totalRight, y);
        row("weakspot.stats.timeSaved", s -> formatDuration(s.savedSeconds()), session, total, labelX, sessionRight, totalRight, y);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private int row(String labelKey, Function<MiningStats, String> value, MiningStats session, MiningStats total,
                    int labelX, int sessionRight, int totalRight, int y) {
        drawString(fontRenderer, I18n.format(labelKey), labelX, y, 0xFFFFFF);
        drawRight(value.apply(session), sessionRight, y, 0xFFFFFF);
        drawRight(value.apply(total), totalRight, y, 0xFFFF55);
        return y + ROW_HEIGHT;
    }

    private void drawRight(String text, int right, int y, int color) {
        drawString(fontRenderer, text, right - fontRenderer.getStringWidth(text), y, color);
    }

    private static String formatAverage(MiningStats stats) {
        double average = stats.averageHitsPerBlock();
        return Double.isNaN(average) ? "-" : String.format("%.2f", average);
    }

    private static String formatDuration(double seconds) {
        if (seconds < 60) {
            return I18n.format("weakspot.stats.time.seconds", String.format("%.1f", seconds));
        }
        long whole = (long) seconds;
        if (whole < 3600) {
            return I18n.format("weakspot.stats.time.minutes", whole / 60, String.format("%02d", whole % 60));
        }
        return I18n.format("weakspot.stats.time.hours", whole / 3600, String.format("%02d", whole / 60 % 60));
    }
}
