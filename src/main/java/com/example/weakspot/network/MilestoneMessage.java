package com.example.weakspot.network;

import com.example.weakspot.WeakSpotMod;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** サーバー → 達成したプレイヤーのクライアント: 採掘ヒットの累計が節目に達した。演出を出すためだけに使う。 */
public class MilestoneMessage implements IMessage {

    private int milestone;

    public MilestoneMessage() {
    }

    public MilestoneMessage(int milestone) {
        this.milestone = milestone;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        milestone = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(milestone);
    }

    public static class Handler implements IMessageHandler<MilestoneMessage, IMessage> {

        @Override
        public IMessage onMessage(MilestoneMessage message, MessageContext ctx) {
            WeakSpotMod.proxy.onMilestone(message.milestone);
            return null;
        }
    }
}
