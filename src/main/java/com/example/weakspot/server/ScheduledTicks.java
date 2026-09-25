package com.example.weakspot.server;

import com.example.weakspot.Reflect;
import com.example.weakspot.common.ScheduledBoost;
import com.example.weakspot.config.WeakSpotConfig;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;

/**
 * 予約された tick（スケジュール tick）で動くレッドストーンの部品の加速（1.4.4）。MachineAccelerator が、
 * ワールドの tick の最後（バニラの予約の処理のあと）に、加速中のブロックごとに1回だけ呼ぶ。
 * 信号の部品（リピーター・コンパレーター・オブザーバー）は、予約の残り時間を余分の tick だけ前倒しし、0 以下なら今動かす。
 * ディスペンサー・ドロッパーは、予約があれば今動かし、1回の予約で複数発にする（ScheduledBoost.dispenseCount）。
 * どのブロックも、前倒しで動くのは 1 tick に1回まで（自分に信号を戻すクロックが、同じ tick の中で回り続けないように）。
 */
final class ScheduledTicks {

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
            dispenseExtra(server, pos, state.getBlock(),
                    ScheduledBoost.dispenseCount(multiplier, WeakSpotConfig.machineBoostMultiplier) - 1);
        }
    }

    private static void dispenseExtra(WorldServer server, BlockPos pos, Block block, int times) {
        if (DISPENSE == null) {
            return;
        }
        for (int i = 0; i < times && server.getBlockState(pos).getBlock() == block; i++) {
            try {
                DISPENSE.invoke(block, server, pos);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to dispense from " + block.getRegistryName() + " at " + pos, e);
            }
        }
    }
}
