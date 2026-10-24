package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.common.ItemRules;
import java.util.List;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 付けているアイテムの効果（両側）。クライアントは自分の大きさ・予告・猶予に、サーバーは猶予と耐久に使う。
 * 付けているかは通信で送らず、それぞれの側が持ち物と効果を見る（持ち物と効果はバニラが同期する）。
 */
public final class Trinkets {

    private Trinkets() {
    }

    public static boolean charm(EntityPlayer player) {
        return usable(player.getHeldItemOffhand(), WeakSpotItems.CHARM);
    }

    public static boolean monocle(EntityPlayer player) {
        return usable(player.getItemStackFromSlot(EntityEquipmentSlot.HEAD), WeakSpotItems.MONOCLE);
    }

    public static int comboResetTicks(EntityPlayer player) {
        return ItemRules.comboResetTicks(player.isPotionActive(WeakSpotItems.COMBO_GRACE));
    }

    /** 自分の弱点の大きさに掛ける。 */
    public static double sizeFactor(EntityPlayer player) {
        return charm(player) ? ItemRules.CHARM_SIZE : 1;
    }

    public static boolean usable(ItemStack stack, Item item) {
        return stack.getItem() == item && ItemRules.usable(stack.getItemDamage(), stack.getMaxDamage());
    }

    @SideOnly(Side.CLIENT)
    static void addTooltip(ItemStack stack, String key, List<String> tooltip) {
        tooltip.add(I18n.format(key));
        if (!ItemRules.usable(stack.getItemDamage(), stack.getMaxDamage())) {
            tooltip.add(TextFormatting.RED + I18n.format("weakspot.item.worn"));
        }
    }
}
