package com.example.weakspot.network;

import com.example.weakspot.common.HitKind;
import com.example.weakspot.server.AnimalHits;
import com.example.weakspot.server.FishingHits;
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
    /** 動物のヒットのときの動物のエンティティ ID。それ以外は -1。 */
    private int entityId = -1;

    public HitMessage() {
    }

    public HitMessage(HitKind kind, BlockPos pos, int streak) {
        this.kind = kind;
        this.pos = pos;
        this.streak = streak;
    }

    /** 動物のヒット。位置は使わない。 */
    public static HitMessage animal(int entityId, int streak) {
        HitMessage message = new HitMessage(HitKind.ANIMAL, BlockPos.ORIGIN, streak);
        message.entityId = entityId;
        return message;
    }

    /** 釣りのヒット。位置も動物も使わない。 */
    public static HitMessage fishing(int streak) {
        return new HitMessage(HitKind.FISHING, BlockPos.ORIGIN, streak);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        kind = HitKind.byId(buf.readByte());
        pos = BlockPos.fromLong(buf.readLong());
        streak = buf.readInt();
        entityId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(kind.ordinal());
        buf.writeLong(pos.toLong());
        buf.writeInt(streak);
        buf.writeInt(entityId);
    }

    public static class Handler implements IMessageHandler<HitMessage, IMessage> {

        @Override
        public IMessage onMessage(HitMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            HitKind kind = message.kind;
            BlockPos pos = message.pos;
            int streak = message.streak;
            int entityId = message.entityId;
            if (kind == null) {
                return null;
            }
            player.getServerWorld().addScheduledTask(() -> {
                if (kind == HitKind.MINING) {
                    ServerBoostTracker.onHit(player, pos, streak);
                } else if (kind == HitKind.ANIMAL) {
                    AnimalHits.onHit(player, entityId);
                } else if (kind == HitKind.FISHING) {
                    FishingHits.onHit(player);
                } else {
                    RightClickHits.onHit(player, kind, pos);
                }
            });
            return null;
        }
    }
}
