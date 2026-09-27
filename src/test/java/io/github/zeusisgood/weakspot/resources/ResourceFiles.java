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

/** テストから、リポジトリの中のファイル（翻訳・README など）を読む。作業ディレクトリはリポジトリの直下。 */
final class ResourceFiles {

    static final String EN_US = "src/main/resources/assets/weakspot/lang/en_us.lang";
    static final String JA_JP = "src/main/resources/assets/weakspot/lang/ja_jp.lang";

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

    /** 翻訳ファイルのキーと値（空行と # の行は飛ばす）。同じキーが 2 回あれば、2 回目を "\u0000dup" として残す。 */
    static Map<String, String> lang(String path) {
        Map<String, String> entries = new LinkedHashMap<>();
        for (String line : lines(path)) {
            if (line.isEmpty() || line.startsWith("#") || line.indexOf('=') < 0) {
                continue;
            }
            String key = line.substring(0, line.indexOf('='));
            String value = line.substring(line.indexOf('=') + 1);
            entries.put(entries.containsKey(key) ? key + "\u0000dup" : key, value);
        }
        return entries;
    }
}
