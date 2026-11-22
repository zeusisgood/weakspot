package io.github.zeusisgood.weakspot.resources;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

    /** generateLang が作った 1.12.2 の .lang（name は en_us など）。キー=値でない行があれば例外。 */
    static Map<String, String> generatedLang(String name) {
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(MC + "build/generated/lang/assets/weakspot/lang/" + name + ".lang"),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Map<String, String> entries = new LinkedHashMap<>();
        for (String line : lines) {
            int eq = line.indexOf('=');
            if (eq <= 0) {
                throw new IllegalStateException(name + ".lang: not a key=value line: " + line);
            }
            entries.put(line.substring(0, eq), line.substring(eq + 1));
        }
        return entries;
    }

    /** Gson は同じキーが 2 回あると例外を投げるので、重複もここで見つかる。 */
    static Map<String, String> lang(String path) {
        return new Gson().fromJson(read(path), new TypeToken<LinkedHashMap<String, String>>() { }.getType());
    }
}
