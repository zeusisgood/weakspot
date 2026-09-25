package com.example.weakspot.network;

import com.example.weakspot.WeakSpotMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * サーバー → クライアント: 機械の進み具合（0〜1）と燃料（0〜1。燃料がない機械は負）（1.6.0）。
 * ハンドラーは専用サーバーでもインスタンス化されるので、クライアントのクラスは proxy 経由で触る。
 */
public class MachineStateMessage implements IMessage {

    private BlockPos pos;
    private float progress;
    private float fuel;

    public MachineStateMessage() {
    }

    public MachineStateMessage(BlockPos pos, float progress, float fuel) {
        this.pos = pos;
        this.progress = progress;
        this.fuel = fuel;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        progress = buf.readFloat();
        fuel = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeFloat(progress);
        buf.writeFloat(fuel);
    }

    public static class Handler implements IMessageHandler<MachineStateMessage, IMessage> {

        @Override
        public IMessage onMessage(MachineStateMessage message, MessageContext ctx) {
            WeakSpotMod.proxy.onMachineState(message.pos, message.progress, message.fuel);
            return null;
        }
    }
}
