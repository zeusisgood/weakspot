package io.github.zeusisgood.weakspot.common;

/** 通信は並びの番号（byId）なので、足すときは末尾に。 */
public enum HitKind {
    MINING(0xFF5926, true),
    GROWTH(0xFF5926, false),
    MACHINE(0xFF5926, false),
    ANIMAL(0xFF5926, false),
    FISHING(0xFF5926, false),
    BOW(0xFF5926, false),
    MELEE(0xD0D8E0, true),
    VEHICLE(0x55CCFF, true),
    EAT(0x7CFC00, false),
    SLEEP(0xFFF1A8, false),
    LADDER(0xC8A060, true),
    ELYTRA(0x7FB2FF, true),
    ENCHANT(0xB070FF, false),
    HARVEST(0xFF3DCB, true),
    THROW(0x2ED3B7, true),
    SPRINT(0xFF5A5F, true),
    PORTAL(0xFFD23F, true),
    SWIM(0x2E6BFF, true),
    FALL(0xEAF6FF, true);

    private final int defaultColor;
    private final boolean usesComboFactor;

    HitKind(int defaultColor, boolean usesComboFactor) {
        this.defaultColor = defaultColor;
        this.usesComboFactor = usesComboFactor;
    }

    /** 0xRRGGBB。「弱点マーカー」タブで書き換えていないときの色。収穫は、作物と重ならないようにマゼンタ。 */
    public int defaultColor() {
        return defaultColor;
    }

    /** 機械も掛け数を使うが、表示は実際の速さ（「機械 4倍速」）なので false。 */
    public boolean usesComboFactor() {
        return usesComboFactor;
    }

    /** 設定 spotSize に掛ける。 */
    public double sizeFactor() {
        return this == FISHING ? 0.75 : 1.0;
    }

    /** 小文字。設定・翻訳キーに使う。 */
    public String key() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /** 知らない名前なら null。 */
    public static HitKind byKey(String key) {
        if (key != null) {
            for (HitKind kind : values()) {
                if (kind.key().equals(key.trim().toLowerCase(java.util.Locale.ROOT))) {
                    return kind;
                }
            }
        }
        return null;
    }

    /** 範囲外なら null。 */
    public static HitKind byId(int id) {
        HitKind[] all = values();
        return id >= 0 && id < all.length ? all[id] : null;
    }
}
