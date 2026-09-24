package com.example.weakspot.common;

/** ヒットの種類。統計は種類ごとに数え、節目と耐久回復は採掘だけが対象。 */
public enum HitKind {
    /** 左クリックの長押しで壊すブロック。 */
    MINING,
    /** 素手で右クリックを押しっぱなしにした作物・苗木。 */
    GROWTH,
    /** しゃがんで両手が空のまま、右クリックを押しっぱなしにした機械。 */
    MACHINE;

    /** 通信用。範囲外なら null。 */
    public static HitKind byId(int id) {
        HitKind[] all = values();
        return id >= 0 && id < all.length ? all[id] : null;
    }
}
