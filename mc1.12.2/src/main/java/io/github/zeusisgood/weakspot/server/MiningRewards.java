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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

public final class MiningRewards {

    /** 次の破壊へ持ち越す分。メモリにだけ持ち、再ログインで 0 に戻る。 */
    private static final Map<UUID, Integer> REPAIR_CARRY = new HashMap<>();

    private MiningRewards() {
    }

    /** 耐久回復は壊したときの onBlockBroken。節目は累計の採掘ヒット数で数える（統計をリセットすると、もう一度受け取れる）。 */
    static void onMiningHit(EntityPlayerMP player) {
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

    static void onKindHit(EntityPlayerMP player, HitKind kind) {
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

    private static void checkTotal(EntityPlayerMP player, MiningStats total) {
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
    private static void broadcast(EntityPlayerMP player, String type, HitKind kind, long milestone) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        String key = "weakspot.milestone.broadcast." + type;
        TextFormatting color = Milestones.isLucky(milestone) ? TextFormatting.LIGHT_PURPLE : TextFormatting.GOLD;
        for (EntityPlayerMP other : server.getPlayerList().getPlayers()) {
            if (other == player) {
                continue;
            }
            ITextComponent text = kind == null
                    ? new TextComponentTranslation(key, player.getName(), milestone)
                    : new TextComponentTranslation(key, player.getName(),
                            new TextComponentTranslation("weakspot.kind." + kind.key()), milestone);
            text.getStyle().setColor(color);
            other.sendMessage(text);
        }
    }

    /** 最大コンボを超えたときだけ。受け取った側は showOthersMilestones で消せる。 */
    static void broadcastCombo(EntityPlayerMP player, int combo) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        TextFormatting color = chatColor(ComboTier.of(combo));
        for (EntityPlayerMP other : server.getPlayerList().getPlayers()) {
            if (other == player) {
                continue;
            }
            ITextComponent text = new TextComponentTranslation("weakspot.milestone.broadcast.combo",
                    player.getName(), combo);
            text.getStyle().setColor(color);
            other.sendMessage(text);
        }
    }

    /** 1.12 のチャットは 16 色だけ。 */
    static TextFormatting chatColor(ComboTier tier) {
        switch (tier) {
            case YELLOW:
                return TextFormatting.YELLOW;
            case ORANGE:
            case GOLD:
                return TextFormatting.GOLD;
            case RED:
                return TextFormatting.RED;
            case PINK:
            case RAINBOW:
                return TextFormatting.LIGHT_PURPLE;
            case PURPLE:
                return TextFormatting.DARK_PURPLE;
            case BLUE:
                return TextFormatting.BLUE;
            case AQUA:
                return TextFormatting.AQUA;
            case GREEN:
                return TextFormatting.GREEN;
            default:
                return TextFormatting.WHITE;
        }
    }

    private static void giveXp(EntityPlayerMP player, int xp) {
        if (xp > 0) {
            player.addExperience(xp);
        }
    }

    /** ツールの耐久が減る前に呼ばれる。 */
    static void onBlockBroken(EntityPlayerMP player, int confirmedHits) {
        ItemStack stack = player.getHeldItemMainhand();
        int damage = !stack.isEmpty() && stack.isItemStackDamageable() ? stack.getItemDamage() : 0;
        RepairSettlement result = RepairSettlement.settle(REPAIR_CARRY.getOrDefault(player.getUniqueID(), 0),
                confirmedHits, WeakSpotConfig.server.repair.hitsPerRepair, WeakSpotConfig.server.repair.repairPerStep,
                WeakSpotConfig.server.repair.maxRepairPerBreak, damage);
        REPAIR_CARRY.put(player.getUniqueID(), result.carry);
        repairHeldTool(player, result.repair);
    }

    /** 耐久のないものや、減っていないものは何もしない。 */
    static void repairHeldTool(EntityPlayerMP player, int amount) {
        ItemStack stack = player.getHeldItemMainhand();
        if (amount <= 0 || stack.isEmpty() || !stack.isItemStackDamageable() || !stack.isItemDamaged()) {
            return;
        }
        stack.setItemDamage(Milestones.repairedDamage(stack.getItemDamage(), amount));
    }

    static void forgetOnLogout(EntityPlayer player) {
        REPAIR_CARRY.remove(player.getUniqueID());
    }
}
