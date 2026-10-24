package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.Registration;
import io.github.zeusisgood.weakspot.Trinkets;
import io.github.zeusisgood.weakspot.common.ItemRules;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** 受け付けたヒット（HitGate.accept）ごとの、欠片とお守り・片眼鏡の耐久。的当てのヒットはここを通らない。 */
final class ItemRewards {

    private ItemRewards() {
    }

    /** combo はヒット後のサーバーのコンボ数。 */
    static void onHit(ServerPlayer player, int combo) {
        if (ItemRules.dropsShard(WeakSpotConfig.server.items.shardChance, combo, player.getRandom().nextDouble())) {
            giveShard(player);
        }
        wear(player, player.getOffhandItem(), Registration.CHARM.get());
        wear(player, player.getItemBySlot(EquipmentSlot.HEAD), Registration.MONOCLE.get());
    }

    private static void giveShard(ServerPlayer player) {
        ItemStack shard = new ItemStack(Registration.SHARD.get());
        if (!player.getInventory().add(shard)) {
            player.drop(shard, false);
        }
        // 本人にだけ鳴らす
        player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.4F);
        ServerStats.record(player, stats -> stats.shards++);
        WeakSpotAdvancements.onShard(player);
    }

    /** 残り 1 で止める（壊さない）。耐久のエンチャントはバニラの hurtAndBreak が見る。 */
    private static void wear(ServerPlayer player, ItemStack stack, Item item) {
        if (Trinkets.usable(stack, item)) {
            stack.hurtAndBreak(1, player.serverLevel(), player, broken -> {
            });
        }
    }
}
