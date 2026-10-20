package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.network.SettingsMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class SettingsSync {

    private SettingsSync() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WeakSpotMod.network.sendTo(new SettingsMessage(SyncedSettings.server()), player);
        }
    }

    /** シングルプレイ・LAN のホストが設定画面で変えたとき。どのスレッドから呼んでもよい。この側でサーバーが動いていなければ何もしない。 */
    public static void resendToAll() {
        SyncedSettings.invalidateServer();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            SettingsMessage message = new SettingsMessage(SyncedSettings.server());
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                WeakSpotMod.network.sendTo(message, player);
            }
        });
    }
}
