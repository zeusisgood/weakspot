package com.example.weakspot.network;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitKind;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * サーバー → クライアント: QueryMessage の返事（1.9.0 で、動物・釣り・機械の 3 つの返事をまとめた）。
 * 種類と相手と、整数 1 つ（flags）と小数の並び（values）。中身の意味は種類ごと:
 * <ul>
 * <li>動物: flags = 動いているタイマーの mask（0 なら弱点を出さない）、values = タイマーごとの進み具合（0〜1）</li>
 * <li>釣り: flags = 待ち時間の段階なら 1、values = {待ち時間の進み具合}</li>
 * <li>機械: values = {進み具合, 燃料（燃料のない機械は負）}</li>
 * </ul>
 * ハンドラーは専用サーバーでもインスタンス化されるので、クライアントのクラスは proxy 経由で触る。
 */
public class StateMessage implements IMessage {

    private HitKind kind;
    private long target;
    private int flags;
    private float[] values;

    public StateMessage() {
    }

    private StateMessage(HitKind kind, long target, int flags, float... values) {
        this.kind = kind;
        this.target = target;
        this.flags = flags;
        this.values = values.clone();
    }

    public static StateMessage animal(int entityId, int mask, float[] progress) {
        return new StateMessage(HitKind.ANIMAL, entityId, mask, progress);
    }

    public static StateMessage fishing(boolean waiting, float progress) {
        return new StateMessage(HitKind.FISHING, 0, waiting ? 1 : 0, progress);
    }

    public static StateMessage machine(BlockPos pos, float progress, float fuel) {
        return new StateMessage(HitKind.MACHINE, pos.toLong(), 0, progress, fuel);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        kind = HitKind.byId(buf.readUnsignedByte());
        target = buf.readLong();
        flags = buf.readInt();
        values = new float[buf.readUnsignedByte()];
        for (int i = 0; i < values.length; i++) {
            values[i] = buf.readFloat();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(kind.ordinal());
        buf.writeLong(target);
        buf.writeInt(flags);
        buf.writeByte(values.length);
        for (float value : values) {
            buf.writeFloat(value);
        }
    }

    public static class Handler implements IMessageHandler<StateMessage, IMessage> {
        @Override
        public IMessage onMessage(StateMessage message, MessageContext ctx) {
            if (message.kind == null) {
                return null;
            }
            float[] v = message.values;
            switch (message.kind) {
                case ANIMAL:
                    WeakSpotMod.proxy.onAnimalState((int) message.target, message.flags, v);
                    break;
                case FISHING:
                    WeakSpotMod.proxy.onFishingState(message.flags != 0, v.length > 0 ? v[0] : 0);
                    break;
                case MACHINE:
                    if (v.length >= 2) {
                        WeakSpotMod.proxy.onMachineState(BlockPos.fromLong(message.target), v[0], v[1]);
                    }
                    break;
                default:
                    break;
            }
            return null;
        }
    }
}
