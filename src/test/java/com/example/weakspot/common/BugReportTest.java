package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class BugReportTest {

    private static final String BASE = "https://github.com/zeusisgood/weakspot/issues/new";
    private static final BugReport.Template TEMPLATE = new BugReport.Template(
            Arrays.asList(new BugReport.Section("Summary"), new BugReport.Section("Steps", "1. ")),
            "Environment", Collections.singletonList(new BugReport.Section("Additional")));
    private static final List<String> ENV = Arrays.asList("Mod: 1.7.1", "Minecraft 1.12.2 / Forge 14.23.5.2860");

    @Test
    public void encodesSpacesAndJapanese() {
        assertEquals("a%20b%26c", BugReport.encode("a b&c"));
        assertEquals("%E5%BC%B1", BugReport.encode("弱"));
        assertEquals("%0A", BugReport.encode("\n"));
    }

    @Test
    public void bodyHasHeadingsEnvironmentAndMods() {
        String body = BugReport.body(TEMPLATE, ENV, "Mods (2)", Arrays.asList("A (a) 1", "B (b) 2"), true, "paste");
        assertTrue(body.startsWith("## Summary\n\n\n## Steps\n1. \n\n## Environment\n"));
        assertTrue(body.endsWith("</details>\n\n## Additional\n\n\n"));
        assertTrue(body.contains("## Environment\n- Mod: 1.7.1\n"));
        assertTrue(body.contains("<details><summary>Mods (2)</summary>"));
        assertTrue(body.contains("- B (b) 2\n"));
        assertFalse(body.contains("paste"));
    }

    @Test
    public void keepsModsWhenShort() {
        String url = BugReport.issueUrlFitting(BASE, "[1.7.1] ", TEMPLATE, ENV, "Mods (1)",
                Collections.singletonList("A (a) 1"), "paste");
        assertTrue(url.startsWith(BASE + "?title=%5B1.7.1%5D%20&body="));
        assertTrue(url.contains(BugReport.encode("A (a) 1")));
    }

    @Test
    public void dropsModsWhenTooLong() {
        List<String> mods = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            mods.add("Some Long Mod Name " + i + " (somelongmodid" + i + ") 1.2.3");
        }
        String url = BugReport.issueUrlFitting(BASE, "[1.7.1] ", TEMPLATE, ENV, "Mods (400)", mods, "paste");
        assertTrue(url.length() <= BugReport.MAX_URL_LENGTH);
        assertTrue(url.contains("paste"));
        assertFalse(url.contains("somelongmodid"));
    }

    @Test
    public void plainTextForTheClipboard() {
        assertEquals("Mod: 1.7.1\nMinecraft 1.12.2 / Forge 14.23.5.2860\nMods (1)\n- A (a) 1\n",
                BugReport.plain(ENV, "Mods (1)", Collections.singletonList("A (a) 1")));
    }

    @Test
    public void osNameNoteOnlyForWindowsWithOldJava() {
        assertTrue(BugReport.osNameMayBeWrong("Windows 8.1", "1.8.0_51"));
        assertTrue(BugReport.osNameMayBeWrong("Windows 10", "1.8.0_320-b10"));
        assertFalse(BugReport.osNameMayBeWrong("Windows 11", "1.8.0_321"));
        assertFalse(BugReport.osNameMayBeWrong("Mac OS X", "1.8.0_51"));
        assertFalse(BugReport.osNameMayBeWrong("Linux", "1.8.0_51"));
        assertFalse(BugReport.osNameMayBeWrong("Windows 8.1", "17.0.8"));
        assertFalse(BugReport.osNameMayBeWrong("Windows 8.1", "1.8.0_x"));
        assertFalse(BugReport.osNameMayBeWrong(null, null));
    }
}
