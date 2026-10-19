package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.common.ModVersions;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 相手のクライアントがそのキーを持っていない（キーを足した版 since より古い）ときは、サーバーで文にして送る
 * （ServerLang。持っていないキーはキーのまま表示されてしまうため）。ここを通すのは、パッチで足したキーだけ。
 * 翻訳の値には %s（と %1$s の形）だけを使う（クライアントの翻訳では %d などが使えない）。
 */
public final class PlayerText {

    /** HelloMessage で届いた版。NeoForge は相手の Mod の版を教えてくれないため。 */
    private static final Map<UUID, String> VERSIONS = new HashMap<>();

    private PlayerText() {
    }

    public static void onHello(ServerPlayer player, String version) {
        VERSIONS.put(player.getUUID(), version);
    }

    static void forgetOnLogout(Player player) {
        VERSIONS.remove(player.getUUID());
    }

    static boolean understands(ServerPlayer player, String since) {
        return ModVersions.hasKeysSince(clientVersion(player), since);
    }

    /** since はそのキーを足した版。 */
    static Component of(ServerPlayer player, String since, String key, Object... args) {
        return understands(player, since) ? Component.translatable(key, args)
                : Component.literal(ServerLang.format(player, key, args));
    }

    /** 版が分からない（HelloMessage がまだ届いていない）ときは true。 */
    public static boolean clientSince(ServerPlayer player, String version) {
        String client = clientVersion(player);
        return client == null || client.isEmpty() || ModVersions.compare(client, version) >= 0;
    }

    /** まだ届いていなければ null。 */
    static String clientVersion(ServerPlayer player) {
        return VERSIONS.get(player.getUUID());
    }
}
