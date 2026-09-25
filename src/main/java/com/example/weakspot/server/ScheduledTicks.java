package com.example.weakspot.server;

import com.example.weakspot.Reflect;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.ScheduledBoost;
import com.example.weakspot.config.WeakSpotConfig;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 予約された tick（スケジュール tick）で動くレッドストーンの部品の加速（1.4.4）。MachineAccelerator が、
 * ワールドの tick の最後（バニラの予約の処理のあと）に、加速中のブロックごとに1回だけ呼ぶ。
 * 信号の部品（リピーター・コンパレーター・オブザーバー）は、予約の残り時間を余分の tick だけ前倒しし、0 以下なら今動かす。
 * ディスペンサー・ドロッパーは、予約があれば今動かし、1回の予約で複数発にする（ScheduledBoost.dispenseCount）。
 * どのブロックも、前倒しで動くのは 1 tick に1回まで（自分に信号を戻すクロックが、同じ tick の中で回り続けないように）。
 * 余分の発射は、まとめて出すと音と煙が重なって連射に見えないので、BURST_INTERVAL_TICKS ごとに1発ずつ出す（1.5.2）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class ScheduledTicks {

    /** ディメンションごとの、ディスペンサー・ドロッパーの残りの発射。 */
    private static final Map<Integer, Map<BlockPos, Burst>> BURSTS = new HashMap<>();

    private static final class Burst {
        final Block block;
        int left;
        long nextTick;

        Burst(Block block, int left, long nextTick) {
            this.block = block;
            this.left = left;
            this.nextTick = nextTick;
        }
    }

    /** BlockDispenser#dispense(World, BlockPos)。ドロッパーは自分の dispense を持っているので、そちらが呼ばれる。 */
    private static final Method DISPENSE = Reflect.method(BlockDispenser.class, "extra dispenser shots",
            new Class<?>[] {World.class, BlockPos.class}, "dispense", "func_176439_d");

    private ScheduledTicks() {
    }

    /**
     * @param extra この tick に余分に進める tick（倍率 − 1。端数は MachineBoost が持ち越す）
     * @param multiplier 今の倍率（ディスペンサーの発射の回数に使う）
     */
    static void boost(World world, BlockPos pos, int extra, double multiplier) {
        if (!(world instanceof WorldServer)) {
            return;
        }
        WorldServer server = (WorldServer) world;
        Block block = server.getBlockState(pos).getBlock();
        boolean dispenser = block instanceof BlockDispenser;
        // 予約がないときは、全部の予約をなめる処理をしない
        if ((!dispenser && extra <= 0) || !server.isUpdateScheduled(pos, block)) {
            return;
        }
        // X と Z だけで絞り込まれる（高さを見ない）ので、同じ列のほかの予約は、元の時刻と優先度のまま入れ直す
        List<NextTickListEntry> column = server.getPendingBlockUpdates(new StructureBoundingBox(
                pos.getX(), 0, pos.getZ(), pos.getX() + 1, server.getHeight(), pos.getZ() + 1), true);
        if (column == null) {
            return;
        }
        long now = server.getTotalWorldTime();
        NextTickListEntry mine = null;
        for (NextTickListEntry entry : column) {
            if (mine == null && entry.position.equals(pos) && Block.isEqualTo(entry.getBlock(), block)) {
                mine = entry;
            } else {
                server.scheduleBlockUpdate(entry.position, entry.getBlock(), (int) (entry.scheduledTime - now),
                        entry.priority);
            }
        }
        if (mine == null) {
            return;
        }
        long remaining = dispenser ? 0 : ScheduledBoost.advance(mine.scheduledTime - now, extra);
        if (remaining > 0) {
            server.scheduleBlockUpdate(pos, block, (int) remaining, mine.priority);
            return;
        }
        // バニラの予約の処理（WorldServer#tickUpdates）と同じ条件で動かす
        IBlockState state = server.getBlockState(pos);
        if (state.getMaterial() == Material.AIR || !Block.isEqualTo(state.getBlock(), mine.getBlock())) {
            return;
        }
        state.getBlock().updateTick(server, pos, state, server.rand);
        if (dispenser) {
            // 次の信号なら、前の信号の残りは捨てて出し直す
            Map<BlockPos, Burst> bursts = BURSTS.computeIfAbsent(server.provider.getDimension(), d -> new HashMap<>());
            int extraShots = ScheduledBoost.dispenseCount(multiplier, WeakSpotConfig.machineBoostMultiplier) - 1;
            if (extraShots > 0 && DISPENSE != null) {
                bursts.put(pos, new Burst(state.getBlock(), extraShots, now + ScheduledBoost.BURST_INTERVAL_TICKS));
            } else {
                bursts.remove(pos);
            }
        }
    }

    /** ワールドの tick の最後に、時間が来た余分の発射を1発ずつ出す。 */
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
            return;
        }
        Map<BlockPos, Burst> bursts = BURSTS.get(event.world.provider.getDimension());
        if (bursts == null || bursts.isEmpty()) {
            return;
        }
        World world = event.world;
        long now = world.getTotalWorldTime();
        for (Iterator<Map.Entry<BlockPos, Burst>> it = bursts.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<BlockPos, Burst> entry = it.next();
            BlockPos pos = entry.getKey();
            Burst burst = entry.getValue();
            if (now < burst.nextTick) {
                continue;
            }
            if (!world.isBlockLoaded(pos) || world.getBlockState(pos).getBlock() != burst.block) {
                it.remove();
                continue;
            }
            dispense(world, pos, burst.block);
            burst.left--;
            burst.nextTick = now + ScheduledBoost.BURST_INTERVAL_TICKS;
            if (burst.left <= 0) {
                it.remove();
            }
        }
    }

    private static void dispense(World world, BlockPos pos, Block block) {
        try {
            DISPENSE.invoke(block, world, pos);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to dispense from " + block.getRegistryName() + " at " + pos, e);
        }
    }

    /** サーバーが止まったら、残りの発射を捨てる。 */
    static void clear() {
        BURSTS.clear();
    }
}
