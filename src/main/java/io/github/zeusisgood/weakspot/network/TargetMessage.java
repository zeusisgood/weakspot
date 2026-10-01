package io.github.zeusisgood.weakspot.network;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * サーバー → クライアント: 的当て（1.11.0）の状態。始まった（カウントダウンから）・ヒット数が変わった・終わった・やめた、
 * と自己ベスト（ログインのとき。ご褒美の解放に使う）。ハンドラーは専用サーバーでもインスタンス化されるので proxy 経由。
 */
public class TargetMessage implements IMessage {

    public static final byte START = 0;
    public static final byte SCORE = 1;
    public static final byte END = 2;
    public static final byte CANCEL = 3;
    public static final byte RECORD = 4;

    private byte type;
    private int hits;
    private long best;
    private boolean newBest;
    /** 新しく届いたご褒美の段階（TargetRules.Tier の番号。なければ -1）。 */
    private byte newTier = -1;

    public TargetMessage() {
    }

    public TargetMessage(byte type, int hits, long best, boolean newBest, int newTier) {
        this.type = type;
        this.hits = hits;
        this.best = best;
        this.newBest = newBest;
        this.newTier = (byte) newTier;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        type = buf.readByte();
        hits = buf.readInt();
        best = buf.readLong();
        newBest = buf.readBoolean();
        newTier = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(type);
        buf.writeInt(hits);
        buf.writeLong(best);
        buf.writeBoolean(newBest);
        buf.writeByte(newTier);
    }

    public static class Handler implements IMessageHandler<TargetMessage, IMessage> {

        @Override
        public IMessage onMessage(TargetMessage m, MessageContext ctx) {
            WeakSpotMod.proxy.onTarget(m.type, m.hits, m.best, m.newBest, m.newTier);
            return null;
        }
    }
}
