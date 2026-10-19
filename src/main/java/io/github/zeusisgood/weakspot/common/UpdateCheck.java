package io.github.zeusisgood.weakspot.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateCheck {

    private static final Pattern MAJOR_MINOR = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\..*)?$");

    private UpdateCheck() {
    }

    /** skipped は「この版は通知しない」を押した版。 */
    public static boolean shouldNotify(boolean newerAvailable, String target, String skipped) {
        if (!newerAvailable || target == null || target.trim().isEmpty()) {
            return false;
        }
        return skipped == null || !target.trim().equals(skipped.trim());
    }

    /** 違うとサーバーと接続できない。数字でない形なら false。 */
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
