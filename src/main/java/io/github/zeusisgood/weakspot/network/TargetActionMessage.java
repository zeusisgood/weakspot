package io.github.zeusisgood.weakspot.network;

import io.github.zeusisgood.weakspot.server.TargetRounds;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** クライアント → サーバー: 的当て（1.11.0）で当てた（当たり・ハズレ）。 */
public class TargetActionMessage implements IMessage {

    public static final byte HIT = 0;
    public static final byte DECOY = 1;

    private byte action;

    public TargetActionMessage() {
    }

    public TargetActionMessage(byte action) {
        this.action = action;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(action);
    }

    public static class Handler implements IMessageHandler<TargetActionMessage, IMessage> {

        @Override
        public IMessage onMessage(TargetActionMessage m, MessageContext ctx) {
            byte action = m.action;
            return ServerThread.run(ctx, player -> TargetRounds.onAction(player, action));
        }
    }
}
