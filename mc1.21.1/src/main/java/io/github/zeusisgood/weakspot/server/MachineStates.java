package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.MachineProgress;
import io.github.zeusisgood.weakspot.network.StateMessage;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

/** 値はバニラの非公開のフィールド（accesstransformer.cfg）から読む。 */
public final class MachineStates {

    /** 位置ごと。 */
    private static final Map<ResourceKey<Level>, Map<BlockPos, MachineProgress.SpawnerDelay>> SPAWNERS = new HashMap<>();
    /** バニラが右クリックを受け付ける距離の余裕（RightClickHits と同じ）。 */
    private static final double REACH_MARGIN = 1.0;

    private MachineStates() {
    }

    /** 両側（クライアントは問い合わせるかどうかに使う）。かまど・燻製器・溶鉱炉・醸造台・スポナー。 */
    public static boolean hasBar(BlockEntity be) {
        return be instanceof AbstractFurnaceBlockEntity || be instanceof BrewingStandBlockEntity
                || be instanceof SpawnerBlockEntity;
    }

    public static void onQuery(ServerPlayer player, BlockPos pos) {
        if (!ServerSwitches.isEnabled(player, HitKind.MACHINE)) {
            return;
        }
        Level world = player.level();
        if (!world.isLoaded(pos) || !player.canInteractWithBlock(pos, REACH_MARGIN)) {
            return;
        }
        BlockEntity be = world.getBlockEntity(pos);
        float progress;
        float fuel = -1;
        if (be instanceof AbstractFurnaceBlockEntity furnace) {
            progress = (float) MachineProgress.ratio(furnace.cookingProgress, furnace.cookingTotalTime);
            fuel = (float) MachineProgress.ratio(furnace.litTime, furnace.litDuration);
        } else if (be instanceof BrewingStandBlockEntity stand) {
            progress = (float) MachineProgress.brewing(stand.brewTime);
            fuel = (float) MachineProgress.ratio(stand.fuel, MachineProgress.BREW_FUEL_MAX);
        } else if (be instanceof SpawnerBlockEntity spawner) {
            int delay = spawner.getSpawner().spawnDelay;
            progress = (float) SPAWNERS.computeIfAbsent(world.dimension(), d -> new HashMap<>())
                    .computeIfAbsent(pos.immutable(), p -> new MachineProgress.SpawnerDelay()).update(delay);
        } else {
            return;
        }
        WeakSpotMod.network.sendTo(StateMessage.machine(pos, progress, fuel), player);
    }

    public static void clear() {
        SPAWNERS.clear();
    }
}
