package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.WeakSpotMod;
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
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Mod;

/** 採掘ヒットの報酬（耐久回復と節目）と、1.7.0 からの種類ごと・合計の節目。耐久回復は採掘だけ。 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class MiningRewards {

    /** 耐久回復の数え方の余り（次の破壊へ持ち越す分）。メモリにだけ持ち、再ログインで0に戻る。 */
    private static final Map<UUID, Integer> REPAIR_CARRY = new HashMap<>();

    private MiningRewards() {
    }

    /**
     * サーバーが採掘ヒットを受け付けて、統計に記録したあとに呼ぶ。節目だけを判定する（耐久回復は壊したときの onBlockBroken）。
     * 節目は累計の採掘ヒット数で数える（1.6.1。統計をリセットすると、もう一度受け取れる）。1.7.0 から、先は繰り返し、
     * 合計の節目も判定する。
     */
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

    /**
     * 採掘以外の種類のヒットを数えたあとに呼ぶ（1.7.0）。種類ごとの節目（採掘と同じ数字、ごほうびは経験値だけ）と、
     * 合計の節目を判定する。
     */
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

    /** すべての種類のヒット数の合計の節目（1.7.0）。ごほうびは経験値だけ。 */
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

    /** 節目の知らせの翻訳キー weakspot.milestone.broadcast.* を足した版。 */
    static final String BROADCAST_SINCE = "1.10.0";

    /**
     * 本人以外の全員のチャットに、節目に届いたことを知らせる（1.10.0）。type は mining / kind / total。
     * 色は金、7 だけが並ぶ数はピンク。受け取った側は showOthersMilestones で消せる（OthersMilestoneFilter）。
     */
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
            ITextComponent text;
            if (kind == null) {
                text = PlayerText.of(other, BROADCAST_SINCE, key, player.getName(), milestone);
            } else if (PlayerText.understands(other, BROADCAST_SINCE)) {
                text = new TextComponentTranslation(key, player.getName(),
                        new TextComponentTranslation("weakspot.kind." + kind.key()), milestone);
            } else {
                text = new TextComponentString(ServerLang.format(other, key, player.getName(),
                        ServerLang.format(other, "weakspot.kind." + kind.key()), milestone));
            }
            text.getStyle().setColor(color);
            other.sendMessage(text);
        }
    }

    private static void giveXp(EntityPlayerMP player, int xp) {
        if (xp > 0) {
            player.addExperience(xp);
        }
    }

    /** 弱点が出るブロックを壊したとき（ツールの耐久が減る前）に、そのブロックで確定したヒットで耐久回復を精算する。 */
    static void onBlockBroken(EntityPlayerMP player, int confirmedHits) {
        ItemStack stack = player.getHeldItemMainhand();
        int damage = !stack.isEmpty() && stack.isItemStackDamageable() ? stack.getItemDamage() : 0;
        RepairSettlement result = RepairSettlement.settle(REPAIR_CARRY.getOrDefault(player.getUniqueID(), 0),
                confirmedHits, WeakSpotConfig.server.repair.hitsPerRepair, WeakSpotConfig.server.repair.repairPerStep,
                WeakSpotConfig.server.repair.maxRepairPerBreak, damage);
        REPAIR_CARRY.put(player.getUniqueID(), result.carry);
        repairHeldTool(player, result.repair);
    }

    /** メインハンドのツールの耐久を回復する。耐久のないものや、減っていないものは何もしない（近接のクリティカルからも使う）。 */
    static void repairHeldTool(EntityPlayerMP player, int amount) {
        ItemStack stack = player.getHeldItemMainhand();
        if (amount <= 0 || stack.isEmpty() || !stack.isItemStackDamageable() || !stack.isItemDamaged()) {
            return;
        }
        stack.setItemDamage(Milestones.repairedDamage(stack.getItemDamage(), amount));
    }

    /** ログアウトの後片付け（HitGate から呼ぶ。1.8.9）。 */
    static void forgetOnLogout(EntityPlayer player) {
        REPAIR_CARRY.remove(player.getUniqueID());
    }
}
