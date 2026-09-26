package com.example.weakspot.network;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.server.ServerStats;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** クライアント → サーバー: 自分の統計を送ってほしい（統計画面を開いたとき）。reset なら累計を消してから送る。 */
public class StatsRequestMessage implements IMessage {

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

    public static class Handler implements IMessageHandler<StatsRequestMessage, IMessage> {

        @Override
        public IMessage onMessage(StatsRequestMessage message, MessageContext ctx) {
            boolean reset = message.reset;
            return ServerThread.run(ctx, player -> {
                if (reset) {
                    ServerStats.resetTotal(player);
                }
                WeakSpotMod.network.sendTo(
                        new StatsMessage(ServerStats.session(player), ServerStats.total(player)), player);
            });
        }
    }
}
