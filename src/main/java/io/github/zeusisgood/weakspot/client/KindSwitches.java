package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.KindMask;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;

/**
 * 自分の弱点の種類ごとのオン・オフ（1.7.0。統計画面の「弱点マーカー」タブ、設定 disabledKinds）。
 * HOME キーの一時オフ（weakSpotsEnabled）がオフなら、どの種類もオフ。
 */
public final class KindSwitches {

    /** 最後に読んだ disabledKinds の配列と、そのビット（設定画面で配列が置き換わったら読み直す）。 */
    private static String[] cachedKeys;
    private static int cachedMask;

    private KindSwitches() {
    }

    /** その種類の弱点を出してよいか。 */
    public static boolean isEnabled(HitKind kind) {
        // 的当て（1.11.0）の間は、ほかの種類の弱点を出さない
        return WeakSpotConfig.client.markers.weakSpotsEnabled && !KindMask.isDisabled(disabledMask(), kind)
                && !TargetPlay.active();
    }

    /** 自分でオフにした種類のビット（一時オフとは別）。 */
    static int disabledMask() {
        String[] keys = WeakSpotConfig.client.markers.disabledKinds;
        if (keys != cachedKeys) {
            cachedKeys = keys;
            cachedMask = KindMask.fromKeys(keys);
        }
        return cachedMask;
    }

    /** 自分でオフにしているか（一時オフは見ない。「弱点マーカー」タブの表示）。 */
    static boolean isDisabledByPlayer(HitKind kind) {
        return KindMask.isDisabled(disabledMask(), kind);
    }

    /** 「弱点マーカー」タブのボタンで切り替えて、保存する。 */
    static void toggle(HitKind kind) {
        WeakSpotConfig.client.markers.disabledKinds = KindMask.toKeys(KindMask.toggled(disabledMask(), kind));
        WeakSpotConfig.save();
        if (!isEnabled(kind)) {
            ClientWeakSpotHandler.stopOwnWeakSpots();
        }
    }
}
