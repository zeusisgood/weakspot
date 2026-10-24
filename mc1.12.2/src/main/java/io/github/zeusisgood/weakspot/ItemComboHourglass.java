package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.common.ItemRules;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 使うたびに、コンボの猶予の残りに足す（上限なし）。 */
public final class ItemComboHourglass extends Item {

    ItemComboHourglass() {
        setRegistryName(WeakSpotMod.MODID, "combo_hourglass");
        setUnlocalizedName(WeakSpotMod.MODID + ".combo_hourglass");
        setMaxStackSize(16);
        setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote) {
            PotionEffect current = player.getActivePotionEffect(WeakSpotItems.COMBO_GRACE);
            int remaining = current == null ? 0 : current.getDuration();
            // 粒子は照準のまわりの弱点のじゃまになるので出さない（アイコンは出る）
            player.addPotionEffect(new PotionEffect(WeakSpotItems.COMBO_GRACE,
                    ItemRules.hourglassDuration(remaining), 0, false, false));
            world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ITEM_BOTTLE_FILL,
                    SoundCategory.PLAYERS, 0.6F, 1.4F);
            if (!player.capabilities.isCreativeMode) {
                stack.shrink(1);
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.format("weakspot.item.combo_hourglass.tooltip"));
    }
}
