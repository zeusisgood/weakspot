package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * 接続中はサーバーから届いた値、それ以外は自分の設定ファイル（weakspot-server.toml）の値。
 * [クライアント] の設定（音と見た目）は WeakSpotConfig をそのまま読む。
 */
@EventBusSubscriber(modid = WeakSpotMod.MODID, value = Dist.CLIENT)
public final class ClientSettings {

    /** 描画のスレッドとネットワークのスレッドから読むので volatile。 */
    private static volatile SyncedSettings fromServer;

    private ClientSettings() {
    }

    static SyncedSettings get() {
        SyncedSettings s = fromServer;
        return s != null ? s : SyncedSettings.fromConfig();
    }

    /** クライアントのスレッドで呼ぶ。 */
    static void receive(SyncedSettings settings) {
        fromServer = settings;
    }

    /** Mods メニューの注意書きに使う。 */
    static boolean isUsingServerValues() {
        return fromServer != null;
    }

    @SubscribeEvent
    public static void onConnect(ClientPlayerNetworkEvent.LoggingIn event) {
        fromServer = null;
    }

    @SubscribeEvent
    public static void onDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        fromServer = null;
    }
}
