package com.example.weakspot.server;

import com.example.weakspot.GuideBook;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.config.WeakSpotConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.NonNullList;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * 初めてログインしたプレイヤーに、ガイドの本を1冊渡す（1.4.1。論理サーバー）。渡したことは、統計と同じ永続データの
 * guideGiven に記録する（死亡・ディメンション移動で消えない。古い版は、このキーを無視する）。持ち物がいっぱいなら足元に落とす。
 * 1.8.5 から、古い形の本（GuideBook#isOutdated）を、ログインしたときの持ち物と、右クリックで開こうとしたときに差し替える。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class GuideBookGiver {

    private static final String TAG_GIVEN = "guideGiven";

    private GuideBookGiver() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        if (player.world.isRemote) {
            return;
        }
        replaceOutdated(player.inventory.mainInventory);
        replaceOutdated(player.inventory.armorInventory);
        replaceOutdated(player.inventory.offHandInventory);
        if (!WeakSpotConfig.giveGuideBook) {
            return;
        }
        NBTTagCompound data = ServerStats.data(player);
        if (data.getBoolean(TAG_GIVEN)) {
            return;
        }
        data.setBoolean(TAG_GIVEN, true);
        ItemStack book = GuideBook.create();
        if (!player.inventory.addItemStackToInventory(book)) {
            player.dropItem(book, false);
        }
    }

    /** 古い本を右クリックしたら、差し替えて開かない（開くのはクライアントが新しい本で行う。client/GuideBookOpener）。 */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getWorld().isRemote || !GuideBook.isOutdated(event.getItemStack())) {
            return;
        }
        event.getEntityPlayer().setHeldItem(event.getHand(), GuideBook.replacement(event.getItemStack()));
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
    }

    private static void replaceOutdated(NonNullList<ItemStack> stacks) {
        for (int i = 0; i < stacks.size(); i++) {
            if (GuideBook.isOutdated(stacks.get(i))) {
                stacks.set(i, GuideBook.replacement(stacks.get(i)));
            }
        }
    }
}
