package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.common.ItemRules;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** オフハンドに持っている間、自分の弱点が大きくなる（Trinkets.charm）。右クリックでは何もしない（素手と同じ扱い）。 */
public final class ItemCharm extends Item {

    ItemCharm() {
        super(new Item.Properties().durability(ItemRules.TRINKET_DURABILITY));
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(Registration.SHARD.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        Trinkets.addTooltip(stack, "weakspot.item.charm.tooltip", tooltip);
    }
}
