package io.github.zeusisgood.weakspot.resources;

import static org.junit.Assert.assertTrue;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

/** 翻訳の抜け（片方の言語だけにあるキー、設定の説明、種類の名前、コードが使うキー）を見つける。 */
public class LangFilesTest {

    private static final Map<String, String> EN = ResourceFiles.lang(ResourceFiles.EN_US);
    private static final Map<String, String> JA = ResourceFiles.lang(ResourceFiles.JA_JP);

    @Test
    public void bothLanguagesHaveTheSameKeys() {
        Set<String> onlyEn = new TreeSet<>(EN.keySet());
        onlyEn.removeAll(JA.keySet());
        Set<String> onlyJa = new TreeSet<>(JA.keySet());
        onlyJa.removeAll(EN.keySet());
        assertTrue("only in en_us.lang: " + onlyEn, onlyEn.isEmpty());
        assertTrue("only in ja_jp.lang: " + onlyJa, onlyJa.isEmpty());
    }

    @Test
    public void noKeyIsWrittenTwice() {
        assertTrue("duplicated in en_us.lang: " + duplicates(EN), duplicates(EN).isEmpty());
        assertTrue("duplicated in ja_jp.lang: " + duplicates(JA), duplicates(JA).isEmpty());
    }

    @Test
    public void noValueIsEmpty() {
        List<String> empty = new ArrayList<>();
        EN.forEach((k, v) -> { if (v.trim().isEmpty()) empty.add("en_us " + k); });
        JA.forEach((k, v) -> { if (v.trim().isEmpty()) empty.add("ja_jp " + k); });
        assertTrue("empty values: " + empty, empty.isEmpty());
    }

    /** 設定画面の説明: weakspot.general.<キーを小文字にしたもの>.tooltip（CLAUDE.md の設定の約束事）。 */
    @Test
    public void everySettingHasATooltip() {
        List<String> missing = new ArrayList<>();
        for (Field field : WeakSpotConfig.class.getFields()) {
            int modifiers = field.getModifiers();
            if (!Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
                continue;
            }
            String key = "weakspot.general." + field.getName().toLowerCase(Locale.ROOT) + ".tooltip";
            if (!EN.containsKey(key) || !JA.containsKey(key)) {
                missing.add(key);
            }
        }
        assertTrue("settings without a tooltip: " + missing, missing.isEmpty());
    }

    /** 種類の名前（weakspot.kind.<key>）と、統計の行の名前（StatsTab.statsKey と同じ決まり）。 */
    @Test
    public void everyKindHasItsNames() {
        List<String> missing = new ArrayList<>();
        for (HitKind kind : HitKind.values()) {
            String name = "weakspot.kind." + kind.key();
            String stats = kind == HitKind.MINING ? "weakspot.stats.hits"
                    : kind == HitKind.MELEE ? "weakspot.stats.critHits" : "weakspot.stats." + kind.key() + "Hits";
            for (String key : new String[] {name, stats}) {
                if (!EN.containsKey(key)) {
                    missing.add(key);
                }
            }
        }
        assertTrue("kinds without a name: " + missing, missing.isEmpty());
    }

    /** コードに文字列でそのまま書いた翻訳キー（"weakspot.xxx"。後ろに + で続きを足すものは除く）が、翻訳にある。 */
    @Test
    public void keysWrittenInTheCodeExist() throws IOException {
        Pattern literal = Pattern.compile("\"(weakspot\\.[A-Za-z0-9_.]*[A-Za-z0-9_])\"(\\s*\\+)?");
        Set<String> missing = new TreeSet<>();
        List<java.nio.file.Path> sources;
        try (Stream<java.nio.file.Path> walk = Files.walk(ResourceFiles.path("src/main/java"))) {
            sources = walk.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
        }
        for (java.nio.file.Path source : sources) {
            Matcher m = literal.matcher(ResourceFiles.read(source.toString()));
            while (m.find()) {
                if (m.group(2) == null && !EN.containsKey(m.group(1))) {
                    missing.add(m.group(1) + " (" + source.getFileName() + ")");
                }
            }
        }
        assertTrue("keys used in the code but missing from the lang files: " + missing, missing.isEmpty());
    }

    /** サーバーから翻訳キーで送る文（1.9.6。PlayerText）は、クライアントの翻訳で使える %s / %1$s の形だけを使う。 */
    @Test
    public void serverSentTextsUseOnlyPercentS() {
        Pattern format = Pattern.compile("%(?!(\\d+\\$)?s)");
        List<String> bad = new ArrayList<>();
        for (Map<String, String> entries : java.util.Arrays.asList(EN, JA)) {
            entries.forEach((key, value) -> {
                if ((key.startsWith("weakspot.version.") || key.startsWith("weakspot.breed."))
                        && format.matcher(value).find()) {
                    bad.add(key + "=" + value);
                }
            });
        }
        assertTrue("server-sent texts with formats other than %s: " + bad, bad.isEmpty());
    }

    /** チャットの頭は、日英とも [WeakSpot]（1.9.6。コマンド /weakspot と同じ綴り）。 */
    @Test
    public void chatPrefixIsWeakSpot() {
        List<String> old = new ArrayList<>();
        for (Map<String, String> entries : java.util.Arrays.asList(EN, JA)) {
            entries.forEach((key, value) -> {
                if (value.contains("[弱点]") || value.contains("[Weak Spot]")) {
                    old.add(key);
                }
            });
        }
        assertTrue("old chat prefixes: " + old, old.isEmpty());
    }

    private static Set<String> duplicates(Map<String, String> entries) {
        Set<String> found = new TreeSet<>();
        for (String key : entries.keySet()) {
            if (key.endsWith("\u0000dup")) {
                found.add(key.substring(0, key.length() - 4));
            }
        }
        return found;
    }
}
