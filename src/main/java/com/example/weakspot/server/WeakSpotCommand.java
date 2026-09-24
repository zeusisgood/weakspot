package com.example.weakspot.server;

import com.example.weakspot.common.MiningStats;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * 管理コマンド /weakspot（OP 以上）。オンラインのプレイヤーの統計を表示する・累計を消す。
 * 表示は翻訳キーで出す（クライアントにも Mod が入っている前提で、クライアントが翻訳する）。
 * 「累計を消す」は統計画面の「累計をリセット」と同じ処理（ServerStats.resetTotal）。
 */
public final class WeakSpotCommand extends CommandBase {

    private static final List<String> SUBCOMMANDS = java.util.Arrays.asList("stats", "reset");

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
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sender.sendMessage(new TextComponentTranslation("weakspot.command.usage"));
            return;
        }
        String sub = args[0];
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
            sender.sendMessage(new TextComponentTranslation("weakspot.command.reset", player.getName()));
        }
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
                stats.growthHits, stats.machineHits, stats.maxStreak));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                                          @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUBCOMMANDS);
        }
        if (args.length == 2 && SUBCOMMANDS.contains(args[0])) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 1;
    }
}
