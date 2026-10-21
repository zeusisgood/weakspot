package io.github.zeusisgood.weakspot.network;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** サーバー → 達成したプレイヤーのクライアント。演出を出すためだけに使う。 */
public class MilestoneMessage extends Message {

    public static final int MINING = 0;
    /** kindId に HitKind の番号。 */
    public static final int KIND = 1;
    public static final int TOTAL = 2;

    private int type;
    private int kindId;
    private long milestone;

    public MilestoneMessage() {
    }

    public MilestoneMessage(int type, int kindId, long milestone) {
        this.type = type;
        this.kindId = kindId;
        this.milestone = milestone;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        type = buf.readByte();
        kindId = buf.readByte();
        milestone = buf.readLong();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(type);
        buf.writeByte(kindId);
        buf.writeLong(milestone);
    }

    @Override
    void handle(NetworkEvent.Context ctx) {
        WeakSpotMod.proxy.onMilestone(type, kindId, milestone);
    }
}
