package com.example.weakspot.common;

/** Mod の版（1.5.1 のような「.」区切りの数）を比べる（1.5.1）。 */
public final class ModVersions {

    private ModVersions() {
    }

    /** a が b より古ければ負、新しければ正、同じなら 0。足りない桁は 0、数でない部分は無視する。 */
    public static int compare(String a, String b) {
        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        for (int i = 0; i < Math.max(pa.length, pb.length); i++) {
            int diff = Integer.compare(number(pa, i), number(pb, i));
            if (diff != 0) {
                return diff;
            }
        }
        return 0;
    }

    private static int number(String[] parts, int index) {
        if (index >= parts.length) {
            return 0;
        }
        String digits = parts[index].replaceAll("[^0-9].*$", "");
        try {
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
