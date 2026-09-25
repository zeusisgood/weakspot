package com.example.weakspot.network;

import com.example.weakspot.WeakSpotMod;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * サーバー → クライアント: 近くのほかのプレイヤーのコンボ（エンティティ ID と数。0 なら途切れた）（1.6.0）。
 * ハンドラーは専用サーバーでもインスタンス化されるので、クライアントのクラスは proxy 経由で触る。
 */
public class OtherComboMessage implements IMessage {

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

    public static class Handler implements IMessageHandler<OtherComboMessage, IMessage> {

        @Override
        public IMessage onMessage(OtherComboMessage message, MessageContext ctx) {
            WeakSpotMod.proxy.onOtherCombo(message.entityId, message.count);
            return null;
        }
    }
}
