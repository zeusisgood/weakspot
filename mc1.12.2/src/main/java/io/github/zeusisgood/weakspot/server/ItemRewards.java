package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.Trinkets;
import io.github.zeusisgood.weakspot.WeakSpotItems;
import io.github.zeusisgood.weakspot.common.ItemRules;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.util.SoundCategory;

/** 受け付けたヒット（HitGate.accept）ごとの、欠片とお守り・片眼鏡の耐久。的当てのヒットはここを通らない。 */
final class ItemRewards {

    private ItemRewards() {
    }

    /** combo はヒット後のサーバーのコンボ数。 */
    static void onHit(EntityPlayerMP player, int combo) {
        if (ItemRules.dropsShard(WeakSpotConfig.server.items.shardChance, combo, player.getRNG().nextDouble())) {
            giveShard(player);
        }
        wear(player, player.getHeldItemOffhand(), WeakSpotItems.CHARM);
        wear(player, player.getItemStackFromSlot(EntityEquipmentSlot.HEAD), WeakSpotItems.MONOCLE);
    }

    private static void giveShard(EntityPlayerMP player) {
        ItemStack shard = new ItemStack(WeakSpotItems.SHARD);
        if (!player.inventory.addItemStackToInventory(shard)) {
            player.dropItem(shard, false);
        }
        // 本人にだけ鳴らす
        player.connection.sendPacket(new SPacketSoundEffect(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.PLAYERS, player.posX, player.posY, player.posZ, 0.4F, 1.6F));
        ServerStats.record(player, stats -> stats.shards++);
        WeakSpotAdvancements.onShard(player);
    }

    /** 残り 1 で止める（壊さない）。耐久のエンチャントはバニラの attemptDamageItem が見る。 */
    private static void wear(EntityPlayerMP player, ItemStack stack, Item item) {
        if (Trinkets.usable(stack, item)) {
            stack.attemptDamageItem(1, player.getRNG(), player);
        }
    }
}
