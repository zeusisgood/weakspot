package io.github.zeusisgood.weakspot.resources;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** テストから、リポジトリの中のファイル（翻訳・README など）を読む。作業ディレクトリはリポジトリの直下（build.gradle の test）。 */
final class ResourceFiles {

    static final String EN_US = "lang/en_us.json";
    static final String JA_JP = "lang/ja_jp.json";
    /** 1.12.2 のビルドのディレクトリ（Minecraft の版ごとのソース）。 */
    static final String MC = "mc1.12.2/";

    private ResourceFiles() {
    }

    static String read(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static List<String> lines(String path) {
        try {
            return Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Path path(String path) {
        return Paths.get(path);
    }

    /**
     * 翻訳の元データのキーと値。1 行に 1 つ（"キー": "値"）の形だけを読む（1.12.2 のビルドも同じ形を前提に .lang を作る）。
     * 同じキーが 2 回あれば、2 回目を "\u0000dup" として残す。
     */
    static Map<String, String> lang(String path) {
        Map<String, String> entries = new LinkedHashMap<>();
        for (String line : lines(path)) {
            String t = line.trim();
            if (t.isEmpty() || t.equals("{") || t.equals("}")) {
                continue;
            }
            int[] at = {0};
            String key = jsonString(t, at, path);
            if (!t.startsWith(": ", at[0])) {
                throw new AssertionError(path + ": not \"key\": \"value\": " + line);
            }
            at[0] += 2;
            String value = jsonString(t, at, path);
            String rest = t.substring(at[0]);
            if (!rest.isEmpty() && !rest.equals(",")) {
                throw new AssertionError(path + ": one entry per line: " + line);
            }
            entries.put(entries.containsKey(key) ? key + "\u0000dup" : key, value);
        }
        return entries;
    }

    /** at[0] の位置の JSON の文字列を読み、at[0] をその後ろに進める。 */
    private static String jsonString(String text, int[] at, String path) {
        if (at[0] >= text.length() || text.charAt(at[0]) != '"') {
            throw new AssertionError(path + ": expected a string: " + text);
        }
        StringBuilder out = new StringBuilder();
        int i = at[0] + 1;
        while (i < text.length() && text.charAt(i) != '"') {
            char c = text.charAt(i++);
            if (c != '\\') {
                out.append(c);
                continue;
            }
            char e = text.charAt(i++);
            switch (e) {
                case 'n':
                    out.append('\n');
                    break;
                case 't':
                    out.append('\t');
                    break;
                case 'r':
                    out.append('\r');
                    break;
                case 'b':
                    out.append('\b');
                    break;
                case 'f':
                    out.append('\f');
                    break;
                case 'u':
                    out.append((char) Integer.parseInt(text.substring(i, i + 4), 16));
                    i += 4;
                    break;
                default:
                    out.append(e);
            }
        }
        if (i >= text.length()) {
            throw new AssertionError(path + ": unterminated string: " + text);
        }
        at[0] = i + 1;
        return out.toString();
    }
}
