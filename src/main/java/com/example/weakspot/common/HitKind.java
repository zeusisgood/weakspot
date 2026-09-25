package com.example.weakspot.common;

/** ヒットの種類。統計は種類ごとに数える。耐久回復は採掘だけ、節目は採掘・種類ごと・合計（1.7.0）。 */
public enum HitKind {
    /** 左クリックの長押しで壊すブロック。 */
    MINING,
    /** 素手で右クリックを押しっぱなしにした作物・苗木。 */
    GROWTH,
    /** しゃがんで両手が空のまま、右クリックを押しっぱなしにした機械。 */
    MACHINE,
    /** 素手で右クリックを押しっぱなしにした動物（子どもの成長、繁殖の待ち時間、羊毛、卵、村人の取引上限）。 */
    ANIMAL,
    /** 釣り竿を持って、浮きのまわりの弱点を左クリックで叩く。 */
    FISHING,
    /** 弓を引いている間、照準の近くに出る弱点に照準を合わせる。 */
    BOW,
    /** 剣か斧を持って敵の近くにいる間、照準の左右に出る弱点に照準を合わせて溜め、次の攻撃を強くする（1.8.0。それまでは敵の体の弱点）。 */
    MELEE,
    /** 馬・豚・トロッコ・ボートに乗って、照準の近くに出る弱点に照準を合わせる（1.6.0）。 */
    VEHICLE,
    /** 食べる・飲む間、照準の近くに出る弱点に照準を合わせる（1.6.0）。 */
    EAT,
    /** 夜にベッドで寝ている間、寝ている画面に出るマーカーをクリックする（1.6.0）。 */
    SLEEP,
    /** はしご・ツタを登り降りしている間、照準の真上か真下に出る弱点に照準を合わせる（1.7.0）。 */
    LADDER,
    /** エリトラで飛んでいる間、照準の近くに出る弱点に照準を合わせる（1.7.0）。 */
    ELYTRA,
    /** エンチャント台の画面に出るマーカーをクリックする（1.7.0）。 */
    ENCHANT,
    /** 素手で右クリックを押しっぱなしにした、実った作物（1.7.0）。 */
    HARVEST,
    /** 投げる物を持っている間、照準の近くに出る弱点に照準を合わせる（1.7.0）。 */
    THROW,
    /** 走っている間、照準の真上か真下に出る弱点に照準を合わせる（1.7.0）。 */
    SPRINT,
    /** ネザーゲートの中に立っている間、照準の近くに出る弱点に照準を合わせる（1.8.0）。 */
    PORTAL;

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
