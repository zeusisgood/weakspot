package io.github.zeusisgood.weakspot.server;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * 古いクライアントは新しい翻訳キーを持っていないので、翻訳キーで送るとキーのまま出てしまう。そこで Mod の jar の中の
 * 言語ファイルを読み、プレイヤーの言語の設定で文章にする。その言語のファイル・キーがなければ en_us。
 */
final class ServerLang {

    private static final String FALLBACK = "en_us";
    private static final Map<String, Map<String, String>> CACHE = new HashMap<>();

    private ServerLang() {
    }

    static String format(ServerPlayer player, String key, Object... args) {
        String text = lang(player.getLanguage().toLowerCase(Locale.ROOT)).get(key);
        if (text == null) {
            text = lang(FALLBACK).getOrDefault(key, key);
        }
        return String.format(text, args);
    }

    private static synchronized Map<String, String> lang(String language) {
        return CACHE.computeIfAbsent(language, ServerLang::load);
    }

    private static Map<String, String> load(String language) {
        String path = "/assets/" + WeakSpotMod.MODID + "/lang/" + language + ".json";
        try (InputStream in = ServerLang.class.getResourceAsStream(path)) {
            if (in == null) {
                return new HashMap<>();
            }
            Map<String, String> entries = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8),
                    new TypeToken<Map<String, String>>() { }.getType());
            return entries == null ? new HashMap<>() : entries;
        } catch (IOException | RuntimeException e) {
            // 読めなければ、キーのまま
            return new HashMap<>();
        }
    }
}
