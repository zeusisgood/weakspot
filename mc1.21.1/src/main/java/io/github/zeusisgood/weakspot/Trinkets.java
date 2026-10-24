package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.common.ItemRules;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 付けているアイテムの効果（両側）。クライアントは自分の大きさ・予告・猶予に、サーバーは猶予と耐久に使う。
 * 付けているかは通信で送らず、それぞれの側が持ち物と効果を見る（持ち物と効果はバニラが同期する）。
 */
public final class Trinkets {

    private Trinkets() {
    }

    public static boolean charm(Player player) {
        return usable(player.getOffhandItem(), Registration.CHARM.get());
    }

    public static boolean monocle(Player player) {
        return usable(player.getItemBySlot(EquipmentSlot.HEAD), Registration.MONOCLE.get());
    }

    public static int comboResetTicks(Player player) {
        return ItemRules.comboResetTicks(player.hasEffect(Registration.COMBO_GRACE));
    }

    /** 自分の弱点の大きさに掛ける。 */
    public static double sizeFactor(Player player) {
        return charm(player) ? ItemRules.CHARM_SIZE : 1;
    }

    public static boolean usable(ItemStack stack, Item item) {
        return stack.is(item) && ItemRules.usable(stack.getDamageValue(), stack.getMaxDamage());
    }

    static void addTooltip(ItemStack stack, String key, List<Component> tooltip) {
        tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        if (!ItemRules.usable(stack.getDamageValue(), stack.getMaxDamage())) {
            tooltip.add(Component.translatable("weakspot.item.worn").withStyle(ChatFormatting.RED));
        }
    }
}
