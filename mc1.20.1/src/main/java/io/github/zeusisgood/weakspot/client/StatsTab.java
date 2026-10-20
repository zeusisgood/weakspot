package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.MiningStats;
import io.github.zeusisgood.weakspot.network.StatsRequestMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

/** サーバーから届いた「今回」と「累計」を並べて表示するだけ。 */
final class StatsTab extends StatsScreenTab {

    private static final int ROW_HEIGHT = 10;

    /** 届くまでは null。 */
    private static MiningStats session;
    private static MiningStats total;

    private Button resetButton;
    private boolean confirmingReset;
    private int scroll;

    StatsTab(StatsScreen screen) {
        super(screen);
    }

    /** クライアントのスレッドで呼ぶ。 */
    static void receive(MiningStats newSession, MiningStats newTotal) {
        session = newSession;
        total = newTotal;
    }

    /** 前のワールドの数字を出さないように、届くまでは空欄にする。 */
    static void forget() {
        session = null;
        total = null;
    }

    @Override
    void init() {
        resetButton = screen.add(Button.builder(Component.empty(), b -> pressReset())
                .bounds(screen.width / 2 - 154, screen.bottom(), 100, 20).build());
        setConfirmingReset(false);
    }

    @Override
    void show(boolean shown) {
        resetButton.visible = shown;
        setConfirmingReset(false);
    }

    private void pressReset() {
        if (confirmingReset) {
            WeakSpotMod.network.sendToServer(new StatsRequestMessage(true));
            setConfirmingReset(false);
        } else {
            setConfirmingReset(true);
        }
    }

    /** 誤操作防止のため、2 回押しで確定する。 */
    private void setConfirmingReset(boolean confirming) {
        confirmingReset = confirming;
        resetButton.setMessage(Component.literal(
                I18n.get(confirming ? "weakspot.stats.reset.confirm" : "weakspot.stats.reset")));
    }

    @Override
    void scroll(int step) {
        scroll = Math.max(0, scroll + step);
    }

    @Override
    void draw(GuiGraphics g) {
        int width = screen.width;
        int labelX = width / 2 - 150;
        int sessionRight = width / 2 + 70;
        int totalRight = width / 2 + 150;
        int y = screen.top() + 42;
        screen.drawRight(g, I18n.get("weakspot.stats.column.session"), sessionRight, y, 0xAAAAAA);
        screen.drawRight(g, I18n.get("weakspot.stats.column.total"), totalRight, y, 0xAAAAAA);
        y += ROW_HEIGHT;

        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] {"weakspot.stats.hits", (Function<MiningStats, String>) s -> Long.toString(s.hits)});
        rows.add(new Object[] {"weakspot.stats.blocks", (Function<MiningStats, String>) s -> Long.toString(s.blocksBroken)});
        rows.add(new Object[] {"weakspot.stats.blocksWithHit",
                (Function<MiningStats, String>) s -> Long.toString(s.blocksBrokenWithHit)});
        rows.add(new Object[] {"weakspot.stats.averageHits", (Function<MiningStats, String>) StatsTab::formatAverage});
        rows.add(new Object[] {"weakspot.stats.maxHits", (Function<MiningStats, String>) s -> Long.toString(s.maxHitsOnBlock)});
        rows.add(new Object[] {"weakspot.stats.timeSaved",
                (Function<MiningStats, String>) s -> formatDuration(s.savedSeconds())});
        for (HitKind kind : HitKind.values()) {
            if (kind != HitKind.MINING) {
                rows.add(new Object[] {statsKey(kind), (Function<MiningStats, String>) s -> Long.toString(s.count(kind))});
            }
        }
        rows.add(new Object[] {"weakspot.stats.maxStreak", (Function<MiningStats, String>) s -> Long.toString(s.maxStreak)});
        rows.add(new Object[] {"weakspot.stats.totalHits", (Function<MiningStats, String>) s -> Long.toString(s.totalHits())});

        int fit = Math.max(1, (screen.bottom() - 4 - y) / ROW_HEIGHT);
        scroll = Math.max(0, Math.min(scroll, rows.size() - fit));
        for (int i = scroll; i < Math.min(rows.size(), scroll + fit); i++) {
            @SuppressWarnings("unchecked")
            Function<MiningStats, String> value = (Function<MiningStats, String>) rows.get(i)[1];
            y = row(g, (String) rows.get(i)[0], value, labelX, sessionRight, totalRight, y);
        }
        screen.drawScrollHint(g, scroll > 0, scroll + fit < rows.size());
    }

    /** 近接はクリティカル数。 */
    private static String statsKey(HitKind kind) {
        return kind == HitKind.MELEE ? "weakspot.stats.critHits" : "weakspot.stats." + kind.key() + "Hits";
    }

    private int row(GuiGraphics g, String labelKey, Function<MiningStats, String> value, int labelX, int sessionRight,
                    int totalRight, int y) {
        screen.drawString(g, I18n.get(labelKey), labelX, y, 0xFFFFFF);
        String tooltipKey = labelKey + ".tooltip";
        if (I18n.exists(tooltipKey) && screen.mouseY() >= y && screen.mouseY() < y + ROW_HEIGHT
                && screen.mouseX() >= labelX && screen.mouseX() <= totalRight) {
            screen.setTooltip(I18n.get(tooltipKey));
        }
        screen.drawRight(g, session == null ? "..." : value.apply(session), sessionRight, y, 0xFFFFFF);
        screen.drawRight(g, total == null ? "..." : value.apply(total), totalRight, y, 0xFFFF55);
        return y + ROW_HEIGHT;
    }

    private static String formatAverage(MiningStats stats) {
        double average = stats.averageHitsPerBlock();
        return Double.isNaN(average) ? "-" : String.format("%.2f", average);
    }

    private static String formatDuration(double seconds) {
        if (seconds < 60) {
            return I18n.get("weakspot.stats.time.seconds", String.format("%.1f", seconds));
        }
        long whole = (long) seconds;
        if (whole < 3600) {
            return I18n.get("weakspot.stats.time.minutes", whole / 60, String.format("%02d", whole % 60));
        }
        return I18n.get("weakspot.stats.time.hours", whole / 3600, String.format("%02d", whole / 60 % 60));
    }
}
