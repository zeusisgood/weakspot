package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.server.TargetRounds;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 「弱点の的」（1.11.0）。手に持って右クリックすると的当てを始める。使い捨て（始めた時点で 1 つ使う）、スタックは 64。
 * ラウンドはサーバーが数える（server/TargetRounds）。レシピは assets/weakspot/recipes/target.json（棒・羊毛・レッドストーン）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class ItemTarget extends Item {

    public static final ItemTarget INSTANCE = new ItemTarget();

    private ItemTarget() {
        setRegistryName(WeakSpotMod.MODID, "target");
        setUnlocalizedName(WeakSpotMod.MODID + ".target");
        setMaxStackSize(64);
        setCreativeTab(CreativeTabs.MISC);
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(INSTANCE);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote && player instanceof EntityPlayerMP && TargetRounds.start((EntityPlayerMP) player)
                && !player.capabilities.isCreativeMode) {
            // 使い捨て（始めた時点で 1 つ使う。クリエイティブでは減らさない）
            stack.shrink(1);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
}
