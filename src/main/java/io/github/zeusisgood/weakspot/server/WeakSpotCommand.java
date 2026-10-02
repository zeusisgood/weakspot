package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.MiningStats;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

/**
 * 管理コマンド /weakspot（OP 以上）。オンラインのプレイヤーの統計を表示する・統計を消す・設定を読み直す。
 * /weakspot top（1.11.0）だけは、だれでも使える（サーバー内の上位 10 人。オフラインの人も。Leaderboard）。
 * 表示は翻訳キーで出す（クライアントにも Mod が入っている前提で、クライアントが翻訳する）。
 * 「統計を消す」は統計画面の「統計をリセット」と同じ処理（ServerStats.resetTotal。今回と累計の両方）。
 */
public final class WeakSpotCommand extends CommandBase {

    /** プレイヤーを指定するサブコマンド。 */
    private static final List<String> SUBCOMMANDS = java.util.Arrays.asList("stats", "reset");
    private static final List<String> ALL_SUBCOMMANDS = java.util.Arrays.asList("stats", "reset", "reload", "top");
    /** 管理のサブコマンドに要る権限のレベル（OP）。 */
    private static final int ADMIN_LEVEL = 2;
    /** /weakspot top で出す人数。 */
    private static final int TOP_SHOWN = 10;

    @Override
    public String getName() {
        return "weakspot";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "weakspot.command.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        // top はだれでも使えるように 0 にし、管理のサブコマンドは execute で OP かを確かめる（1.11.0）
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sender.sendMessage(new TextComponentTranslation("weakspot.command.usage"));
            return;
        }
        String sub = args[0];
        if (sub.equals("top")) {
            showTop(server, sender, args);
            return;
        }
        if (!sender.canUseCommand(ADMIN_LEVEL, getName())) {
            throw new CommandException("commands.generic.permission");
        }
        if (sub.equals("reload")) {
            if (args.length != 1) {
                throw new WrongUsageException("weakspot.command.usage.reload");
            }
            reload(sender);
            return;
        }
        if (!SUBCOMMANDS.contains(sub)) {
            throw new WrongUsageException("weakspot.command.usage");
        }
        if (args.length != 2) {
            throw new WrongUsageException("weakspot.command.usage." + sub);
        }
        EntityPlayerMP player = server.getPlayerList().getPlayerByUsername(args[1]);
        if (player == null) {
            throw new CommandException("weakspot.command.offline");
        }
        if (sub.equals("stats")) {
            showStats(sender, player);
        } else {
            ServerStats.resetTotal(player);
            Leaderboard.update(player);
            sender.sendMessage(new TextComponentTranslation("weakspot.command.reset", player.getName()));
        }
    }

    /** /weakspot top [mining|combo|target|total]: その項目の上位 10 人。自分が 10 位より下なら、最後に自分の順位。 */
    private static void showTop(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        Leaderboard.Category category = args.length >= 2 ? Leaderboard.Category.byKey(args[1])
                : Leaderboard.Category.MINING;
        if (category == null || args.length > 2) {
            throw new WrongUsageException("weakspot.command.usage.top");
        }
        List<Leaderboard.Entry> ranking = Leaderboard.ranking(server, category);
        ITextComponent header = new TextComponentTranslation("weakspot.command.top.header",
                new TextComponentTranslation("weakspot.command.top." + category.key()));
        header.getStyle().setColor(TextFormatting.GOLD);
        sender.sendMessage(header);
        if (ranking.isEmpty()) {
            sender.sendMessage(new TextComponentTranslation("weakspot.command.top.empty"));
            return;
        }
        Entity self = sender.getCommandSenderEntity();
        for (int i = 0; i < ranking.size(); i++) {
            Leaderboard.Entry entry = ranking.get(i);
            boolean mine = self != null && entry.id.equals(self.getUniqueID());
            if (i < TOP_SHOWN || mine) {
                ITextComponent line = new TextComponentTranslation("weakspot.command.top.line", i + 1, entry.name,
                        entry.value(category));
                line.getStyle().setColor(i == 0 ? TextFormatting.YELLOW
                        : mine ? TextFormatting.AQUA : TextFormatting.WHITE);
                sender.sendMessage(line);
            }
        }
    }

    /** weakspot.cfg を読み直し、全員に設定を送り直す（設定画面で変えたときと同じ）。 */
    private static void reload(ICommandSender sender) throws CommandException {
        if (!WeakSpotConfig.reloadFromFile()) {
            throw new CommandException("weakspot.command.reload.failed");
        }
        SettingsSync.resendToAll();
        WeakSpotConfig.warnIfMisconfigured();
        sender.sendMessage(new TextComponentTranslation("weakspot.command.reload"));
    }

    private static void showStats(ICommandSender sender, EntityPlayerMP player) {
        sender.sendMessage(new TextComponentTranslation("weakspot.command.stats.header", player.getName()));
        line(sender, "weakspot.command.stats.session", ServerStats.session(player));
        line(sender, "weakspot.command.stats.total", ServerStats.total(player));
    }

    /** 統計画面と同じ項目を1行にまとめる。 */
    private static void line(ICommandSender sender, String key, MiningStats stats) {
        double average = stats.averageHitsPerBlock();
        sender.sendMessage(new TextComponentTranslation(key,
                stats.hits, stats.blocksBroken, stats.blocksBrokenWithHit,
                Double.isNaN(average) ? "-" : String.format("%.2f", average),
                stats.maxHitsOnBlock, String.format("%.1f", stats.savedSeconds()),
                stats.count(HitKind.GROWTH), stats.count(HitKind.MACHINE), stats.maxStreak,
                stats.count(HitKind.ANIMAL), stats.count(HitKind.FISHING), stats.count(HitKind.BOW),
                stats.count(HitKind.MELEE), stats.count(HitKind.VEHICLE), stats.count(HitKind.EAT),
                stats.count(HitKind.SLEEP), stats.count(HitKind.LADDER), stats.count(HitKind.ELYTRA),
                stats.count(HitKind.ENCHANT), stats.count(HitKind.HARVEST), stats.count(HitKind.THROW),
                stats.count(HitKind.SPRINT), stats.count(HitKind.PORTAL), stats.count(HitKind.SWIM), stats.count(HitKind.FALL),
                stats.totalHits()));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                                          @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, ALL_SUBCOMMANDS);
        }
        if (args.length == 2 && args[0].equals("top")) {
            return getListOfStringsMatchingLastWord(args, "mining", "combo", "target", "total");
        }
        if (args.length == 2 && SUBCOMMANDS.contains(args[0])) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 1 && args.length > 0 && SUBCOMMANDS.contains(args[0]);
    }
}
