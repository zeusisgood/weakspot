package com.example.weakspot.network;

import com.example.weakspot.server.ServerBoostTracker;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** クライアント → サーバー: 「このブロックの弱点にヒットした」という通知。 */
public class HitMessage implements IMessage {

    private BlockPos pos;

    public HitMessage() {
    }

    public HitMessage(BlockPos pos) {
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

    public static class Handler implements IMessageHandler<HitMessage, IMessage> {

        @Override
        public IMessage onMessage(HitMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            BlockPos pos = message.pos;
            player.getServerWorld().addScheduledTask(() -> ServerBoostTracker.onHit(player, pos));
            return null;
        }
    }
}
