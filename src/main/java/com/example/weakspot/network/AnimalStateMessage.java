package com.example.weakspot.network;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.AnimalTimers;
import com.example.weakspot.server.AnimalHits;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * サーバー → クライアント: 動物の状態（動いているタイマーの種類と、それぞれの進み具合 0.0〜1.0）。
 * mask が 0 なら、弱点が出る条件を満たしていない。
 * ハンドラーは専用サーバーでもインスタンス化されるので、クライアントのクラスは proxy 経由で触る。
 */
public class AnimalStateMessage implements IMessage {

    private int entityId;
    private int mask;
    private final float[] progress = new float[AnimalTimers.Timer.values().length];

    public AnimalStateMessage() {
    }

    public AnimalStateMessage(int entityId, AnimalHits.State state) {
        this.entityId = entityId;
        this.mask = state.mask;
        System.arraycopy(state.progress, 0, progress, 0, progress.length);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
        mask = buf.readUnsignedByte();
        for (int i = 0; i < progress.length; i++) {
            progress[i] = buf.readFloat();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeByte(mask);
        for (float value : progress) {
            buf.writeFloat(value);
        }
    }

    public static class Handler implements IMessageHandler<AnimalStateMessage, IMessage> {

        @Override
        public IMessage onMessage(AnimalStateMessage message, MessageContext ctx) {
            WeakSpotMod.proxy.onAnimalState(message.entityId, message.mask, message.progress);
            return null;
        }
    }
}
