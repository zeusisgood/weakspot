package io.github.zeusisgood.weakspot;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** サーバーが受け付けたヒットで、まれに渡す（server/ItemRewards）。 */
public final class ItemShard extends Item {

    ItemShard() {
        setRegistryName(WeakSpotMod.MODID, "shard");
        setUnlocalizedName(WeakSpotMod.MODID + ".shard");
        setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.format("weakspot.item.shard.tooltip"));
    }
}
