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

    /** 見出しと環境・Mod の一覧（Markdown）。includeMods が false なら、一覧の代わりに pasteNote を書く。 */
    public static String body(List<String> headings, List<String> environment, String modsSummary, List<String> mods,
                              boolean includeMods, String pasteNote) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < headings.size() - 1; i++) {
            sb.append("## ").append(headings.get(i)).append("\n\n\n");
        }
        sb.append("## ").append(headings.get(headings.size() - 1)).append("\n");
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
        return sb.toString();
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
    public static String issueUrlFitting(String newIssueUrl, String title, List<String> headings,
                                         List<String> environment, String modsSummary, List<String> mods,
                                         String pasteNote) {
        String full = issueUrl(newIssueUrl, title, body(headings, environment, modsSummary, mods, true, pasteNote));
        if (full.length() <= MAX_URL_LENGTH) {
            return full;
        }
        return issueUrl(newIssueUrl, title, body(headings, environment, modsSummary, mods, false, pasteNote));
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
