package com.example.weakspot.network;

import com.example.weakspot.server.FishingHits;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * クライアント → サーバー: 自分の浮きの状態の問い合わせ。待ち時間のタイマーはサーバーだけが持つので、
 * 浮きが水にある間、数 tick ごとと、ヒットのたびに送る。サーバーは FishingStateMessage で返す。
 */
public class FishingQueryMessage implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<FishingQueryMessage, IMessage> {

        @Override
        public IMessage onMessage(FishingQueryMessage message, MessageContext ctx) {
            return ServerThread.run(ctx, FishingHits::onQuery);
        }
    }
}
