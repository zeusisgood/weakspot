package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.KindMask;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;

/** HOME キーの一時オフ（weakSpotsEnabled）がオフなら、どの種類もオフ。 */
public final class KindSwitches {

    /** 設定画面で配列が置き換わったら読み直す。 */
    private static String[] cachedKeys;
    private static int cachedMask;

    private KindSwitches() {
    }

    public static boolean isEnabled(HitKind kind) {
        return WeakSpotConfig.client.markers.weakSpotsEnabled && !KindMask.isDisabled(disabledMask(), kind)
                && !TargetPlay.active();
    }

    /** 一時オフとは別。 */
    static int disabledMask() {
        String[] keys = WeakSpotConfig.client.markers.disabledKinds;
        if (keys != cachedKeys) {
            cachedKeys = keys;
            cachedMask = KindMask.fromKeys(keys);
        }
        return cachedMask;
    }

    /** 一時オフは見ない（「弱点マーカー」タブの表示）。 */
    static boolean isDisabledByPlayer(HitKind kind) {
        return KindMask.isDisabled(disabledMask(), kind);
    }

    static void toggle(HitKind kind) {
        WeakSpotConfig.client.markers.disabledKinds = KindMask.toKeys(KindMask.toggled(disabledMask(), kind));
        WeakSpotConfig.save();
        if (!isEnabled(kind)) {
            ClientWeakSpotHandler.stopOwnWeakSpots();
        }
    }
}
