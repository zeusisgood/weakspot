package com.example.weakspot.network;

import com.example.weakspot.common.HitKind;
import com.example.weakspot.server.RightClickHits;
import com.example.weakspot.server.ServerBoostTracker;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * クライアント → サーバー: 「このブロックの弱点にヒットした」という通知。kind はヒットの種類（採掘 / 成長 / 機械）。
 * streak は連続ヒット数で、周りのプレイヤーのヒット音の音階を合わせるためだけに使う（採掘だけ）。
 */
public class HitMessage implements IMessage {

    private HitKind kind;
    private BlockPos pos;
    private int streak;

    public HitMessage() {
    }

    public HitMessage(HitKind kind, BlockPos pos, int streak) {
        this.kind = kind;
        this.pos = pos;
        this.streak = streak;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        kind = HitKind.byId(buf.readByte());
        pos = BlockPos.fromLong(buf.readLong());
        streak = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(kind.ordinal());
        buf.writeLong(pos.toLong());
        buf.writeInt(streak);
    }

    public static class Handler implements IMessageHandler<HitMessage, IMessage> {

        @Override
        public IMessage onMessage(HitMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            HitKind kind = message.kind;
            BlockPos pos = message.pos;
            int streak = message.streak;
            if (kind == null) {
                return null;
            }
            player.getServerWorld().addScheduledTask(() -> {
                if (kind == HitKind.MINING) {
                    ServerBoostTracker.onHit(player, pos, streak);
                } else {
                    RightClickHits.onHit(player, kind, pos);
                }
            });
            return null;
        }
    }
}
