package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.ModVersions;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;

/**
 * クライアントの版は、接続直後にクライアントが送る HelloMessage で知る（PlayerText）。
 * ログインのメッセージに埋もれないように、DELAY_TICKS 後に出す。
 */
@EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class VersionCheck {

    static final int DELAY_TICKS = 40;

    /** 残りの tick。 */
    private static final Map<UUID, Integer> PENDING = new HashMap<>();

    private VersionCheck() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            PENDING.put(event.getEntity().getUUID(), DELAY_TICKS);
        }
    }

    static void forgetOnLogout(Player player) {
        PENDING.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        for (Iterator<Map.Entry<UUID, Integer>> it = PENDING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Integer> entry = it.next();
            if (entry.getValue() > 1) {
                entry.setValue(entry.getValue() - 1);
                continue;
            }
            it.remove();
            ServerPlayer player = server == null ? null : server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                check(server, player);
            }
        }
    }

    private static void check(MinecraftServer server, ServerPlayer player) {
        String clientVersion = PlayerText.clientVersion(player);
        String serverVersion = WeakSpotMod.VERSION;
        if (clientVersion == null) {
            return;
        }
        int diff = ModVersions.compare(clientVersion, serverVersion);
        if (diff == 0) {
            return;
        }
        String name = player.getName().getString();
        LogManager.getLogger(WeakSpotMod.MODID).warn("Player {} joined with weakspot {} (server {})",
                name, clientVersion, serverVersion);
        if (diff < 0) {
            send(player, Component.translatable(
                    "weakspot.version.clientOlder", clientVersion, serverVersion));
        } else {
            send(player, Component.translatable(
                    "weakspot.version.serverOlder", serverVersion, clientVersion));
        }
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other != player && server.getPlayerList().isOp(other.getGameProfile())) {
                send(other, Component.translatable(
                    "weakspot.version.op", name, clientVersion, serverVersion));
            }
        }
    }

    private static void send(ServerPlayer player, Component message) {
        player.sendSystemMessage(message.copy().withStyle(ChatFormatting.YELLOW));
    }
}
