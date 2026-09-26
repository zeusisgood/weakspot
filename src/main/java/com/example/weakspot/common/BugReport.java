package com.example.weakspot.common;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

/**
 * 不具合の報告（/weakspot bug。1.7.1）の本文とリンクを作る。環境の集め方と文章（翻訳）は呼ぶ側が渡す。
 * GitHub の新しい issue の画面は、リンクの title と body で最初から書き込める。Mod の一覧でリンクが長くなりすぎるときは、
 * 一覧を外して「貼り付けてください」と書く。Minecraft に依存しない。
 */
public final class BugReport {

    /** これより長いリンクは、Mod の一覧を外す（GitHub とブラウザーが長すぎるリンクを受け付けないため）。 */
    public static final int MAX_URL_LENGTH = 6000;

    private BugReport() {
    }

    /** 本文の 1 つの節: 見出しと、最初から入れておく文（空なら空欄）。 */
    public static final class Section {
        final String heading;
        final String text;

        public Section(String heading, String text) {
            this.heading = heading;
            this.text = text;
        }

        public Section(String heading) {
            this(heading, "");
        }
    }

    /** 本文の形: 環境の前の節、環境の見出し、環境の後の節（1.9.1。それまでは環境が最後）。 */
    public static final class Template {
        final List<Section> before;
        final String environmentHeading;
        final List<Section> after;

        public Template(List<Section> before, String environmentHeading, List<Section> after) {
            this.before = before;
            this.environmentHeading = environmentHeading;
            this.after = after;
        }
    }

    /** 見出しと環境・Mod の一覧（Markdown）。includeMods が false なら、一覧の代わりに pasteNote を書く。 */
    public static String body(Template template, List<String> environment, String modsSummary, List<String> mods,
                              boolean includeMods, String pasteNote) {
        StringBuilder sb = new StringBuilder();
        for (Section section : template.before) {
            appendSection(sb, section);
        }
        sb.append("## ").append(template.environmentHeading).append("\n");
        for (String line : environment) {
            sb.append("- ").append(line).append("\n");
        }
        sb.append("\n");
        if (includeMods) {
            sb.append("<details><summary>").append(modsSummary).append("</summary>\n\n");
            for (String mod : mods) {
                sb.append("- ").append(mod).append("\n");
            }
            sb.append("</details>\n");
        } else {
            sb.append(pasteNote).append("\n");
        }
        for (Section section : template.after) {
            sb.append("\n");
            appendSection(sb, section);
        }
        return sb.toString();
    }

    private static void appendSection(StringBuilder sb, Section section) {
        sb.append("## ").append(section.heading).append("\n").append(section.text).append("\n\n");
    }

    /**
     * 環境の OS の行に、「OS 名が不正確かもしれない」注記を付けるか（1.9.1）。Minecraft のランチャーが同梱する古い
     * Java 8（8u51 など）は Windows 10・11 を知らず、os.name を「Windows 8.1」と答える（Windows 11 を正しく答えるのは
     * 8u321 から）。Windows で、java.version が 1.8.0_N（N < 321）のときだけ true。
     */
    public static boolean osNameMayBeWrong(String osName, String javaVersion) {
        if (osName == null || !osName.startsWith("Windows") || javaVersion == null
                || !javaVersion.startsWith("1.8.0_")) {
            return false;
        }
        String update = javaVersion.substring("1.8.0_".length());
        int end = 0;
        while (end < update.length() && Character.isDigit(update.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return false;
        }
        return Integer.parseInt(update.substring(0, end)) < 321;
    }

    /** クリップボードに入れる文章（見出しなしの環境と Mod の一覧）。 */
    public static String plain(List<String> environment, String modsSummary, List<String> mods) {
        StringBuilder sb = new StringBuilder();
        for (String line : environment) {
            sb.append(line).append("\n");
        }
        sb.append(modsSummary).append("\n");
        for (String mod : mods) {
            sb.append("- ").append(mod).append("\n");
        }
        return sb.toString();
    }

    /** issue の画面を、タイトルと本文を入れた状態で開くリンク。 */
    public static String issueUrl(String newIssueUrl, String title, String body) {
        return newIssueUrl + "?title=" + encode(title) + "&body=" + encode(body);
    }

    /**
     * Mod の一覧を入れたリンク。長すぎれば、一覧を外して pasteNote を入れたリンク。
     */
    public static String issueUrlFitting(String newIssueUrl, String title, Template template,
                                         List<String> environment, String modsSummary, List<String> mods,
                                         String pasteNote) {
        String full = issueUrl(newIssueUrl, title, body(template, environment, modsSummary, mods, true, pasteNote));
        if (full.length() <= MAX_URL_LENGTH) {
            return full;
        }
        return issueUrl(newIssueUrl, title, body(template, environment, modsSummary, mods, false, pasteNote));
    }

    /** URL のクエリの値としてエンコードする（空白は %20）。 */
    public static String encode(String text) {
        try {
            return URLEncoder.encode(text, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
