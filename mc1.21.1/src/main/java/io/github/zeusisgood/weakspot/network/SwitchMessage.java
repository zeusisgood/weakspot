package io.github.zeusisgood.weakspot.network;

import io.github.zeusisgood.weakspot.server.ServerSwitches;
import io.netty.buffer.ByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** クライアント → サーバー。ログイン時と、どれかが変わったときに送る。 */
public class SwitchMessage extends Message {

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

    @Override
    void handle(IPayloadContext ctx) {
        boolean enabled = this.enabled;
        boolean particlesVisible = this.particlesVisible;
        int disabledKinds = this.disabledKinds;
        ServerThread.run(ctx, player -> {
            ServerSwitches.set(player, enabled);
            ServerSwitches.setParticlesVisible(player, particlesVisible);
            ServerSwitches.setDisabledKinds(player, disabledKinds);
        });
    }
}
