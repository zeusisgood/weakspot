package com.example.weakspot.network;

import com.example.weakspot.server.ServerBoostTracker;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * クライアント → サーバー: 「このブロックの弱点にヒットした」という通知。
 * streak は連続ヒット数で、周りのプレイヤーのヒット音の音階を合わせるためだけに使う。
 */
public class HitMessage implements IMessage {

    private BlockPos pos;
    private int streak;

    public HitMessage() {
    }

    public HitMessage(BlockPos pos, int streak) {
        this.pos = pos;
        this.streak = streak;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        streak = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeInt(streak);
    }

    public static class Handler implements IMessageHandler<HitMessage, IMessage> {

        @Override
        public IMessage onMessage(HitMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            BlockPos pos = message.pos;
            int streak = message.streak;
            player.getServerWorld().addScheduledTask(() -> ServerBoostTracker.onHit(player, pos, streak));
            return null;
        }
    }
}
