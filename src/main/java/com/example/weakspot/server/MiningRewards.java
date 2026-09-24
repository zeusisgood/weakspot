package com.example.weakspot.server;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.Milestones;
import com.example.weakspot.common.RepairSettlement;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.network.MilestoneMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/** 採掘ヒットの報酬（耐久回復と節目）。成長・機械のヒットは対象外。 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class MiningRewards {

    /** 耐久回復の数え方の余り（次の破壊へ持ち越す分）。メモリにだけ持ち、再ログインで0に戻る。 */
    private static final Map<UUID, Integer> REPAIR_CARRY = new HashMap<>();

    private MiningRewards() {
    }

    /** サーバーが採掘ヒットを受け付けたときに呼ぶ。節目だけを判定する（耐久回復は壊したときの onBlockBroken）。 */
    static void onMiningHit(EntityPlayerMP player) {
        long hits = ServerStats.addRewardHit(player);
        for (int index : Milestones.reached(WeakSpotConfig.milestones, hits)) {
            int xp = Milestones.amountAt(WeakSpotConfig.milestoneXp, index);
            if (xp > 0) {
                player.addExperience(xp);
            }
            repairHeldTool(player, Milestones.amountAt(WeakSpotConfig.milestoneRepair, index));
            WeakSpotMod.network.sendTo(new MilestoneMessage(WeakSpotConfig.milestones[index]), player);
        }
    }

    /** 弱点が出るブロックを壊したとき（ツールの耐久が減る前）に、そのブロックで確定したヒットで耐久回復を精算する。 */
    static void onBlockBroken(EntityPlayerMP player, int confirmedHits) {
        ItemStack stack = player.getHeldItemMainhand();
        int damage = !stack.isEmpty() && stack.isItemStackDamageable() ? stack.getItemDamage() : 0;
        RepairSettlement result = RepairSettlement.settle(REPAIR_CARRY.getOrDefault(player.getUniqueID(), 0),
                confirmedHits, WeakSpotConfig.hitsPerRepair, WeakSpotConfig.repairPerStep,
                WeakSpotConfig.maxRepairPerBreak, damage);
        REPAIR_CARRY.put(player.getUniqueID(), result.carry);
        repairHeldTool(player, result.repair);
    }

    /** メインハンドのツールの耐久を回復する。耐久のないものや、減っていないものは何もしない。 */
    private static void repairHeldTool(EntityPlayerMP player, int amount) {
        ItemStack stack = player.getHeldItemMainhand();
        if (amount <= 0 || stack.isEmpty() || !stack.isItemStackDamageable() || !stack.isItemDamaged()) {
            return;
        }
        stack.setItemDamage(Milestones.repairedDamage(stack.getItemDamage(), amount));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        REPAIR_CARRY.remove(event.player.getUniqueID());
    }
}
