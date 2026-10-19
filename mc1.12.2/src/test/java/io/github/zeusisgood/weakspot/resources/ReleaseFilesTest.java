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

    private static final String VERSION = find(ResourceFiles.read(ResourceFiles.MC + "build.gradle"), "(?m)^version = '([^']+)'");
    private static final String MOD_SOURCE =
            ResourceFiles.read(ResourceFiles.MC + "src/main/java/io/github/zeusisgood/weakspot/WeakSpotMod.java");

    @Test
    public void modVersionMatchesBuildGradle() {
        assertEquals("WeakSpotMod.VERSION", VERSION, find(MOD_SOURCE, "String VERSION = \"([^\"]+)\""));
    }

    /** いつも両方の jar を同じ番号で出す。 */
    @Test
    public void everyMinecraftVersionHasTheSameModVersion() {
        assertEquals("mc1.21.1/gradle.properties mod_version", VERSION,
                find(ResourceFiles.read("mc1.21.1/gradle.properties"), "(?m)^mod_version=(\\S+)"));
        assertEquals("mc1.21.1 WeakSpotMod.VERSION", VERSION, find(ResourceFiles.read(
                "mc1.21.1/src/main/java/io/github/zeusisgood/weakspot/WeakSpotMod.java"), "String VERSION = \"([^\"]+)\""));
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

    /** 1.21.1 は、同じマイナーの印（Network.PROTOCOL）同士だけが接続できる。 */
    @Test
    public void networkProtocolIsTheCurrentMinor() {
        String[] parts = VERSION.split("\\.");
        assertEquals("mc1.21.1 Network.PROTOCOL", parts[0] + "." + parts[1], find(ResourceFiles.read(
                "mc1.21.1/src/main/java/io/github/zeusisgood/weakspot/network/Network.java"),
                "String PROTOCOL = \"([^\"]+)\""));
    }

    /** README のダウンロードは配布サイトへのリンクだけ（版ごとに直さない）。「最近の更新」は版ごとに足す。 */
    @Test
    public void readmeHasTheCurrentVersionInRecentUpdates() {
        assertTrue("README.md: 最近の更新 ### " + VERSION,
                ResourceFiles.read("README.md").contains("\n### " + VERSION + "\n"));
    }

    /** 英語の README の「Recent updates」は、英語のお知らせ（weakspot.news.<版>）の 1 行をそのまま使う。 */
    @Test
    public void englishReadmeHasTheCurrentNewsInRecentUpdates() {
        String news = ResourceFiles.lang(ResourceFiles.EN_US).get("weakspot.news." + VERSION);
        assertTrue("README.en.md: Recent updates ### " + VERSION + " / - " + news,
                ResourceFiles.read("README.en.md").contains("\n### " + VERSION + "\n\n- " + news + "\n"));
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
        assertTrue("en_us.json: " + key, ResourceFiles.lang(ResourceFiles.EN_US).containsKey(key));
        assertTrue("ja_jp.json: " + key, ResourceFiles.lang(ResourceFiles.JA_JP).containsKey(key));
    }

    /** 新しい版の通知（Forge・NeoForge の更新確認）。Minecraft の版ごとの節の両方。 */
    @Test
    public void updateJsonPointsToTheCurrentVersion() {
        String json = ResourceFiles.read("update.json");
        for (String mc : new String[] {"1.12.2", "1.21.1"}) {
            String quoted = Pattern.quote(mc);
            assertEquals("update.json " + mc + "-latest", VERSION,
                    find(json, "\"" + quoted + "-latest\"\\s*:\\s*\"([^\"]+)\""));
            assertEquals("update.json " + mc + "-recommended", VERSION,
                    find(json, "\"" + quoted + "-recommended\"\\s*:\\s*\"([^\"]+)\""));
            String section = find(json, "\"" + quoted + "\"\\s*:\\s*\\{([^}]*)\\}");
            assertTrue("update.json \"" + mc + "\" has \"" + VERSION + "\"",
                    Pattern.compile("\"" + Pattern.quote(VERSION) + "\"\\s*:").matcher(section).find());
        }
    }

    private static String find(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        if (!m.find()) {
            fail("not found: " + regex);
        }
        return m.group(1);
    }
}
