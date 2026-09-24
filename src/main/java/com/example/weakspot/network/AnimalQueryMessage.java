package com.example.weakspot.network;

import com.example.weakspot.server.AnimalHits;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * クライアント → サーバー: 動物の状態の問い合わせ（エンティティ ID）。動物のタイマーはサーバーだけが持つので、
 * 右クリックを押しっぱなしで動物に照準を合わせている間、数 tick ごとと、ヒットのたびに送る。
 * サーバーは AnimalStateMessage で返す。
 */
public class AnimalQueryMessage implements IMessage {

    private int entityId;

    public AnimalQueryMessage() {
    }

    public AnimalQueryMessage(int entityId) {
        this.entityId = entityId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
    }

    public static class Handler implements IMessageHandler<AnimalQueryMessage, IMessage> {

        @Override
        public IMessage onMessage(AnimalQueryMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            int entityId = message.entityId;
            player.getServerWorld().addScheduledTask(() -> AnimalHits.onQuery(player, entityId));
            return null;
        }
    }
}
