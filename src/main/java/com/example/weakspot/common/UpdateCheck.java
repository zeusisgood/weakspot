package com.example.weakspot.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 新しい版の通知（1.9.2）の判定。Forge の更新確認の結果を、Minecraft に依存しない形で受け取る。 */
public final class UpdateCheck {

    /** 先頭の「メジャー.マイナー」。 */
    private static final Pattern MAJOR_MINOR = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\..*)?$");

    private UpdateCheck() {
    }

    /** 新しい版があり、それが「この版は通知しない」を押した版でなければ、通知する。 */
    public static boolean shouldNotify(boolean newerAvailable, String target, String skipped) {
        if (!newerAvailable || target == null || target.trim().isEmpty()) {
            return false;
        }
        return skipped == null || !target.trim().equals(skipped.trim());
    }

    /** 今の版と新しい版で、メジャーかマイナーが違うか（違うとサーバーと接続できない）。数字でない形なら false。 */
    public static boolean differentMinor(String current, String target) {
        if (current == null || target == null) {
            return false;
        }
        Matcher a = MAJOR_MINOR.matcher(current.trim());
        Matcher b = MAJOR_MINOR.matcher(target.trim());
        if (!a.matches() || !b.matches()) {
            return false;
        }
        return !a.group(1).equals(b.group(1)) || Integer.parseInt(a.group(2)) != Integer.parseInt(b.group(2));
    }
}
