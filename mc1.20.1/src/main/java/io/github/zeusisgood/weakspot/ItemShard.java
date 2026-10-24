package io.github.zeusisgood.weakspot;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** サーバーが受け付けたヒットで、まれに渡す（server/ItemRewards）。 */
public final class ItemShard extends Item {

    ItemShard() {
        super(new Item.Properties());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("weakspot.item.shard.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
