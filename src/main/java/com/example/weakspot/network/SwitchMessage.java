package com.example.weakspot.network;

import com.example.weakspot.server.ServerSwitches;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** クライアント → サーバー: 弱点のオン・オフ（J キー）の状態。ログイン時と、切り替えたときに送る。 */
public class SwitchMessage implements IMessage {

    private boolean enabled;

    public SwitchMessage() {
    }

    public SwitchMessage(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        enabled = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(enabled);
    }

    public static class Handler implements IMessageHandler<SwitchMessage, IMessage> {

        @Override
        public IMessage onMessage(SwitchMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            boolean enabled = message.enabled;
            player.getServerWorld().addScheduledTask(() -> ServerSwitches.set(player, enabled));
            return null;
        }
    }
}
