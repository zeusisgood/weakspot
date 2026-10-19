package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.server.TargetRounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** ラウンドはサーバーが数える（TargetRounds）。 */
public final class ItemTarget extends Item {

    ItemTarget() {
        super(new Item.Properties().stacksTo(64));
    }

    public static ItemTarget get() {
        return Registration.TARGET.get();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && TargetRounds.start(serverPlayer)
                && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.success(stack);
    }
}
