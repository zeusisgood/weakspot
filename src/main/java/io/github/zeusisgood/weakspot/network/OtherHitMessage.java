package io.github.zeusisgood.weakspot.network;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * サーバー → クライアント。ヒット音を鳴らすためだけに使う。pos は鳴らす位置、miss は的当ての ✕ に当てたとき。
 * ハンドラーは専用サーバーでもインスタンス化されるので、クライアントのクラスは proxy 経由で触る。
 */
public class OtherHitMessage implements IMessage {

    private BlockPos pos;
    private int streak;
    private boolean miss;

    public OtherHitMessage() {
    }

    public OtherHitMessage(BlockPos pos, int streak) {
        this(pos, streak, false);
    }

    public OtherHitMessage(BlockPos pos, int streak, boolean miss) {
        this.pos = pos;
        this.streak = streak;
        this.miss = miss;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        streak = buf.readInt();
        miss = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeInt(streak);
        buf.writeBoolean(miss);
    }

    public static class Handler implements IMessageHandler<OtherHitMessage, IMessage> {

        @Override
        public IMessage onMessage(OtherHitMessage message, MessageContext ctx) {
            WeakSpotMod.proxy.onOtherPlayerHit(message.pos, message.streak, message.miss);
            return null;
        }
    }
}
