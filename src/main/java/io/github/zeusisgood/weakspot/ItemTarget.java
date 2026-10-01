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
 * 「弱点の的」（1.11.0）。手に持って右クリックすると的当てを始める（もう一度でやめる）。耐久なし、スタックは 1。
 * ラウンドはサーバーが数える（server/TargetRounds）。レシピは assets/weakspot/recipes/target.json（棒・羊毛・レッドストーン）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class ItemTarget extends Item {

    public static final ItemTarget INSTANCE = new ItemTarget();

    private ItemTarget() {
        setRegistryName(WeakSpotMod.MODID, "target");
        setUnlocalizedName(WeakSpotMod.MODID + ".target");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.MISC);
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(INSTANCE);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (!world.isRemote && player instanceof EntityPlayerMP) {
            TargetRounds.toggle((EntityPlayerMP) player, hand);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }
}
