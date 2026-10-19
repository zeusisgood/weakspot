package io.github.zeusisgood.weakspot.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 1.12.2 の IMessage と同じ形（fromBytes / toBytes）のパケット。種類は Network.register で登録する。
 * ハンドラーはメインのスレッドで呼ばれる（NeoForge の初期値）。
 */
public abstract class Message implements CustomPacketPayload {

    public abstract void fromBytes(ByteBuf buf);

    public abstract void toBytes(ByteBuf buf);

    abstract void handle(IPayloadContext ctx);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return Network.typeOf(getClass());
    }
}
