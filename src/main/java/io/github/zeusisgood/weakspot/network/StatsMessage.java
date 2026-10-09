package io.github.zeusisgood.weakspot.network;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.MiningStats;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** サーバー → クライアント: そのプレイヤーの「今回」と累計の統計と、的当てのサーバー内の上位（1.11.0）。 */
public class StatsMessage implements IMessage {

    private MiningStats session;
    private MiningStats total;
    /** 的当ての上位（名前と自己ベスト。上から）。 */
    private List<String> topNames = new ArrayList<>();
    private List<Long> topScores = new ArrayList<>();

    public StatsMessage() {
    }

    public StatsMessage(MiningStats session, MiningStats total) {
        this.session = session;
        this.total = total;
    }

    public StatsMessage(MiningStats session, MiningStats total, List<String> topNames, List<Long> topScores) {
        this(session, total);
        this.topNames = topNames;
        this.topScores = topScores;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        session = read(buf);
        total = read(buf);
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            topNames.add(ByteBufUtils.readUTF8String(buf));
            topScores.add(buf.readLong());
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        write(buf, session);
        write(buf, total);
        buf.writeInt(topNames.size());
        for (int i = 0; i < topNames.size(); i++) {
            ByteBufUtils.writeUTF8String(buf, topNames.get(i));
            buf.writeLong(topScores.get(i));
        }
    }

    /** 並びは MiningStats#writeTo（1.8.5 までと同じ並び）。 */
    private static MiningStats read(ByteBuf buf) {
        try {
            return MiningStats.readFrom(new ByteBufInputStream(buf));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void write(ByteBuf buf, MiningStats stats) {
        try {
            stats.writeTo(new ByteBufOutputStream(buf));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static class Handler implements IMessageHandler<StatsMessage, IMessage> {

        @Override
        public IMessage onMessage(StatsMessage message, MessageContext ctx) {
            WeakSpotMod.proxy.onStatsReceived(message.session, message.total, message.topNames, message.topScores);
            return null;
        }
    }
}
