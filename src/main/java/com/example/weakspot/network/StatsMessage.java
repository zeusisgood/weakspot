package com.example.weakspot.network;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.MiningStats;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** サーバー → クライアント: そのプレイヤーの「今回」と累計の統計。 */
public class StatsMessage implements IMessage {

    private MiningStats session;
    private MiningStats total;

    public StatsMessage() {
    }

    public StatsMessage(MiningStats session, MiningStats total) {
        this.session = session;
        this.total = total;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        session = read(buf);
        total = read(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        write(buf, session);
        write(buf, total);
    }

    /** 並びは MiningStats#writeTo（1.8.5 までと同じ並び）。 */
    private static MiningStats read(ByteBuf buf) {
        return MiningStats.readFrom(new MiningStats.Reader() {
            @Override
            public long readLong() {
                return buf.readLong();
            }

            @Override
            public double readDouble() {
                return buf.readDouble();
            }
        });
    }

    private static void write(ByteBuf buf, MiningStats stats) {
        stats.writeTo(new MiningStats.Writer() {
            @Override
            public void writeLong(long value) {
                buf.writeLong(value);
            }

            @Override
            public void writeDouble(double value) {
                buf.writeDouble(value);
            }
        });
    }

    public static class Handler implements IMessageHandler<StatsMessage, IMessage> {

        @Override
        public IMessage onMessage(StatsMessage message, MessageContext ctx) {
            WeakSpotMod.proxy.onStatsReceived(message.session, message.total);
            return null;
        }
    }
}
