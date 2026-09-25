package com.example.weakspot.common;

/**
 * 弱点の種類ごとのオン・オフ（1.7.0）。オフの種類を HitKind の番号のビットで持つ（SwitchMessage で送る）。
 * Minecraft に依存しない。
 */
public final class KindMask {

    private KindMask() {
    }

    /** 設定 disabledKinds（種類の名前の一覧）から、オフの種類のビット。知らない名前は無視する。 */
    public static int fromKeys(String[] keys) {
        int mask = 0;
        if (keys != null) {
            for (String key : keys) {
                HitKind kind = HitKind.byKey(key);
                if (kind != null) {
                    mask |= bit(kind);
                }
            }
        }
        return mask;
    }

    /** オフの種類のビットから、設定に書く名前の一覧（HitKind の順）。 */
    public static String[] toKeys(int mask) {
        java.util.List<String> keys = new java.util.ArrayList<>();
        for (HitKind kind : HitKind.values()) {
            if (isDisabled(mask, kind)) {
                keys.add(kind.key());
            }
        }
        return keys.toArray(new String[0]);
    }

    public static int bit(HitKind kind) {
        return 1 << kind.ordinal();
    }

    public static boolean isDisabled(int mask, HitKind kind) {
        return (mask & bit(kind)) != 0;
    }

    /** kind のオン・オフを切り替えたビット。 */
    public static int toggled(int mask, HitKind kind) {
        return mask ^ bit(kind);
    }
}
