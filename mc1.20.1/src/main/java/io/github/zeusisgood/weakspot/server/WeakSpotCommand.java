package io.github.zeusisgood.weakspot.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.MiningStats;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * 管理コマンド /weakspot（OP 以上。top だけはだれでも使える）。表示は翻訳キーで出す（クライアントにも Mod が入っている前提）。
 * 「統計を消す」は統計画面の「統計をリセット」と同じ処理（今回と累計の両方）。
 */
public final class WeakSpotCommand {

    /** OP。 */
    private static final int ADMIN_LEVEL = 2;
    private static final int TOP_SHOWN = 10;
    private static final SimpleCommandExceptionType TOP_USAGE =
            new SimpleCommandExceptionType(Component.translatable("weakspot.command.usage.top"));
    private static final SimpleCommandExceptionType RELOAD_FAILED =
            new SimpleCommandExceptionType(Component.translatable("weakspot.command.reload.failed"));

    private WeakSpotCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("weakspot")
                .executes(ctx -> {
                    ctx.getSource().sendSystemMessage(Component.translatable("weakspot.command.usage"));
                    return 1;
                })
                .then(Commands.literal("top")
                        .executes(ctx -> showTop(ctx.getSource(), Leaderboard.Category.MINING))
                        .then(Commands.argument("category", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(categoryKeys(), builder))
                                .executes(ctx -> showTop(ctx.getSource(),
                                        category(StringArgumentType.getString(ctx, "category"))))))
                .then(Commands.literal("stats").requires(source -> source.hasPermission(ADMIN_LEVEL))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> showStats(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("reset").requires(source -> source.hasPermission(ADMIN_LEVEL))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> reset(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("reload").requires(source -> source.hasPermission(ADMIN_LEVEL))
                        .executes(ctx -> reload(ctx.getSource()))));
    }

    private static List<String> categoryKeys() {
        return java.util.Arrays.stream(Leaderboard.Category.values()).map(Leaderboard.Category::key).toList();
    }

    private static Leaderboard.Category category(String key) throws CommandSyntaxException {
        Leaderboard.Category category = Leaderboard.Category.byKey(key.toLowerCase(Locale.ROOT));
        if (category == null) {
            throw TOP_USAGE.create();
        }
        return category;
    }

    /** 自分が 10 位より下なら、最後に自分の順位。 */
    private static int showTop(CommandSourceStack source, Leaderboard.Category category) {
        List<Leaderboard.Entry> ranking = Leaderboard.ranking(source.getServer(), category);
        source.sendSystemMessage(Component.translatable("weakspot.command.top.header",
                Component.translatable("weakspot.command.top." + category.key())).withStyle(ChatFormatting.GOLD));
        if (ranking.isEmpty()) {
            source.sendSystemMessage(Component.translatable("weakspot.command.top.empty"));
            return 0;
        }
        Entity self = source.getEntity();
        for (int i = 0; i < ranking.size(); i++) {
            Leaderboard.Entry entry = ranking.get(i);
            boolean mine = self != null && entry.id.equals(self.getUUID());
            if (i < TOP_SHOWN || mine) {
                source.sendSystemMessage(Component.translatable("weakspot.command.top.line", i + 1, entry.name,
                        entry.value(category)).withStyle(i == 0 ? ChatFormatting.YELLOW
                        : mine ? ChatFormatting.AQUA : ChatFormatting.WHITE));
            }
        }
        return ranking.size();
    }

    private static int reset(CommandSourceStack source, ServerPlayer player) {
        ServerStats.resetTotal(player);
        Leaderboard.update(player);
        source.sendSuccess(() -> Component.translatable("weakspot.command.reset", player.getName()), true);
        return 1;
    }

    /** 設定画面で変えたときと同じく、全員に設定を送り直す。 */
    private static int reload(CommandSourceStack source) throws CommandSyntaxException {
        if (!WeakSpotConfig.reloadFromFile()) {
            throw RELOAD_FAILED.create();
        }
        SettingsSync.resendToAll();
        WeakSpotConfig.warnIfMisconfigured();
        source.sendSuccess(() -> Component.translatable("weakspot.command.reload"), true);
        return 1;
    }

    private static int showStats(CommandSourceStack source, ServerPlayer player) {
        source.sendSystemMessage(Component.translatable("weakspot.command.stats.header", player.getName()));
        line(source, "weakspot.command.stats.session", ServerStats.session(player));
        line(source, "weakspot.command.stats.total", ServerStats.total(player));
        return 1;
    }

    private static void line(CommandSourceStack source, String key, MiningStats stats) {
        double average = stats.averageHitsPerBlock();
        source.sendSystemMessage(Component.translatable(key,
                stats.hits, stats.blocksBroken, stats.blocksBrokenWithHit,
                Double.isNaN(average) ? "-" : String.format("%.2f", average),
                stats.maxHitsOnBlock, String.format("%.1f", stats.savedSeconds()),
                stats.count(HitKind.GROWTH), stats.count(HitKind.MACHINE), stats.maxStreak,
                stats.count(HitKind.ANIMAL), stats.count(HitKind.FISHING), stats.count(HitKind.BOW),
                stats.count(HitKind.MELEE), stats.count(HitKind.VEHICLE), stats.count(HitKind.EAT),
                stats.count(HitKind.SLEEP), stats.count(HitKind.LADDER), stats.count(HitKind.ELYTRA),
                stats.count(HitKind.ENCHANT), stats.count(HitKind.HARVEST), stats.count(HitKind.THROW),
                stats.count(HitKind.SPRINT), stats.count(HitKind.PORTAL), stats.count(HitKind.SWIM),
                stats.count(HitKind.FALL), stats.totalHits()));
    }
}
