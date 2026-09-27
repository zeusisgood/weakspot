package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.ModVersions;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;

/**
 * サーバーとクライアントの Mod の版が違うときに、本人・OP 権限を持つほかのプレイヤー・サーバーのログに知らせる（1.5.1）。
 * クライアントの版は、接続のときに Forge が受け取った Mod の一覧（NetworkDispatcher#getModList）から読む（通信は増やさない）。
 * ログインのメッセージに埋もれないように、DELAY_TICKS 後に出す。文は 1.9.6 から翻訳キーで送り、プレイヤーの言語で表示する
 * （PlayerText）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class VersionCheck {

    static final int DELAY_TICKS = 40;
    /** 知らせの翻訳キーを足した版。 */
    private static final String KEYS_SINCE = "1.5.1";

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

    /** ログアウトの後片付け（HitGate から呼ぶ。1.8.9）。 */
    static void forgetOnLogout(EntityPlayer player) {
        PENDING.remove(player.getUniqueID());
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
        String clientVersion = PlayerText.clientVersion(player);
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
            send(player, PlayerText.of(player, KEYS_SINCE,
                    "weakspot.version.clientOlder", clientVersion, serverVersion));
        } else {
            send(player, PlayerText.of(player, KEYS_SINCE,
                    "weakspot.version.serverOlder", serverVersion, clientVersion));
        }
        for (EntityPlayerMP other : server.getPlayerList().getPlayers()) {
            if (other != player && server.getPlayerList().canSendCommands(other.getGameProfile())) {
                send(other, PlayerText.of(other, KEYS_SINCE,
                    "weakspot.version.op", name, clientVersion, serverVersion));
            }
        }
    }

    private static void send(EntityPlayerMP player, ITextComponent message) {
        message.getStyle().setColor(TextFormatting.YELLOW);
        player.sendMessage(message);
    }
}
