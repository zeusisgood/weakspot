package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.common.ItemRules;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 頭に付けている間、自分の弱点が次に移る位置を見せる（Trinkets.monocle）。防具（ItemArmor）にしないのは、
 * バニラが傷を受けるたびに防具の耐久を減らし、壊してしまうため。頭の上の見た目は、モデルの head の表示で出す。
 */
public final class ItemMonocle extends Item {

    ItemMonocle() {
        setRegistryName(WeakSpotMod.MODID, "monocle");
        setUnlocalizedName(WeakSpotMod.MODID + ".monocle");
        setMaxStackSize(1);
        setMaxDamage(ItemRules.TRINKET_DURABILITY);
        setCreativeTab(CreativeTabs.MISC);
    }

    @Nullable
    @Override
    public EntityEquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EntityEquipmentSlot.HEAD;
    }

    /** 防具と同じく、右クリックで頭が空いていれば付ける。 */
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!player.getItemStackFromSlot(EntityEquipmentSlot.HEAD).isEmpty()) {
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        player.setItemStackToSlot(EntityEquipmentSlot.HEAD, stack.copy());
        player.playSound(SoundEvents.ITEM_ARMOR_EQUIP_GOLD, 1.0F, 1.0F);
        stack.setCount(0);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return repair.getItem() == WeakSpotItems.SHARD;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        Trinkets.addTooltip(stack, "weakspot.item.monocle.tooltip", tooltip);
    }
}
