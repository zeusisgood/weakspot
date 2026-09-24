package com.example.weakspot.network;

import com.example.weakspot.WeakSpotMod;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * サーバー → クライアント: 自分の浮きが、魚が寄ってくるのを待っている段階か（弱点を出せるか）と、待ち時間の進み具合（0.0〜1.0）。
 * ハンドラーは専用サーバーでもインスタンス化されるので、クライアントのクラスは proxy 経由で触る。
 */
public class FishingStateMessage implements IMessage {

    private boolean waiting;
    private float progress;

    public FishingStateMessage() {
    }

    public FishingStateMessage(boolean waiting, float progress) {
        this.waiting = waiting;
        this.progress = progress;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        waiting = buf.readBoolean();
        progress = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(waiting);
        buf.writeFloat(progress);
    }

    public static class Handler implements IMessageHandler<FishingStateMessage, IMessage> {

        @Override
        public IMessage onMessage(FishingStateMessage message, MessageContext ctx) {
            WeakSpotMod.proxy.onFishingState(message.waiting, message.progress);
            return null;
        }
    }
}
