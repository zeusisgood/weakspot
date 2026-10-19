package io.github.zeusisgood.weakspot.network;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.netty.buffer.ByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * サーバー → クライアント。0 なら途切れた。
 * ハンドラーは専用サーバーでもインスタンス化されるので、クライアントのクラスは proxy 経由で触る。
 */
public class OtherComboMessage extends Message {

    private int entityId;
    private int count;

    public OtherComboMessage() {
    }

    public OtherComboMessage(int entityId, int count) {
        this.entityId = entityId;
        this.count = count;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
        count = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(count);
    }

    @Override
    void handle(IPayloadContext ctx) {
        WeakSpotMod.proxy.onOtherCombo(entityId, count);
    }
}
