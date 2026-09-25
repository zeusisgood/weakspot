package com.example.weakspot.client;

import com.example.weakspot.GuideBook;
import com.example.weakspot.WeakSpotMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.util.EnumActionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 古い形のガイドの本（1.8.5 より前。番号の翻訳キーで、今の lang にはない）を開こうとしたら、代わりに新しい本を開く。
 * 持ち物の本は、サーバー（1.8.5 以降）が差し替える（server/GuideBookGiver）。古いサーバーでも、読むのはいつも新しい本。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
public final class GuideBookOpener {

    private GuideBookOpener() {
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getWorld().isRemote || !GuideBook.isOutdated(event.getItemStack())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(new GuiScreenBook(mc.player, GuideBook.create(), false));
    }
}
