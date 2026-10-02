package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.TargetRules;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;

/**
 * 統計画面の「的当て」タブ（1.11.0）。自己ベスト・回数・ご褒美の段階と次の段階まで・ご褒美の一覧（使えるか）・
 * サーバー内の上位 10 人。金の粒（金の段階のご褒美）のオン・オフもここで切り替える。値はサーバーから届いたもの（TargetRecords）。
 */
final class TargetTab extends StatsScreenTab {

    private static final int BUTTON_GOLD_PARTICLES = 700;
    private static final int ROW_HEIGHT = 11;
    /** 金の粒のオン・オフのボタン（高さ 20 なので、その行だけ上下に間をあけて、ほかの行に重ねない）。 */
    private static final int GOLD_BUTTON_WIDTH = 60;
    private static final int GOLD_ROW_GAP = 6;

    private GuiButton goldButton;

    TargetTab(StatsScreen screen) {
        super(screen);
    }

    @Override
    void init() {
        goldButton = screen.add(new GuiButton(BUTTON_GOLD_PARTICLES,
                screen.width / 2 + 150 - GOLD_BUTTON_WIDTH, 0, GOLD_BUTTON_WIDTH, 20, ""));
        goldButton.visible = false;
    }

    @Override
    void show(boolean shown) {
        goldButton.visible = shown;
        updateLabel();
    }

    private void updateLabel() {
        goldButton.enabled = TargetRecords.unlocked(TargetRules.Tier.GOLD);
        goldButton.displayString = I18n.format(WeakSpotConfig.client.markers.goldHitParticles
                ? "weakspot.kinds.on" : "weakspot.kinds.off");
    }

    @Override
    boolean action(int id) {
        if (id != BUTTON_GOLD_PARTICLES) {
            return false;
        }
        WeakSpotConfig.client.markers.goldHitParticles = !WeakSpotConfig.client.markers.goldHitParticles;
        WeakSpotConfig.save();
        updateLabel();
        return true;
    }

    @Override
    void draw() {
        int left = screen.width / 2 - 150;
        int right = screen.width / 2 + 150;
        int y = screen.top() + 44;
        TargetRules.Tier tier = TargetRecords.tier();
        screen.drawString(screen.font(), I18n.format("weakspot.target.tab.best"), left, y, 0xFFFFFF);
        screen.drawRight(I18n.format("weakspot.combo.hit", TargetRecords.best), right, y, 0xFFFF55);
        y += ROW_HEIGHT;
        screen.drawString(screen.font(), I18n.format("weakspot.target.tab.rounds"), left, y, 0xFFFFFF);
        screen.drawRight(Long.toString(TargetRecords.rounds), right, y, 0xFFFF55);
        y += ROW_HEIGHT;
        screen.drawString(screen.font(), I18n.format("weakspot.target.tab.tier"), left, y, 0xFFFFFF);
        TargetRules.Tier next = tier.next();
        String tierText = tier == TargetRules.Tier.NONE ? I18n.format("weakspot.target.tier.none")
                : I18n.format("weakspot.target.tier." + TargetPlay.tierKey(tier));
        if (next != null) {
            tierText += "  " + I18n.format("weakspot.target.tab.next",
                    I18n.format("weakspot.target.tier." + TargetPlay.tierKey(next)), next.from - TargetRecords.best);
        }
        screen.drawRight(tierText, right, y, tierRgb(tier));
        y += ROW_HEIGHT + 4;

        // ご褒美（解放したもの、まだのもの）
        screen.drawString(screen.font(), I18n.format("weakspot.target.tab.rewards"), left, y, 0xAAAAAA);
        y += ROW_HEIGHT;
        for (TargetRules.Tier t : TargetRules.Tier.values()) {
            if (t == TargetRules.Tier.NONE) {
                continue;
            }
            if (t == TargetRules.Tier.GOLD) {
                y += GOLD_ROW_GAP;
            }
            boolean open = TargetRecords.unlocked(t);
            String line = (open ? "✔ " : "🔒 ") + I18n.format("weakspot.target.reward." + TargetPlay.tierKey(t));
            screen.drawString(screen.font(), line, left + 6, y, open ? 0xFFFFFF : 0x888888);
            String need = I18n.format("weakspot.target.tab.unlockAt",
                    I18n.format("weakspot.target.tier." + TargetPlay.tierKey(t)), t.from);
            if (t == TargetRules.Tier.GOLD) {
                goldButton.y = y - 6;
                screen.drawRight(need, right - GOLD_BUTTON_WIDTH - 6, y, tierRgb(t));
            } else {
                screen.drawRight(need, right, y, tierRgb(t));
            }
            y += t == TargetRules.Tier.GOLD ? ROW_HEIGHT + GOLD_ROW_GAP + 2 : ROW_HEIGHT;
        }
        y += 4;

        // サーバー内の上位
        screen.drawString(screen.font(), I18n.format("weakspot.target.tab.top"), left, y, 0xAAAAAA);
        y += ROW_HEIGHT;
        if (TargetRecords.TOP_NAMES.isEmpty()) {
            screen.drawString(screen.font(), I18n.format("weakspot.target.tab.noRecords"), left + 6, y, 0x888888);
            return;
        }
        for (int i = 0; i < TargetRecords.TOP_NAMES.size() && y < screen.bottom() - 6; i++) {
            screen.drawString(screen.font(), (i + 1) + ". " + TargetRecords.TOP_NAMES.get(i), left + 6, y,
                    i == 0 ? 0xFFD700 : 0xFFFFFF);
            screen.drawRight(I18n.format("weakspot.combo.hit", TargetRecords.TOP_SCORES.get(i)), right, y, 0xFFFF55);
            y += ROW_HEIGHT;
        }
    }

    private static int tierRgb(TargetRules.Tier tier) {
        return tier == TargetRules.Tier.RAINBOW ? MarkerLook.rainbowRgb() : tier == TargetRules.Tier.NONE ? 0xAAAAAA
                : tier.rgb;
    }
}
