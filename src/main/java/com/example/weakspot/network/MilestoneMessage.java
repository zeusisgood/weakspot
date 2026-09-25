package com.example.weakspot.network;

import com.example.weakspot.WeakSpotMod;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * サーバー → 達成したプレイヤーのクライアント: ヒットの累計が節目に達した。演出を出すためだけに使う。
 * 1.7.0 から、節目の種類（採掘・種類ごと・合計）と、種類ごとの節目の HitKind の番号も運ぶ。
 */
public class MilestoneMessage implements IMessage {

    /** 採掘の節目（今までどおり）。 */
    public static final int MINING = 0;
    /** 種類ごとの節目（kindId に HitKind の番号）。 */
    public static final int KIND = 1;
    /** すべての種類の合計の節目。 */
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

    public static class Handler implements IMessageHandler<MilestoneMessage, IMessage> {

        @Override
        public IMessage onMessage(MilestoneMessage message, MessageContext ctx) {
            WeakSpotMod.proxy.onMilestone(message.type, message.kindId, message.milestone);
            return null;
        }
    }
}
