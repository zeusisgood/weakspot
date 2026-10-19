package io.github.zeusisgood.weakspot.common;

/** オフの種類を HitKind の番号のビットで持つ（SwitchMessage で送る）。 */
public final class KindMask {

    private KindMask() {
    }

    /** keys は設定 disabledKinds。知らない名前は無視する。 */
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

    /** HitKind の順。 */
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

    public static int toggled(int mask, HitKind kind) {
        return mask ^ bit(kind);
    }
}
