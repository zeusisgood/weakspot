package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.RightClickTargets;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.ScheduledBoost;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickContainerAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * MachineAccelerator が、ワールドの tick の最後（バニラの予約の処理のあと）に、加速中のブロックごとに 1 回だけ呼ぶ。
 * どのブロックも、前倒しで動くのは 1 tick に 1 回まで（自分に信号を戻すクロックが、同じ tick の中で回り続けないように）。
 * 予約はチャンクごとの LevelChunkTicks から探して、時刻を早めて入れ直す（同じ位置・同じブロックの予約は 1 つまでなので、先に消す）。
 */
@EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class ScheduledTicks {

    private static final Map<ResourceKey<Level>, Map<BlockPos, Burst>> BURSTS = new HashMap<>();

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

    private ScheduledTicks() {
    }

    /**
    * @param extra この tick に余分に進める tick（端数は MachineBoost が持ち越す）
    * @param multiplier ディスペンサー・クラフターの、1 回の信号で動く回数に使う
    */
    static void boost(ServerLevel level, BlockPos pos, int extra, double multiplier) {
        Block block = level.getBlockState(pos).getBlock();
        boolean dispenser = RightClickTargets.isBurstMachine(block);
        if ((!dispenser && extra <= 0) || !level.getBlockTicks().hasScheduledTick(pos, block)) {
            return;
        }
        TickContainerAccess<Block> container = level.getChunkAt(pos).getBlockTicks();
        if (!(container instanceof LevelChunkTicks<Block> ticks)) {
            return;
        }
        ScheduledTick<Block> mine = ticks.getAll()
                .filter(tick -> tick.pos().equals(pos) && tick.type() == block).findFirst().orElse(null);
        if (mine == null) {
            return;
        }
        long now = level.getGameTime();
        long remaining = dispenser ? 0 : ScheduledBoost.advance(mine.triggerTick() - now, extra);
        ticks.removeIf(tick -> tick == mine);
        if (remaining > 0) {
            level.scheduleTick(pos, block, (int) remaining, mine.priority());
            return;
        }
        // バニラの予約の処理（ServerLevel#tickBlock）と同じ条件で動かす
        BlockState state = level.getBlockState(pos);
        if (!state.is(block)) {
            return;
        }
        state.tick(level, pos, level.random);
        if (dispenser) {
            // 次の信号なら、前の信号の残りは捨てて出し直す
            Map<BlockPos, Burst> bursts = BURSTS.computeIfAbsent(level.dimension(), d -> new HashMap<>());
            int extraShots = ScheduledBoost.dispenseCount(multiplier,
                    WeakSpotConfig.server.machine.machineBoostMultiplier) - 1;
            if (extraShots > 0) {
                bursts.put(pos.immutable(), new Burst(block, extraShots, now + ScheduledBoost.BURST_INTERVAL_TICKS));
            } else {
                bursts.remove(pos);
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Map<BlockPos, Burst> bursts = BURSTS.get(level.dimension());
        if (bursts == null || bursts.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        for (Iterator<Map.Entry<BlockPos, Burst>> it = bursts.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<BlockPos, Burst> entry = it.next();
            BlockPos pos = entry.getKey();
            Burst burst = entry.getValue();
            if (now < burst.nextTick) {
                continue;
            }
            BlockState state = level.isLoaded(pos) ? level.getBlockState(pos) : null;
            if (state == null || state.getBlock() != burst.block) {
                it.remove();
                continue;
            }
            if (burst.block instanceof DispenserBlock dispenser) {
                dispenser.dispenseFrom(level, state, pos);
            } else {
                // クラフターの予約の処理は、作る 1 回だけ
                state.tick(level, pos, level.random);
            }
            burst.left--;
            burst.nextTick = now + ScheduledBoost.BURST_INTERVAL_TICKS;
            if (burst.left <= 0) {
                it.remove();
            }
        }
    }

    static void clear() {
        BURSTS.clear();
    }
}
