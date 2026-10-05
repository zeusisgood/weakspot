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
 * サーバーからプレイヤーに送る文（1.9.6）。翻訳キーと値（TextComponentTranslation）で送り、クライアントが自分の今の言語で
 * 文にする。ただし相手のクライアントがそのキーを持っていない（キーを足した版 since より古い）ときは、今までどおり
 * サーバーで文にして送る（ServerLang。持っていないキーはキーのまま表示されてしまうため）。
 * 翻訳の値には %s（と %1$s の形）だけを使う（クライアントの翻訳では %d などが使えない）。
 */
public final class PlayerText {

    private PlayerText() {
    }

    /** 相手のクライアントが、since の版で足した翻訳キーを持っているか。 */
    static boolean understands(EntityPlayerMP player, String since) {
        return ModVersions.hasKeysSince(clientVersion(player), since);
    }

    /** key の文。since はそのキーを足した版。 */
    static ITextComponent of(EntityPlayerMP player, String since, String key, Object... args) {
        return understands(player, since) ? new TextComponentTranslation(key, args)
                : new TextComponentString(ServerLang.format(player, key, args));
    }

    /**
     * クライアントの版が version 以上か（1.11.2。サーバーとクライアントの両方が要る機能を、古いクライアントに使わない）。
     * 版が分からない（同じ jar のシングルプレイなど。古い版のクライアントは必ず版を送る）ときは true。
     */
    public static boolean clientSince(EntityPlayerMP player, String version) {
        String client = clientVersion(player);
        return client == null || client.isEmpty() || ModVersions.compare(client, version) >= 0;
    }

    /** 接続のときに Forge が受け取ったクライアントの Mod の一覧の weakspot の版。読めなければ null。 */
    static String clientVersion(EntityPlayerMP player) {
        if (player.connection == null) {
            return null;
        }
        NetworkDispatcher dispatcher = NetworkDispatcher.get(player.connection.getNetworkManager());
        Map<String, String> mods = dispatcher == null ? null : dispatcher.getModList();
        return mods == null ? null : mods.get(WeakSpotMod.MODID);
    }
}
