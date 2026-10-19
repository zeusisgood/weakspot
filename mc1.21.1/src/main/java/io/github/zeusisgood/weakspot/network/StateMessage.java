package io.github.zeusisgood.weakspot.network;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * サーバー → クライアント。flags と values の意味は種類ごと:
 * <ul>
 * <li>動物: flags = 動いているタイマーの mask（0 なら弱点を出さない）、values = タイマーごとの進み具合（0〜1）</li>
 * <li>釣り: flags = 待ち時間の段階なら 1、values = {待ち時間の進み具合}</li>
 * <li>機械: values = {進み具合, 燃料（燃料のない機械は負）}</li>
 * </ul>
 * ハンドラーは専用サーバーでもインスタンス化されるので、クライアントのクラスは proxy 経由で触る。
 */
public class StateMessage extends Message {

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
        return new StateMessage(HitKind.MACHINE, pos.asLong(), 0, progress, fuel);
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

    @Override
    void handle(IPayloadContext ctx) {
        if (kind == null) {
            return;
        }
        float[] v = values;
        switch (kind) {
            case ANIMAL:
                WeakSpotMod.proxy.onAnimalState((int) target, flags, v);
                break;
            case FISHING:
                WeakSpotMod.proxy.onFishingState(flags != 0, v.length > 0 ? v[0] : 0);
                break;
            case MACHINE:
                if (v.length >= 2) {
                    WeakSpotMod.proxy.onMachineState(BlockPos.of(target), v[0], v[1]);
                }
                break;
            default:
                break;
        }
    }
}
