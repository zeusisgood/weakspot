package io.github.zeusisgood.weakspot.common;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

/**
 * GitHub の新しい issue の画面は、リンクの title と body で最初から書き込める。
 * Mod の一覧はリンクに入れず、クリップボードから貼ってもらう（リンクの長さを Mod の数に依らず一定にするため。
 * GitHub にログインしていないと、戻り先としてもう一度符号化されて伸びる）。
 */
public final class BugReport {

    private BugReport() {
    }

    /** text が空なら空欄。 */
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

    /** pasteNote は Mod の一覧の場所に書く。 */
    public static String body(Template template, List<String> environment, String pasteNote) {
        StringBuilder sb = new StringBuilder();
        for (Section section : template.before) {
            appendSection(sb, section);
        }
        sb.append("## ").append(template.environmentHeading).append("\n");
        for (String line : environment) {
            sb.append("- ").append(line).append("\n");
        }
        sb.append("\n").append(pasteNote).append("\n");
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
    * Minecraft のランチャーが同梱する古い Java 8（8u51 など）は Windows 10・11 を知らず、os.name を「Windows 8.1」と答える
    * （Windows 11 を正しく答えるのは 8u321 から）。
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

    /** クリップボード用（見出しなし）。 */
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

    public static String issueUrl(String newIssueUrl, String title, String body) {
        return newIssueUrl + "?title=" + encode(title) + "&body=" + encode(body);
    }

    /** 空白は %20。 */
    public static String encode(String text) {
        try {
            return URLEncoder.encode(text, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
