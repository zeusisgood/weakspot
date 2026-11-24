package io.github.zeusisgood.weakspot.network;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.server.VersionCheck;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * クライアント → サーバー: 接続直後に自分の Mod の版を送る。1.12.2 は Forge が受け取った Mod の一覧から読めたが、
 * Forge 47 では読めないため（サーバーの VersionCheck が使う）。
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
    void handle(NetworkEvent.Context ctx) {
        String v = version;
        ServerThread.run(ctx, player -> VersionCheck.onHello(player, v));
    }
}
