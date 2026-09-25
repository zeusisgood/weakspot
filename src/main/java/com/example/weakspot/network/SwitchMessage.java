package com.example.weakspot.network;

import com.example.weakspot.server.ServerSwitches;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * クライアント → サーバー: 弱点のオン・オフ（HOME キー）の状態と、機械の粒子を見るか（1.6.0、machineParticlesVisible）。
 * 1.7.0 から、自分でオフにした弱点の種類（KindMask のビット）も送る。ログイン時と、どれかが変わったときに送る。
 */
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
            EntityPlayerMP player = ctx.getServerHandler().player;
            boolean enabled = message.enabled;
            boolean particlesVisible = message.particlesVisible;
            int disabledKinds = message.disabledKinds;
            player.getServerWorld().addScheduledTask(() -> {
                ServerSwitches.set(player, enabled);
                ServerSwitches.setParticlesVisible(player, particlesVisible);
                ServerSwitches.setDisabledKinds(player, disabledKinds);
            });
            return null;
        }
    }
}
