package io.github.zeusisgood.weakspot.network;

import io.github.zeusisgood.weakspot.server.ServerSwitches;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** クライアント → サーバー。ログイン時と、どれかが変わったときに送る。 */
public class SwitchMessage implements IMessage {

    private boolean enabled;
    private boolean particlesVisible = true;
    private int disabledKinds;

    public SwitchMessage() {
    }

    public SwitchMessage(boolean enabled, boolean particlesVisible, int disabledKinds) {
        this.enabled = enabled;
        this.particlesVisible = particlesVisible;
        this.disabledKinds = disabledKinds;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        enabled = buf.readBoolean();
        particlesVisible = buf.readBoolean();
        disabledKinds = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(enabled);
        buf.writeBoolean(particlesVisible);
        buf.writeInt(disabledKinds);
    }

    public static class Handler implements IMessageHandler<SwitchMessage, IMessage> {

        @Override
        public IMessage onMessage(SwitchMessage message, MessageContext ctx) {
            boolean enabled = message.enabled;
            boolean particlesVisible = message.particlesVisible;
            int disabledKinds = message.disabledKinds;
            return ServerThread.run(ctx, player -> {
                ServerSwitches.set(player, enabled);
                ServerSwitches.setParticlesVisible(player, particlesVisible);
                ServerSwitches.setDisabledKinds(player, disabledKinds);
            });
        }
    }
}
