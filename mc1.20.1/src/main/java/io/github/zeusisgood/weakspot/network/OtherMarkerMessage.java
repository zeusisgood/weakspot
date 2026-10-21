package io.github.zeusisgood.weakspot.network;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.network.MarkerMessage.MarkerData;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * サーバー → クライアント。
 * ハンドラーは専用サーバーでもインスタンス化されるので、クライアントのクラスは proxy 経由で触る。
 */
public class OtherMarkerMessage extends Message {

    private int playerEntityId;
    private MarkerData marker;

    public OtherMarkerMessage() {
    }

    /** marker が null なら「消えた」。 */
    public OtherMarkerMessage(int playerEntityId, MarkerData marker) {
        this.playerEntityId = playerEntityId;
        this.marker = marker;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        playerEntityId = buf.readInt();
        marker = MarkerData.read(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(playerEntityId);
        MarkerData.write(buf, marker);
    }

    @Override
    void handle(NetworkEvent.Context ctx) {
        WeakSpotMod.proxy.onOtherMarker(playerEntityId, marker);
    }
}
