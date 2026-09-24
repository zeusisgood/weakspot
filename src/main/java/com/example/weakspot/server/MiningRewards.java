package com.example.weakspot.server;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.Milestones;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.network.MilestoneMessage;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

/** 採掘ヒットの報酬（耐久回復と節目）。成長・機械のヒットは対象外。 */
final class MiningRewards {

    private MiningRewards() {
    }

    /** サーバーが採掘ヒットを受け付けたときに呼ぶ。 */
    static void onMiningHit(EntityPlayerMP player) {
        long hits = ServerStats.addRewardHit(player);
        if (Milestones.isRepairHit(hits, WeakSpotConfig.hitsPerRepair)) {
            repairHeldTool(player, WeakSpotConfig.repairPerStep);
        }
        for (int index : Milestones.reached(WeakSpotConfig.milestones, hits)) {
            int xp = Milestones.amountAt(WeakSpotConfig.milestoneXp, index);
            if (xp > 0) {
                player.addExperience(xp);
            }
            repairHeldTool(player, Milestones.amountAt(WeakSpotConfig.milestoneRepair, index));
            WeakSpotMod.network.sendTo(new MilestoneMessage(WeakSpotConfig.milestones[index]), player);
        }
    }

    /** メインハンドのツールの耐久を回復する。耐久のないものや、減っていないものは何もしない。 */
    private static void repairHeldTool(EntityPlayerMP player, int amount) {
        ItemStack stack = player.getHeldItemMainhand();
        if (amount <= 0 || stack.isEmpty() || !stack.isItemStackDamageable() || !stack.isItemDamaged()) {
            return;
        }
        stack.setItemDamage(Milestones.repairedDamage(stack.getItemDamage(), amount));
    }
}
