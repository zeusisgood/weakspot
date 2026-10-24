package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.common.ItemRules;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** オフハンドに持っている間、自分の弱点が大きくなる（Trinkets.charm）。右クリックでは何もしない（素手と同じ扱い）。 */
public final class ItemCharm extends Item {

    ItemCharm() {
        setRegistryName(WeakSpotMod.MODID, "charm");
        setUnlocalizedName(WeakSpotMod.MODID + ".charm");
        setMaxStackSize(1);
        setMaxDamage(ItemRules.TRINKET_DURABILITY);
        setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return repair.getItem() == WeakSpotItems.SHARD;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        Trinkets.addTooltip(stack, "weakspot.item.charm.tooltip", tooltip);
    }
}
