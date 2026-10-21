package io.github.zeusisgood.weakspot.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * 1.12.2 の IMessage と同じ形（fromBytes / toBytes）のパケット。種類は Network.register で登録する。
 * handle はメインのスレッドで呼ばれる（Network が enqueueWork で回す）。
 */
public abstract class Message {

    public abstract void fromBytes(ByteBuf buf);

    public abstract void toBytes(ByteBuf buf);

    abstract void handle(NetworkEvent.Context ctx);
}
