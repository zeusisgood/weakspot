package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.BugReport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.network.play.client.CPacketChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

/**
 * クライアントのコマンド /weakspot（1.7.1）。/weakspot bug だけを自分のクライアントで受け取り、不具合の報告の方法を出す
 * （サーバーの /weakspot は OP 権限が要り、ソロでチートがオフだと打てない。古いサーバーにも無い）。
 * 環境を自動で集め、GitHub の issue の画面に最初から書き込むリンクと、クリップボードへのコピーの両方で渡す。
 * bug 以外（/weakspot、stats、reset、reload）は、打った文字をそのままサーバーへ送る（サーバーのコマンドが動く）。
 */
final class BugCommand extends CommandBase {

    private static final String NEW_ISSUE_URL = "https://github.com/zeusisgood/weakspot/issues/new";
    /** Mod の一覧に載せない、土台の Mod。 */
    private static final Set<String> BASE_MODS = new HashSet<>(Arrays.asList("minecraft", "mcp", "FML", "forge"));

    @Override
    public String getName() {
        return "weakspot";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/weakspot bug";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public void execute(@Nullable MinecraftServer server, ICommandSender sender, String[] args) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return;
        }
        if (args.length >= 1 && "bug".equalsIgnoreCase(args[0])) {
            showGuide(mc);
            return;
        }
        // bug 以外は、サーバーのコマンドとして送る（チャットのパケットは、クライアントのコマンドを通らない）
        String line = "/weakspot" + (args.length > 0 ? " " + String.join(" ", args) : "");
        mc.player.connection.sendPacket(new CPacketChatMessage(line));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                                          @Nullable BlockPos targetPos) {
        // サーバーの stats / reset / reload の補完と合わさる
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, "bug") : Collections.emptyList();
    }

    private static void showGuide(Minecraft mc) {
        List<String> environment = environment(mc);
        List<String> mods = mods();
        String modsSummary = I18n.format("weakspot.bug.template.mods", mods.size());
        List<String> headings = Arrays.asList(I18n.format("weakspot.bug.template.did"),
                I18n.format("weakspot.bug.template.happened"), I18n.format("weakspot.bug.template.expected"),
                I18n.format("weakspot.bug.template.environment"));
        String url = BugReport.issueUrlFitting(NEW_ISSUE_URL, "[" + WeakSpotMod.VERSION + "] ", headings, environment,
                modsSummary, mods, I18n.format("weakspot.bug.template.paste"));
        GuiScreen.setClipboardString(BugReport.plain(environment, modsSummary, mods));

        send(mc, gold(new TextComponentTranslation("weakspot.bug.title")));
        TextComponentTranslation open = new TextComponentTranslation("weakspot.bug.step1",
                UpdateNotes.link(new TextComponentTranslation("weakspot.bug.openIssue"),
                        new ClickEvent(ClickEvent.Action.OPEN_URL, url)));
        send(mc, white(open));
        send(mc, white(new TextComponentTranslation("weakspot.bug.step2")));
        send(mc, white(new TextComponentTranslation("weakspot.bug.step3", shortEnvironment(mc, mods.size()))));
        send(mc, white(new TextComponentTranslation("weakspot.bug.step4")));
        TextComponentTranslation note = new TextComponentTranslation("weakspot.bug.step5");
        note.getStyle().setColor(TextFormatting.GRAY);
        send(mc, note);
    }

    /** 本文とクリップボードに入れる環境（1 行ずつ）。 */
    private static List<String> environment(Minecraft mc) {
        List<String> lines = new ArrayList<>();
        lines.add(I18n.format("weakspot.bug.env.mod", WeakSpotMod.VERSION, serverVersion(), playMode(mc)));
        lines.add("Minecraft " + Loader.MC_VERSION + " / Forge " + ForgeVersion.getVersion());
        lines.add("Java " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ") / "
                + System.getProperty("os.name") + " " + System.getProperty("os.version") + " ("
                + System.getProperty("os.arch") + ")");
        lines.add(I18n.format("weakspot.bug.env.language", mc.gameSettings.language));
        return lines;
    }

    /** チャットに出す、短い環境。 */
    private static String shortEnvironment(Minecraft mc, int modCount) {
        return I18n.format("weakspot.bug.env.short", WeakSpotMod.VERSION, serverVersion(), playMode(mc),
                Loader.MC_VERSION, ForgeVersion.getVersion(), modCount);
    }

    private static String serverVersion() {
        String version = ClientSettings.isUsingServerValues() ? ClientSettings.get().serverVersion : null;
        return version == null || version.isEmpty() ? I18n.format("weakspot.bug.env.unknown") : version;
    }

    private static String playMode(Minecraft mc) {
        return I18n.format(mc.isSingleplayer() ? "weakspot.bug.env.solo" : "weakspot.bug.env.multi");
    }

    /** 入れている Mod の一覧（名前 (modid) 版）。土台の Mod は除く。 */
    private static List<String> mods() {
        List<String> mods = new ArrayList<>();
        for (ModContainer mod : Loader.instance().getActiveModList()) {
            if (!BASE_MODS.contains(mod.getModId())) {
                mods.add(mod.getName() + " (" + mod.getModId() + ") " + mod.getVersion());
            }
        }
        mods.sort(String.CASE_INSENSITIVE_ORDER);
        return mods;
    }

    private static TextComponentTranslation gold(TextComponentTranslation text) {
        text.getStyle().setColor(TextFormatting.GOLD);
        return text;
    }

    private static TextComponentTranslation white(TextComponentTranslation text) {
        text.getStyle().setColor(TextFormatting.WHITE);
        return text;
    }

    private static void send(Minecraft mc, ITextComponent text) {
        mc.player.sendMessage(text);
    }
}
