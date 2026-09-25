package com.example.weakspot.server;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.ModVersions;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import org.apache.logging.log4j.LogManager;

/**
 * サーバーとクライアントの Mod の版が違うときに、本人・OP 権限を持つほかのプレイヤー・サーバーのログに知らせる（1.5.1）。
 * クライアントの版は、接続のときに Forge が受け取った Mod の一覧（NetworkDispatcher#getModList）から読む（通信は増やさない）。
 * ログインのメッセージに埋もれないように、また言語の設定が届いてから文章を作るために、DELAY_TICKS 後に出す。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class VersionCheck {

    static final int DELAY_TICKS = 40;

    /** 知らせを待っているプレイヤーと、残りの tick。 */
    private static final Map<UUID, Integer> PENDING = new HashMap<>();

    private VersionCheck() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            PENDING.put(event.player.getUniqueID(), DELAY_TICKS);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.player.getUniqueID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) {
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        for (Iterator<Map.Entry<UUID, Integer>> it = PENDING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Integer> entry = it.next();
            if (entry.getValue() > 1) {
                entry.setValue(entry.getValue() - 1);
                continue;
            }
            it.remove();
            EntityPlayerMP player = server == null ? null : server.getPlayerList().getPlayerByUUID(entry.getKey());
            if (player != null) {
                check(server, player);
            }
        }
    }

    private static void check(MinecraftServer server, EntityPlayerMP player) {
        String clientVersion = clientVersion(player);
        String serverVersion = WeakSpotMod.VERSION;
        if (clientVersion == null) {
            return;
        }
        int diff = ModVersions.compare(clientVersion, serverVersion);
        if (diff == 0) {
            return;
        }
        String name = player.getName();
        LogManager.getLogger(WeakSpotMod.MODID).warn("Player {} joined with weakspot {} (server {})",
                name, clientVersion, serverVersion);
        if (diff < 0) {
            send(player, ServerLang.format(player, "weakspot.version.clientOlder", clientVersion, serverVersion));
        } else {
            send(player, ServerLang.format(player, "weakspot.version.serverOlder", serverVersion, clientVersion));
        }
        for (EntityPlayerMP other : server.getPlayerList().getPlayers()) {
            if (other != player && server.getPlayerList().canSendCommands(other.getGameProfile())) {
                send(other, ServerLang.format(other, "weakspot.version.op", name, clientVersion, serverVersion));
            }
        }
    }

    /** 接続のときに受け取ったクライアントの Mod の一覧の weakspot の版。読めなければ null。 */
    private static String clientVersion(EntityPlayerMP player) {
        if (player.connection == null) {
            return null;
        }
        NetworkDispatcher dispatcher = NetworkDispatcher.get(player.connection.getNetworkManager());
        Map<String, String> mods = dispatcher == null ? null : dispatcher.getModList();
        return mods == null ? null : mods.get(WeakSpotMod.MODID);
    }

    private static void send(EntityPlayerMP player, String text) {
        TextComponentString message = new TextComponentString(text);
        message.getStyle().setColor(TextFormatting.YELLOW);
        player.sendMessage(message);
    }
}
