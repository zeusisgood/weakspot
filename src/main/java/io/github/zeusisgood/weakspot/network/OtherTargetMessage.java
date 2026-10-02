package io.github.zeusisgood.weakspot.network;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * サーバー → クライアント: 近くのほかのプレイヤーの的当て（1.11.0。エンティティ ID と、ラウンド中のヒット数。負なら終わった）。
 * ハンドラーは専用サーバーでもインスタンス化されるので、クライアントのクラスは proxy 経由で触る。
 */
public class OtherTargetMessage implements IMessage {

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

    public static class Handler implements IMessageHandler<OtherTargetMessage, IMessage> {

        @Override
        public IMessage onMessage(OtherTargetMessage message, MessageContext ctx) {
            WeakSpotMod.proxy.onOtherTarget(message.entityId, message.hits);
            return null;
        }
    }
}
