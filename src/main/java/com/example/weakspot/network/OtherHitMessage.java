package com.example.weakspot.network;

import com.example.weakspot.WeakSpotMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * サーバー → クライアント: 近くの他のプレイヤーが弱点（採掘・成長・機械）にヒットした。ヒット音を鳴らすためだけに使う。
 * ハンドラーは専用サーバーでもインスタンス化されるので、クライアントのクラスは proxy 経由で触る。
 */
public class OtherHitMessage implements IMessage {

    private BlockPos pos;
    private int streak;

    public OtherHitMessage() {
    }

    public OtherHitMessage(BlockPos pos, int streak) {
        this.pos = pos;
        this.streak = streak;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        streak = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeInt(streak);
    }

    public static class Handler implements IMessageHandler<OtherHitMessage, IMessage> {

        @Override
        public IMessage onMessage(OtherHitMessage message, MessageContext ctx) {
            WeakSpotMod.proxy.onOtherPlayerHit(message.pos, message.streak);
            return null;
        }
    }
}
