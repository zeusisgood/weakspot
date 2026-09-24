package com.example.weakspot.common;

/** ヒットの種類。統計は種類ごとに数え、節目と耐久回復は採掘だけが対象。 */
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
    /** 敵（IMob）に出た弱点を殴る（クリティカルヒット）。 */
    MELEE;

    /** 通信用。範囲外なら null。 */
    public static HitKind byId(int id) {
        HitKind[] all = values();
        return id >= 0 && id < all.length ? all[id] : null;
    }
}
