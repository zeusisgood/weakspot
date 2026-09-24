package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.config.SyncedSettings;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * クライアントが今使う [サーバー] の設定値。接続中はサーバーから届いた値、それ以外は自分の weakspot.cfg の値。
 * [クライアント] の設定（音と見た目）は WeakSpotConfig をそのまま読む。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
public final class ClientSettings {

    /** ネットワークのスレッドから切断時に書き換えるので volatile。 */
    private static volatile SyncedSettings fromServer;

    private ClientSettings() {
    }

    static SyncedSettings get() {
        SyncedSettings s = fromServer;
        return s != null ? s : SyncedSettings.fromConfig();
    }

    /** サーバーから値が届いた（クライアントのスレッドで呼ぶ）。 */
    static void receive(SyncedSettings settings) {
        fromServer = settings;
    }

    /** サーバーの値で動いているか（Mods メニューの注意書きに使う）。 */
    static boolean isUsingServerValues() {
        return fromServer != null;
    }

    @SubscribeEvent
    public static void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        fromServer = null;
    }

    @SubscribeEvent
    public static void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        fromServer = null;
    }
}
