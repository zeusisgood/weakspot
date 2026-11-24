package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.ComboTier;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.Milestones;
import io.github.zeusisgood.weakspot.common.MiningStats;
import io.github.zeusisgood.weakspot.common.RepairSettlement;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import io.github.zeusisgood.weakspot.network.MilestoneMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class MiningRewards {

    /** 次の破壊へ持ち越す分。メモリにだけ持ち、再ログインで 0 に戻る。 */
    private static final Map<UUID, Integer> REPAIR_CARRY = new HashMap<>();

    private MiningRewards() {
    }

    /** 耐久回復は壊したときの onBlockBroken。節目は累計の採掘ヒット数で数える（統計をリセットすると、もう一度受け取れる）。 */
    static void onMiningHit(ServerPlayer player) {
        MiningStats total = ServerStats.total(player);
        for (long[] reached : Milestones.reachedWithRepeat(WeakSpotConfig.server.milestones.milestones,
                WeakSpotConfig.server.milestones.milestoneRepeatInterval, total.hits)) {
            int index = (int) reached[1];
            giveXp(player, Milestones.amountAt(WeakSpotConfig.server.milestones.milestoneXp, index));
            repairHeldTool(player, Milestones.amountAt(WeakSpotConfig.server.milestones.milestoneRepair, index));
            WeakSpotMod.network.sendTo(new MilestoneMessage(MilestoneMessage.MINING, -1, reached[0]), player);
            broadcast(player, "mining", null, reached[0]);
        }
        checkTotal(player, total);
    }

    static void onKindHit(ServerPlayer player, HitKind kind) {
        MiningStats total = ServerStats.total(player);
        if (WeakSpotConfig.server.milestones.kindMilestonesEnabled) {
            for (long[] reached : Milestones.reachedWithRepeat(WeakSpotConfig.server.milestones.milestones,
                    WeakSpotConfig.server.milestones.milestoneRepeatInterval, total.count(kind))) {
                giveXp(player, Milestones.amountAt(WeakSpotConfig.server.milestones.milestoneXp, (int) reached[1]));
                WeakSpotMod.network.sendTo(new MilestoneMessage(MilestoneMessage.KIND, kind.ordinal(), reached[0]),
                        player);
                broadcast(player, "kind", kind, reached[0]);
            }
        }
        checkTotal(player, total);
    }

    private static void checkTotal(ServerPlayer player, MiningStats total) {
        if (!WeakSpotConfig.server.milestones.totalMilestonesEnabled) {
            return;
        }
        for (long[] reached : Milestones.reachedWithRepeat(WeakSpotConfig.server.milestones.totalMilestones,
                WeakSpotConfig.server.milestones.totalMilestoneRepeatInterval, total.totalHits())) {
            giveXp(player, Milestones.amountAt(WeakSpotConfig.server.milestones.totalMilestoneXp, (int) reached[1]));
            WeakSpotMod.network.sendTo(new MilestoneMessage(MilestoneMessage.TOTAL, -1, reached[0]), player);
            broadcast(player, "total", null, reached[0]);
        }
    }

    /** 受け取った側は showOthersMilestones で消せる（OthersMilestoneFilter）。 */
    private static void broadcast(ServerPlayer player, String type, HitKind kind, long milestone) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        String key = "weakspot.milestone.broadcast." + type;
        ChatFormatting color = Milestones.isLucky(milestone) ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GOLD;
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == player) {
                continue;
            }
            Component text = kind == null
                    ? Component.translatable(key, player.getName(), milestone)
                    : Component.translatable(key, player.getName(),
                            Component.translatable("weakspot.kind." + kind.key()), milestone);
            other.sendSystemMessage(text.copy().withStyle(color));
        }
    }

    /** 最大コンボを超えたときだけ。受け取った側は showOthersMilestones で消せる。 */
    static void broadcastCombo(ServerPlayer player, int combo) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ChatFormatting color = chatColor(ComboTier.of(combo));
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == player) {
                continue;
            }
            Component text = Component.translatable("weakspot.milestone.broadcast.combo",
                    player.getName(), combo);
            other.sendSystemMessage(text.copy().withStyle(color));
        }
    }

    /** 1.12.2 版と同じ色（1.12 のチャットは 16 色だけ）。 */
    static ChatFormatting chatColor(ComboTier tier) {
        switch (tier) {
            case YELLOW:
                return ChatFormatting.YELLOW;
            case ORANGE:
            case GOLD:
                return ChatFormatting.GOLD;
            case RED:
                return ChatFormatting.RED;
            case PINK:
            case RAINBOW:
                return ChatFormatting.LIGHT_PURPLE;
            case PURPLE:
                return ChatFormatting.DARK_PURPLE;
            case BLUE:
                return ChatFormatting.BLUE;
            case AQUA:
                return ChatFormatting.AQUA;
            case GREEN:
                return ChatFormatting.GREEN;
            default:
                return ChatFormatting.WHITE;
        }
    }

    private static void giveXp(ServerPlayer player, int xp) {
        if (xp > 0) {
            player.giveExperiencePoints(xp);
        }
    }

    /** ツールの耐久が減る前に呼ばれる。 */
    static void onBlockBroken(ServerPlayer player, int confirmedHits) {
        ItemStack stack = player.getMainHandItem();
        int damage = !stack.isEmpty() && stack.isDamageableItem() ? stack.getDamageValue() : 0;
        RepairSettlement result = RepairSettlement.settle(REPAIR_CARRY.getOrDefault(player.getUUID(), 0),
                confirmedHits, WeakSpotConfig.server.repair.hitsPerRepair, WeakSpotConfig.server.repair.repairPerStep,
                WeakSpotConfig.server.repair.maxRepairPerBreak, damage);
        REPAIR_CARRY.put(player.getUUID(), result.carry);
        repairHeldTool(player, result.repair);
    }

    /** 耐久のないものや、減っていないものは何もしない。 */
    static void repairHeldTool(ServerPlayer player, int amount) {
        ItemStack stack = player.getMainHandItem();
        if (amount <= 0 || stack.isEmpty() || !stack.isDamageableItem() || !stack.isDamaged()) {
            return;
        }
        stack.setDamageValue(Milestones.repairedDamage(stack.getDamageValue(), amount));
    }

    static void forgetOnLogout(Player player) {
        REPAIR_CARRY.remove(player.getUUID());
    }
}
