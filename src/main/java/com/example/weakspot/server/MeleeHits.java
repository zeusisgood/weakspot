package com.example.weakspot.server;

import com.example.weakspot.MeleeCharge;
import com.example.weakspot.MeleeTargets;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.ComboFactor;
import com.example.weakspot.common.RepairSettlement;
import com.example.weakspot.config.WeakSpotConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Mod;

/**
 * 近接の弱点（論理サーバー。1.8.0 で溜めの形に置き換えた）。剣か斧を持ち、近くに敵がいる間に、照準の左右の弱点に
 * 当てたヒットを受け付け、溜める（MeleeCharge。meleeChargePerHit × コンボの掛け数、上限 meleeChargeMax）。
 * 溜めた攻撃が生き物に当たったら（MeleeCharge.onCriticalHit）、耐久回復を数える。照準の角度は確かめない。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class MeleeHits {

    /** クライアントは 16 ブロック以内の敵で出す。サーバーは位置のずれを見込んで少し甘くする。 */
    private static final double ENEMY_RANGE = 20;

    /** 耐久回復の数え方の余り（溜めた攻撃の回数）。メモリにだけ持ち、再ログインで 0 に戻る。 */
    private static final Map<UUID, Integer> REPAIR_CARRY = new HashMap<>();

    private MeleeHits() {
    }

    /** クライアントからのヒット通知（サーバースレッド）。 */
    public static void onHit(EntityPlayerMP player, int streak) {
        if (!HitGate.allowed(player, HitKind.MELEE)
                || !MeleeCharge.isHoldingWeapon(player) || !MeleeTargets.hasEnemyNear(player, ENEMY_RANGE)) {
            return;
        }
        if (!HitGate.ready(player, HitKind.MELEE)) {
            return;
        }
        int combo = HitGate.accept(player, HitKind.MELEE, new BlockPos(player), streak);
        MeleeCharge.add(player, WeakSpotConfig.meleeChargePerHit * ComboFactor.factor(combo),
                WeakSpotConfig.meleeChargeMax);
    }

    /**
     * 溜めた攻撃が生き物に当たった（MeleeCharge から）。critsPerRepair 回ごとに、メインハンドの物の耐久を
     * critRepairPerStep 回復する（採掘の耐久回復と同じ精算）。このあと、バニラが攻撃の分の耐久を減らす。
     */
    public static void onChargedAttack(EntityPlayerMP player) {
        ItemStack stack = player.getHeldItemMainhand();
        int damage = !stack.isEmpty() && stack.isItemStackDamageable() ? stack.getItemDamage() : 0;
        RepairSettlement result = RepairSettlement.settle(REPAIR_CARRY.getOrDefault(player.getUniqueID(), 0), 1,
                WeakSpotConfig.critsPerRepair, WeakSpotConfig.critRepairPerStep, WeakSpotConfig.critRepairPerStep,
                damage);
        REPAIR_CARRY.put(player.getUniqueID(), result.carry);
        MiningRewards.repairHeldTool(player, result.repair);
    }


    /** ログアウトの後片付け（HitGate から呼ぶ）。 */
    static void forget(EntityPlayer player, HitGate.Leave leave) {
        if (leave == HitGate.Leave.LOGOUT) {
            REPAIR_CARRY.remove(player.getUniqueID());
            MeleeCharge.clear(player);
        } else if (leave == HitGate.Leave.DIMENSION) {
            MeleeCharge.clear(player);
        }
    }
}
