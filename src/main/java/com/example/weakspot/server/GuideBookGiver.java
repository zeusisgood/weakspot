package com.example.weakspot.server;

import com.example.weakspot.GuideBook;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.config.WeakSpotConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * 初めてログインしたプレイヤーに、ガイドの本を1冊渡す（1.4.1。論理サーバー）。渡したことは、統計と同じ永続データの
 * guideGiven に記録する（死亡・ディメンション移動で消えない。古い版は、このキーを無視する）。持ち物がいっぱいなら足元に落とす。
 * （1.8.5〜1.8.9 にあった、古い形の本の差し替えは 1.9.0 でやめた。）
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
}
