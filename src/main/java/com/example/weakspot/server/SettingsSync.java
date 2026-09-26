package com.example.weakspot.server;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.network.SettingsMessage;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/** サーバーの設定値を、ログインしたプレイヤーと、設定が変わったときは接続中の全員へ送る。 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class SettingsSync {

    private SettingsSync() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            WeakSpotMod.network.sendTo(new SettingsMessage(SyncedSettings.server()), (EntityPlayerMP) event.player);
        }
    }

    /**
     * 設定が変わったとき（シングルプレイ・LAN のホストが Mods メニューで変えたとき）に呼ぶ。
     * どのスレッドから呼んでもよい。この側でサーバーが動いていなければ何もしない。
     */
    public static void resendToAll() {
        SyncedSettings.invalidateServer();
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return;
        }
        server.addScheduledTask(() -> WeakSpotMod.network.sendToAll(new SettingsMessage(SyncedSettings.server())));
    }
}
