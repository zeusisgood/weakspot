package io.github.zeusisgood.weakspot.network;

import io.github.zeusisgood.weakspot.server.TargetRounds;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** クライアント → サーバー。 */
public class TargetActionMessage extends Message {

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

    @Override
    void handle(NetworkEvent.Context ctx) {
        byte action = this.action;
        ServerThread.run(ctx, player -> TargetRounds.onAction(player, action));
    }
}
