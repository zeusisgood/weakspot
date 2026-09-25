package com.example.weakspot.server;

import com.example.weakspot.Reflect;
import com.example.weakspot.WeakSpotMod;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * サーバーで、受け取るプレイヤーの言語の文章を作る（1.5.1）。古いクライアントは新しい翻訳キーを持っていないので、
 * 翻訳キーで送るとキーのまま出てしまう。そこで Mod の jar の中の言語ファイルを読み、プレイヤーの言語の設定で文章にする。
 * その言語のファイル・キーがなければ en_us。
 */
final class ServerLang {

    private static final String FALLBACK = "en_us";
    /** EntityPlayerMP#language（クライアントの言語の設定。CPacketClientSettings で届く）。 */
    private static final Field LANGUAGE = Reflect.field(EntityPlayerMP.class, "player language",
            "language", "field_71148_cg");
    private static final Map<String, Map<String, String>> CACHE = new HashMap<>();

    private ServerLang() {
    }

    static String format(EntityPlayerMP player, String key, Object... args) {
        String text = lang(languageOf(player)).get(key);
        if (text == null) {
            text = lang(FALLBACK).getOrDefault(key, key);
        }
        return String.format(text, args);
    }

    private static String languageOf(EntityPlayerMP player) {
        if (LANGUAGE != null) {
            try {
                Object value = LANGUAGE.get(player);
                if (value instanceof String) {
                    return ((String) value).toLowerCase(Locale.ROOT);
                }
            } catch (IllegalAccessException e) {
                // en_us にする
            }
        }
        return FALLBACK;
    }

    private static synchronized Map<String, String> lang(String language) {
        return CACHE.computeIfAbsent(language, ServerLang::load);
    }

    private static Map<String, String> load(String language) {
        Map<String, String> entries = new HashMap<>();
        String path = "/assets/" + WeakSpotMod.MODID + "/lang/" + language + ".lang";
        try (InputStream in = ServerLang.class.getResourceAsStream(path)) {
            if (in == null) {
                return entries;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line; (line = reader.readLine()) != null; ) {
                int eq = line.indexOf('=');
                if (eq > 0 && !line.startsWith("#")) {
                    entries.put(line.substring(0, eq), line.substring(eq + 1));
                }
            }
        } catch (IOException e) {
            // 読めなければ、キーのまま
        }
        return entries;
    }
}
