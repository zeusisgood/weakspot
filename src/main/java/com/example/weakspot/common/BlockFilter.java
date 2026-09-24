package com.example.weakspot.common;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 設定に書く「ブロック名と、状態の条件」の1行（SPEC_v1.4 §1）。Minecraft に依存しない。
 * 書き方は {@code modid:block} か {@code modid:block[property=pattern,property=pattern]}。
 * pattern は、数の範囲 {@code a-b}、{@code *} を含む形（{@code *} は任意の文字列）、それ以外は完全一致。
 */
public final class BlockFilter {

    /** ブロックの登録名。 */
    public final String block;
    /** プロパティの名前 → 値の条件（書いた順）。空なら条件なし。 */
    public final Map<String, String> conditions;

    private BlockFilter(String block, Map<String, String> conditions) {
        this.block = block;
        this.conditions = Collections.unmodifiableMap(conditions);
    }

    public boolean hasConditions() {
        return !conditions.isEmpty();
    }

    /** 1行を読む。読めない行は null。前後と区切りの空白は無視する。 */
    public static BlockFilter parse(String entry) {
        if (entry == null) {
            return null;
        }
        String text = entry.trim();
        int open = text.indexOf('[');
        String block = open < 0 ? text : text.substring(0, open).trim();
        if (block.isEmpty() || block.indexOf(']') >= 0 || block.indexOf('=') >= 0 || block.indexOf(',') >= 0) {
            return null;
        }
        Map<String, String> conditions = new LinkedHashMap<>();
        if (open >= 0) {
            if (!text.endsWith("]")) {
                return null;
            }
            String inside = text.substring(open + 1, text.length() - 1).trim();
            if (inside.isEmpty() || inside.indexOf('[') >= 0 || inside.indexOf(']') >= 0) {
                return null;
            }
            for (String part : inside.split(",")) {
                int eq = part.indexOf('=');
                if (eq < 0) {
                    return null;
                }
                String property = part.substring(0, eq).trim();
                String pattern = part.substring(eq + 1).trim();
                if (property.isEmpty() || pattern.isEmpty()) {
                    return null;
                }
                conditions.put(property, pattern);
            }
        }
        return new BlockFilter(block, conditions);
    }

    /** 状態（プロパティの名前 → 値の名前）が、すべての条件を満たすか。条件にあるプロパティがなければ満たさない。 */
    public boolean matches(Map<String, String> properties) {
        for (Map.Entry<String, String> condition : conditions.entrySet()) {
            String value = properties.get(condition.getKey());
            if (value == null || !matchesValue(condition.getValue(), value)) {
                return false;
            }
        }
        return true;
    }

    /** 値 value が条件 pattern を満たすか。 */
    public static boolean matchesValue(String pattern, String value) {
        int[] range = range(pattern);
        if (range != null) {
            try {
                int number = Integer.parseInt(value);
                return number >= range[0] && number <= range[1];
            } catch (NumberFormatException e) {
                return false;
            }
        }
        if (pattern.indexOf('*') >= 0) {
            return glob(pattern, value);
        }
        return pattern.equals(value);
    }

    /** {@code a-b}（a, b は 0 以上の整数）なら {a, b}。それ以外は null。 */
    private static int[] range(String pattern) {
        int dash = pattern.indexOf('-');
        if (dash <= 0 || dash == pattern.length() - 1) {
            return null;
        }
        String from = pattern.substring(0, dash);
        String to = pattern.substring(dash + 1);
        if (!isDigits(from) || !isDigits(to)) {
            return null;
        }
        try {
            return new int[] {Integer.parseInt(from), Integer.parseInt(to)};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isDigits(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) {
                return false;
            }
        }
        return !text.isEmpty();
    }

    /** {@code *} を任意の文字列として、全体が一致するか。 */
    private static boolean glob(String pattern, String value) {
        String[] parts = pattern.split("\\*", -1);
        if (!value.startsWith(parts[0])) {
            return false;
        }
        int at = parts[0].length();
        for (int i = 1; i < parts.length - 1; i++) {
            int found = value.indexOf(parts[i], at);
            if (found < 0) {
                return false;
            }
            at = found + parts[i].length();
        }
        String last = parts[parts.length - 1];
        return value.length() - at >= last.length() && value.endsWith(last);
    }
}
