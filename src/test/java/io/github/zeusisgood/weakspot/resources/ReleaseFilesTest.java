package io.github.zeusisgood.weakspot.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/**
 * リリースの手順（doc/development.md）で、版を上げるときに直すファイルが揃っているか。
 * 版の直し忘れ・お知らせの書き忘れを、PR の CI で見つける。
 */
public class ReleaseFilesTest {

    private static final String VERSION = find(ResourceFiles.read("build.gradle"), "(?m)^version = '([^']+)'");
    private static final String MOD_SOURCE =
            ResourceFiles.read("src/main/java/io/github/zeusisgood/weakspot/WeakSpotMod.java");

    @Test
    public void modVersionMatchesBuildGradle() {
        assertEquals("WeakSpotMod.VERSION", VERSION, find(MOD_SOURCE, "String VERSION = \"([^\"]+)\""));
    }

    /** ACCEPTED_VERSIONS は [今のマイナー,次のマイナー)。 */
    @Test
    public void acceptedVersionsCoverTheCurrentMinor() {
        String[] parts = VERSION.split("\\.");
        String minor = parts[0] + "." + parts[1];
        String next = parts[0] + "." + (Integer.parseInt(parts[1]) + 1);
        assertEquals("WeakSpotMod.ACCEPTED_VERSIONS", "[" + minor + "," + next + ")",
                find(MOD_SOURCE, "String ACCEPTED_VERSIONS = \"([^\"]+)\""));
    }

    /** README のダウンロードは配布サイトへのリンクだけ（版ごとに直さない）。「最近の更新」は版ごとに足す。 */
    @Test
    public void readmeHasTheCurrentVersionInRecentUpdates() {
        assertTrue("README.md: 最近の更新 ### " + VERSION,
                ResourceFiles.read("README.md").contains("\n### " + VERSION + "\n"));
    }

    @Test
    public void changelogHasTheCurrentVersion() {
        assertTrue("CHANGELOG.md: ## " + VERSION, ResourceFiles.read("CHANGELOG.md").contains("\n## " + VERSION + "\n"));
    }

    @Test
    public void specListHasTheCurrentVersion() {
        assertTrue("doc/spec/README.md: [" + VERSION + "](SPEC_v" + VERSION + ".md)",
                ResourceFiles.read("doc/spec/README.md").contains("[" + VERSION + "](SPEC_v" + VERSION + ".md)"));
    }

    /** 更新のお知らせ（ゲーム内・配布サイトの更新内容）。 */
    @Test
    public void newsExistsInBothLanguages() {
        String key = "weakspot.news." + VERSION;
        assertTrue("en_us.lang: " + key, ResourceFiles.lang(ResourceFiles.EN_US).containsKey(key));
        assertTrue("ja_jp.lang: " + key, ResourceFiles.lang(ResourceFiles.JA_JP).containsKey(key));
    }

    /** 新しい版の通知（Forge の更新確認）。 */
    @Test
    public void updateJsonPointsToTheCurrentVersion() {
        String json = ResourceFiles.read("update.json");
        assertEquals("update.json 1.12.2-latest", VERSION, find(json, "\"1\\.12\\.2-latest\"\\s*:\\s*\"([^\"]+)\""));
        assertEquals("update.json 1.12.2-recommended", VERSION,
                find(json, "\"1\\.12\\.2-recommended\"\\s*:\\s*\"([^\"]+)\""));
        assertTrue("update.json \"1.12.2\" has \"" + VERSION + "\"",
                Pattern.compile("\"" + Pattern.quote(VERSION) + "\"\\s*:").matcher(json).find());
    }

    private static String find(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        if (!m.find()) {
            fail("not found: " + regex);
        }
        return m.group(1);
    }
}
