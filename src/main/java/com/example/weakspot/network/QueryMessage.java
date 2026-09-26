package com.example.weakspot.network;

import com.example.weakspot.common.HitKind;
import com.example.weakspot.server.QueryHandlers;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * クライアント → サーバー: 弱点を出すための状態の問い合わせ（1.9.0 で、動物・釣り・機械の 3 つの問い合わせをまとめた）。
 * 種類と相手（動物は生き物の ID、機械はブロックの位置、釣りはなし）を送り、サーバーは StateMessage で返す。
 * 問い合わせの間隔は、クライアントの QueryThrottle が決める。
 */
public class QueryMessage implements IMessage {

    private HitKind kind;
    private long target;

    public QueryMessage() {
    }

    private QueryMessage(HitKind kind, long target) {
        this.kind = kind;
        this.target = target;
    }

    /** 動物の状態（動いているタイマー）。 */
    public static QueryMessage animal(int entityId) {
        return new QueryMessage(HitKind.ANIMAL, entityId);
    }

    /** 自分の浮きの状態。 */
    public static QueryMessage fishing() {
        return new QueryMessage(HitKind.FISHING, 0);
    }

    /** 機械の進み具合と燃料。 */
    public static QueryMessage machine(BlockPos pos) {
        return new QueryMessage(HitKind.MACHINE, pos.toLong());
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        kind = HitKind.byId(buf.readUnsignedByte());
        target = buf.readLong();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(kind.ordinal());
        buf.writeLong(target);
    }

    public static class Handler implements IMessageHandler<QueryMessage, IMessage> {
        @Override
        public IMessage onMessage(QueryMessage message, MessageContext ctx) {
            HitKind kind = message.kind;
            long target = message.target;
            if (kind == null) {
                return null;
            }
            return ServerThread.run(ctx, player -> QueryHandlers.onQuery(player, kind, target));
        }
    }
}
