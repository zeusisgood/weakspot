package io.github.zeusisgood.weakspot.network;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** サーバー → クライアント。ハンドラーは専用サーバーでもインスタンス化されるので proxy 経由。 */
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
    /** TargetRules.Tier の番号。なければ -1。 */
    private byte newTier = -1;
    /** END だけ。だれもいなければ空と 0。 */
    private int good;
    private int decoys;
    private String topName = "";
    private long topScore;

    public TargetMessage() {
    }

    public TargetMessage(byte type, int hits, long best, boolean newBest, int newTier) {
        this.type = type;
        this.hits = hits;
        this.best = best;
        this.newBest = newBest;
        this.newTier = (byte) newTier;
    }

    public TargetMessage withResult(int good, int decoys, String topName, long topScore) {
        this.good = good;
        this.decoys = decoys;
        this.topName = topName == null ? "" : topName;
        this.topScore = topScore;
        return this;
    }

    public byte type() {
        return type;
    }

    public int hits() {
        return hits;
    }

    public long best() {
        return best;
    }

    public boolean newBest() {
        return newBest;
    }

    public int newTier() {
        return newTier;
    }

    public int good() {
        return good;
    }

    public int decoys() {
        return decoys;
    }

    public String topName() {
        return topName;
    }

    public long topScore() {
        return topScore;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        type = buf.readByte();
        hits = buf.readInt();
        best = buf.readLong();
        newBest = buf.readBoolean();
        newTier = buf.readByte();
        if (type == END) {
            good = buf.readInt();
            decoys = buf.readInt();
            topName = ByteBufUtils.readUTF8String(buf);
            topScore = buf.readLong();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(type);
        buf.writeInt(hits);
        buf.writeLong(best);
        buf.writeBoolean(newBest);
        buf.writeByte(newTier);
        if (type == END) {
            buf.writeInt(good);
            buf.writeInt(decoys);
            ByteBufUtils.writeUTF8String(buf, topName);
            buf.writeLong(topScore);
        }
    }

    public static class Handler implements IMessageHandler<TargetMessage, IMessage> {

        @Override
        public IMessage onMessage(TargetMessage m, MessageContext ctx) {
            WeakSpotMod.proxy.onTarget(m);
            return null;
        }
    }
}
