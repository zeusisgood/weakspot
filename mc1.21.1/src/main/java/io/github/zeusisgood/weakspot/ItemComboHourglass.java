package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.common.ItemRules;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 使うたびに、コンボの猶予の残りに足す（上限なし）。 */
public final class ItemComboHourglass extends Item {

    ItemComboHourglass() {
        super(new Item.Properties().stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            MobEffectInstance current = player.getEffect(Registration.COMBO_GRACE);
            int remaining = current == null ? 0 : current.getDuration();
            // 粒子は照準のまわりの弱点のじゃまになるので出さない（アイコンは出る）
            player.addEffect(new MobEffectInstance(Registration.COMBO_GRACE, ItemRules.hourglassDuration(remaining),
                    0, false, false, true));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL,
                    SoundSource.PLAYERS, 0.6F, 1.4F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("weakspot.item.combo_hourglass.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
