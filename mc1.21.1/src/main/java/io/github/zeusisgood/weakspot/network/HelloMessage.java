package io.github.zeusisgood.weakspot.network;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.server.PlayerText;
import io.netty.buffer.ByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * クライアント → サーバー: 接続直後に自分の Mod の版を送る。1.12.2 は Forge が受け取った Mod の一覧から読めたが、
 * NeoForge では読めないため（サーバーが PlayerText.clientSince・VersionCheck で使う）。
 */
public class HelloMessage extends Message {

    private String version;

    public HelloMessage() {
        this(WeakSpotMod.VERSION);
    }

    public HelloMessage(String version) {
        this.version = version;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        version = Bytes.readString(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        Bytes.writeString(buf, version);
    }

    @Override
    void handle(IPayloadContext ctx) {
        String v = version;
        ServerThread.run(ctx, player -> PlayerText.onHello(player, v));
    }
}
