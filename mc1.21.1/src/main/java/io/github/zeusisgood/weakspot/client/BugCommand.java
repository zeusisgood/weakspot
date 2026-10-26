package io.github.zeusisgood.weakspot.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.BugReport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.internal.versions.neoforge.NeoForgeVersion;
import net.neoforged.neoforgespi.language.IModInfo;

/**
 * /weakspot bug を自分のクライアントで受け取る（サーバーの /weakspot は OP 権限が要り、ソロでチートがオフだと打てない）。
 * クライアントには bug・skipupdate だけを登録する。ほかの /weakspot は、クライアントのコマンドに当たらないので
 * NeoForge がそのままサーバーへ送る（補完もサーバーのものと合わさる）。
 */
final class BugCommand {

    private static final String NEW_ISSUE_URL = "https://github.com/zeusisgood/weakspot/issues/new";
    private static final Set<String> BASE_MODS = new HashSet<>(Arrays.asList("minecraft", "neoforge"));

    private BugCommand() {
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("weakspot")
                .then(Commands.literal("bug").executes(ctx -> {
                    showGuide(Minecraft.getInstance());
                    return 1;
                }).then(Commands.literal("open").executes(ctx -> {
                    openIssue(Minecraft.getInstance());
                    return 1;
                })))
                // チャットのクリックで動くもの
                .then(Commands.literal("skipupdate")
                        .then(Commands.argument("version", StringArgumentType.greedyString()).executes(ctx -> {
                            UpdateCheckNotice.skip(Minecraft.getInstance(),
                                    StringArgumentType.getString(ctx, "version"));
                            return 1;
                        }))));
    }

    private static void showGuide(Minecraft mc) {
        if (mc.player == null) {
            return;
        }
        List<String> environment = environment(mc);
        List<String> mods = mods();
        String modsSummary = I18n.get("weakspot.bug.template.mods", mods.size());
        mc.keyboardHandler.setClipboard(BugReport.plain(environment, modsSummary, mods));

        send(mc, Component.translatable("weakspot.bug.title").withStyle(ChatFormatting.GOLD));
        // OPEN_URL の確認画面は URL を全文表示し、長いとボタンが画面の外に出るので、自分で開く
        send(mc, Component.translatable("weakspot.bug.step1",
                UpdateNotes.link(Component.translatable("weakspot.bug.openIssue"),
                        new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/weakspot bug open")))
                .withStyle(ChatFormatting.WHITE));
        send(mc, Component.translatable("weakspot.bug.step2").withStyle(ChatFormatting.WHITE));
        send(mc, Component.translatable("weakspot.bug.step3", shortEnvironment(mc, mods.size()))
                .withStyle(ChatFormatting.WHITE));
        send(mc, Component.translatable("weakspot.bug.step4").withStyle(ChatFormatting.WHITE));
        send(mc, Component.translatable("weakspot.bug.step5").withStyle(ChatFormatting.GRAY));
    }

    private static void openIssue(Minecraft mc) {
        if (mc.player == null) {
            return;
        }
        // バニラの確認画面が「はい」のあとに使うもの（開けなくてもログに書くだけで、知る方法がない）
        Util.getPlatform().openUri(issueUrl(mc));
    }

    /** URL をコマンドの引数で渡すとチャットの長さの上限に当たるので、開くときに作り直す。 */
    private static String issueUrl(Minecraft mc) {
        List<String> environment = environment(mc);
        List<String> mods = mods();
        String modsSummary = I18n.get("weakspot.bug.template.mods", mods.size());
        // 一般的な不具合報告のテンプレートと同じ見出し
        BugReport.Template template = new BugReport.Template(Arrays.asList(
                new BugReport.Section(I18n.get("weakspot.bug.template.summary")),
                new BugReport.Section(I18n.get("weakspot.bug.template.steps"), "1. "),
                new BugReport.Section(I18n.get("weakspot.bug.template.actual")),
                new BugReport.Section(I18n.get("weakspot.bug.template.expected"))),
                I18n.get("weakspot.bug.template.environment"),
                Collections.singletonList(new BugReport.Section(I18n.get("weakspot.bug.template.additional"))));
        return BugReport.issueUrlFitting(NEW_ISSUE_URL, "[" + WeakSpotMod.VERSION + "] ", template, environment,
                modsSummary, mods, I18n.get("weakspot.bug.template.paste"));
    }

    private static List<String> environment(Minecraft mc) {
        List<String> lines = new ArrayList<>();
        lines.add(I18n.get("weakspot.bug.env.mod", WeakSpotMod.VERSION, serverVersion(), playMode(mc)));
        lines.add("Minecraft " + minecraftVersion() + " / NeoForge " + NeoForgeVersion.getVersion());
        String javaVersion = System.getProperty("java.version");
        String osName = System.getProperty("os.name");
        // 古い Java は Windows 10・11 を 8.1 と答えるので、そのときは注記を付ける
        String osNote = BugReport.osNameMayBeWrong(osName, javaVersion) ? I18n.get("weakspot.bug.env.osNote") : "";
        lines.add("Java " + javaVersion + " (" + System.getProperty("java.vendor") + ") / "
                + osName + " " + System.getProperty("os.version") + " ("
                + System.getProperty("os.arch") + ")" + osNote);
        lines.add(I18n.get("weakspot.bug.env.language", mc.options.languageCode));
        return lines;
    }

    private static String minecraftVersion() {
        return SharedConstants.getCurrentVersion().getName();
    }

    private static String shortEnvironment(Minecraft mc, int modCount) {
        return I18n.get("weakspot.bug.env.short", WeakSpotMod.VERSION, serverVersion(), playMode(mc),
                minecraftVersion(), "NeoForge " + NeoForgeVersion.getVersion(), modCount);
    }

    private static String serverVersion() {
        String version = ClientSettings.isUsingServerValues() ? ClientSettings.get().serverVersion : null;
        return version == null || version.isEmpty() ? I18n.get("weakspot.bug.env.unknown") : version;
    }

    private static String playMode(Minecraft mc) {
        return I18n.get(mc.isSingleplayer() ? "weakspot.bug.env.solo" : "weakspot.bug.env.multi");
    }

    private static List<String> mods() {
        List<String> mods = new ArrayList<>();
        for (IModInfo mod : ModList.get().getMods()) {
            if (!BASE_MODS.contains(mod.getModId())) {
                mods.add(mod.getDisplayName() + " (" + mod.getModId() + ") " + mod.getVersion());
            }
        }
        mods.sort(String.CASE_INSENSITIVE_ORDER);
        return mods;
    }

    private static void send(Minecraft mc, MutableComponent text) {
        mc.player.displayClientMessage(text, false);
    }
}
