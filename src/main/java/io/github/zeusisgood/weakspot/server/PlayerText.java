package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.ModVersions;
import java.util.Map;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;

/**
 * 相手のクライアントがそのキーを持っていない（キーを足した版 since より古い）ときは、サーバーで文にして送る
 * （ServerLang。持っていないキーはキーのまま表示されてしまうため）。ここを通すのは、パッチで足したキーだけ。
 * 翻訳の値には %s（と %1$s の形）だけを使う（クライアントの翻訳では %d などが使えない）。
 */
public final class PlayerText {

    private PlayerText() {
    }

    static boolean understands(EntityPlayerMP player, String since) {
        return ModVersions.hasKeysSince(clientVersion(player), since);
    }

    /** since はそのキーを足した版。 */
    static ITextComponent of(EntityPlayerMP player, String since, String key, Object... args) {
        return understands(player, since) ? new TextComponentTranslation(key, args)
                : new TextComponentString(ServerLang.format(player, key, args));
    }

    /** 版が分からない（同じ jar のシングルプレイなど。古い版のクライアントは必ず版を送る）ときは true。 */
    public static boolean clientSince(EntityPlayerMP player, String version) {
        String client = clientVersion(player);
        return client == null || client.isEmpty() || ModVersions.compare(client, version) >= 0;
    }

    /** 接続のときに Forge が受け取った Mod の一覧から読む。読めなければ null。 */
    static String clientVersion(EntityPlayerMP player) {
        if (player.connection == null) {
            return null;
        }
        NetworkDispatcher dispatcher = NetworkDispatcher.get(player.connection.getNetworkManager());
        Map<String, String> mods = dispatcher == null ? null : dispatcher.getModList();
        return mods == null ? null : mods.get(WeakSpotMod.MODID);
    }
}
