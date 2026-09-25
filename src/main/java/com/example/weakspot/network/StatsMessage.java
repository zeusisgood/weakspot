package com.example.weakspot.network;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.MiningStats;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** サーバー → クライアント: そのプレイヤーの「今回」と累計の統計。 */
public class StatsMessage implements IMessage {

    private MiningStats session;
    private MiningStats total;

    public StatsMessage() {
    }

    public StatsMessage(MiningStats session, MiningStats total) {
        this.session = session;
        this.total = total;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        session = read(buf);
        total = read(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        write(buf, session);
        write(buf, total);
    }

    private static MiningStats read(ByteBuf buf) {
        MiningStats stats = new MiningStats();
        stats.hits = buf.readLong();
        stats.blocksBroken = buf.readLong();
        stats.blocksBrokenWithHit = buf.readLong();
        stats.maxHitsOnBlock = buf.readLong();
        stats.savedTicks = buf.readDouble();
        stats.growthHits = buf.readLong();
        stats.machineHits = buf.readLong();
        stats.maxStreak = buf.readLong();
        stats.animalHits = buf.readLong();
        stats.fishingHits = buf.readLong();
        stats.bowHits = buf.readLong();
        stats.critHits = buf.readLong();
        stats.vehicleHits = buf.readLong();
        stats.eatHits = buf.readLong();
        stats.sleepHits = buf.readLong();
        stats.ladderHits = buf.readLong();
        stats.elytraHits = buf.readLong();
        stats.enchantHits = buf.readLong();
        stats.harvestHits = buf.readLong();
        stats.throwHits = buf.readLong();
        stats.sprintHits = buf.readLong();
        return stats;
    }

    private static void write(ByteBuf buf, MiningStats stats) {
        buf.writeLong(stats.hits);
        buf.writeLong(stats.blocksBroken);
        buf.writeLong(stats.blocksBrokenWithHit);
        buf.writeLong(stats.maxHitsOnBlock);
        buf.writeDouble(stats.savedTicks);
        buf.writeLong(stats.growthHits);
        buf.writeLong(stats.machineHits);
        buf.writeLong(stats.maxStreak);
        buf.writeLong(stats.animalHits);
        buf.writeLong(stats.fishingHits);
        buf.writeLong(stats.bowHits);
        buf.writeLong(stats.critHits);
        buf.writeLong(stats.vehicleHits);
        buf.writeLong(stats.eatHits);
        buf.writeLong(stats.sleepHits);
        buf.writeLong(stats.ladderHits);
        buf.writeLong(stats.elytraHits);
        buf.writeLong(stats.enchantHits);
        buf.writeLong(stats.harvestHits);
        buf.writeLong(stats.throwHits);
        buf.writeLong(stats.sprintHits);
    }

    public static class Handler implements IMessageHandler<StatsMessage, IMessage> {

        @Override
        public IMessage onMessage(StatsMessage message, MessageContext ctx) {
            WeakSpotMod.proxy.onStatsReceived(message.session, message.total);
            return null;
        }
    }
}
