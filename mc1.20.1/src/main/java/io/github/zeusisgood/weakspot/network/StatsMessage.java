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
import net.minecraftforge.network.NetworkEvent;

/** サーバー → クライアント。 */
public class StatsMessage extends Message {

    private MiningStats session;
    private MiningStats total;
    /** 上から。 */
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
            topNames.add(Bytes.readString(buf));
            topScores.add(buf.readLong());
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        write(buf, session);
        write(buf, total);
        buf.writeInt(topNames.size());
        for (int i = 0; i < topNames.size(); i++) {
            Bytes.writeString(buf, topNames.get(i));
            buf.writeLong(topScores.get(i));
        }
    }

    /** 並びは MiningStats#writeTo。 */
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

    @Override
    void handle(NetworkEvent.Context ctx) {
        WeakSpotMod.proxy.onStatsReceived(session, total, topNames, topScores);
    }
}
