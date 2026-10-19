package io.github.zeusisgood.weakspot.network;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.server.ServerStats;
import io.netty.buffer.ByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** クライアント → サーバー。reset なら累計を消してから送る。 */
public class StatsRequestMessage extends Message {

    private boolean reset;

    public StatsRequestMessage() {
    }

    public StatsRequestMessage(boolean reset) {
        this.reset = reset;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        reset = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(reset);
    }

    @Override
    void handle(IPayloadContext ctx) {
        boolean reset = this.reset;
        ServerThread.run(ctx, player -> {
            if (reset) {
                ServerStats.resetTotal(player);
            }
            WeakSpotMod.network.sendTo(ServerStats.message(player), player);
        });
    }
}
