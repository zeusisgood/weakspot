package com.example.weakspot.common;

/** ヒットの種類。統計は種類ごとに数える。耐久回復は採掘だけ、節目は採掘・種類ごと・合計（1.7.0）。 */
public enum HitKind {
    /** 左クリックの長押しで壊すブロック。 */
    MINING(0xFF5926, true),
    /** 素手で右クリックを押しっぱなしにした作物・苗木。 */
    GROWTH(0xFF5926, false),
    /** しゃがんで両手が空のまま、右クリックを押しっぱなしにした機械。 */
    MACHINE(0xFF5926, false),
    /** 素手で右クリックを押しっぱなしにした動物（子どもの成長、繁殖の待ち時間、羊毛、卵、村人の取引上限）。 */
    ANIMAL(0xFF5926, false),
    /** 釣り竿を持って、浮きのまわりの弱点を左クリックで叩く。 */
    FISHING(0xFF5926, false),
    /** 弓を引いている間、照準の近くに出る弱点に照準を合わせる。 */
    BOW(0xFF5926, false),
    /** 剣か斧を持って敵の近くにいる間、照準の左右に出る弱点に照準を合わせて溜め、次の攻撃を強くする（1.8.0。それまでは敵の体の弱点）。 */
    MELEE(0xD0D8E0, true),
    /** 馬・豚・トロッコ・ボートに乗って、照準の近くに出る弱点に照準を合わせる（1.6.0）。 */
    VEHICLE(0x55CCFF, true),
    /** 食べる・飲む間、照準の近くに出る弱点に照準を合わせる（1.6.0）。 */
    EAT(0x7CFC00, false),
    /** 夜にベッドで寝ている間、寝ている画面に出るマーカーをクリックする（1.6.0）。 */
    SLEEP(0xFFF1A8, false),
    /** はしご・ツタを登り降りしている間、照準の真上か真下に出る弱点に照準を合わせる（1.7.0）。 */
    LADDER(0xC8A060, true),
    /** エリトラで飛んでいる間、照準の近くに出る弱点に照準を合わせる（1.7.0）。 */
    ELYTRA(0x7FB2FF, true),
    /** エンチャント台の画面に出るマーカーをクリックする（1.7.0）。 */
    ENCHANT(0xB070FF, false),
    /** 素手で右クリックを押しっぱなしにした、実った作物（1.7.0）。 */
    HARVEST(0xFF3DCB, true),
    /** 投げる物を持っている間、照準の近くに出る弱点に照準を合わせる（1.7.0）。 */
    THROW(0x2ED3B7, true),
    /** 走っている間、照準の真上か真下に出る弱点に照準を合わせる（1.7.0）。 */
    SPRINT(0xFF5A5F, true),
    /** ネザーゲートの中に立っている間、照準の近くに出る弱点に照準を合わせる（1.8.0）。 */
    PORTAL(0xFFD23F, true);

    private final int defaultColor;
    private final boolean usesComboFactor;

    HitKind(int defaultColor, boolean usesComboFactor) {
        this.defaultColor = defaultColor;
        this.usesComboFactor = usesComboFactor;
    }

    /**
     * 自分の弱点の初期値の色（0xRRGGBB。「弱点マーカー」タブで書き換えていないとき。1.8.9 で MarkerLook から移した）。
     * ブロック・生き物・弓・釣りの弱点は橙赤、収穫は作物と重ならないマゼンタ、ほかは種類ごとの色。
     */
    public int defaultColor() {
        return defaultColor;
    }

    /**
     * 効果がコンボの掛け数（ComboFactor）で強くなり、コンボ表示の下に「ダッシュ ×1.25」を出すか（1.8.9 で ComboHud から移した）。
     * 機械も掛け数を使うが、実際の速さ「機械 4倍速」を出すので false。
     */
    public boolean usesComboFactor() {
        return usesComboFactor;
    }

    /** 設定・翻訳キーに使う名前（小文字。"harvest" など）。 */
    public String key() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /** key() から種類を探す。知らない名前なら null。 */
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

    /** 通信用。範囲外なら null。 */
    public static HitKind byId(int id) {
        HitKind[] all = values();
        return id >= 0 && id < all.length ? all[id] : null;
    }
}
