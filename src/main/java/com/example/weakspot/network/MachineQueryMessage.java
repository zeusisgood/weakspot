package com.example.weakspot.network;

import com.example.weakspot.server.MachineStates;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * クライアント → サーバー: 機械（かまど・醸造台・スポナー）の進み具合の問い合わせ（1.6.0）。
 * 機械の弱点に照準を合わせている間、数 tick ごとと、ヒットのたびに送る。サーバーは MachineStateMessage で返す。
 */
public class MachineQueryMessage implements IMessage {

    private BlockPos pos;

    public MachineQueryMessage() {
    }

    public MachineQueryMessage(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
    }

    public static class Handler implements IMessageHandler<MachineQueryMessage, IMessage> {

        @Override
        public IMessage onMessage(MachineQueryMessage message, MessageContext ctx) {
            BlockPos pos = message.pos;
            return ServerThread.run(ctx, player -> MachineStates.onQuery(player, pos));
        }
    }
}
