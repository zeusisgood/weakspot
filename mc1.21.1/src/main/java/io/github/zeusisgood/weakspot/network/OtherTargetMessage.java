package io.github.zeusisgood.weakspot.network;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.netty.buffer.ByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * サーバー → クライアント。ヒット数が負なら終わった。
 * ハンドラーは専用サーバーでもインスタンス化されるので、クライアントのクラスは proxy 経由で触る。
 */
public class OtherTargetMessage extends Message {

    private int entityId;
    private int hits;

    public OtherTargetMessage() {
    }

    public OtherTargetMessage(int entityId, int hits) {
        this.entityId = entityId;
        this.hits = hits;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
        hits = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(hits);
    }

    @Override
    void handle(IPayloadContext ctx) {
        WeakSpotMod.proxy.onOtherTarget(entityId, hits);
    }
}
