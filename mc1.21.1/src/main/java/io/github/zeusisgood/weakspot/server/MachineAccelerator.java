package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.RightClickTargets;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.MachineBoost;
import io.github.zeusisgood.weakspot.common.MachineComboBoost;
import io.github.zeusisgood.weakspot.common.MachineParticles;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3f;

/**
 * Random Things の Time in a Bottle と同じ方式。ヒットした機械の位置と残り時間をメモリにだけ記録し
 * （エンティティは使わず、セーブデータにも残さない）、そのワールドの tick の最後にブロックエンティティの ticker を余分に呼ぶ。
 *
 * 機械 Mod の多くは ticker が 1 tick に 1 回だけ呼ばれる前提で作られているので、余分に呼ぶと不具合や例外の原因になりうる。
 * 例外はあえて捕まえない（クラッシュレポートで機械を特定し、excludedBlocks に足してもらう）。
 */
@EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class MachineAccelerator {

    private static final Map<ResourceKey<Level>, Map<BlockPos, MachineBoost>> BOOSTS = new HashMap<>();
    /** ブロック。 */
    private static final double PARTICLE_OUTSET = 0.05;
    /** ブロック。バニラの粒子と同じ。 */
    private static final double PARTICLE_RANGE = 32;

    private MachineAccelerator() {
    }

    /** combo はこのヒットを数えたあとの数。 */
    static void hit(Level world, BlockPos pos, int combo) {
        double multiplier = MachineComboBoost.multiplier(WeakSpotConfig.server.machine.machineBoostMultiplier,
                WeakSpotConfig.server.machine.machineBoostMaxMultiplier, combo);
        BOOSTS.computeIfAbsent(world.dimension(), d -> new HashMap<>())
                .computeIfAbsent(pos.immutable(), p -> new MachineBoost())
                .hit(multiplier, WeakSpotConfig.server.machine.machineBoostDurationTicks);
    }

    /** バニラのブロックエンティティの更新が終わった後に呼ぶ。 */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel world)) {
            return;
        }
        Map<BlockPos, MachineBoost> boosts = BOOSTS.get(world.dimension());
        if (boosts == null || boosts.isEmpty()) {
            return;
        }
        for (Iterator<Map.Entry<BlockPos, MachineBoost>> it = boosts.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<BlockPos, MachineBoost> entry = it.next();
            BlockPos pos = entry.getKey();
            int particles = entry.getValue().nextParticles();
            int calls = entry.getValue().nextTickCalls();
            if (!entry.getValue().isActive()) {
                it.remove();
            }
            if (!world.isLoaded(pos)) {
                continue;
            }
            if (WeakSpotConfig.server.machine.machineBoostParticles && particles > 0) {
                spawnParticles(world, pos, particles, entry.getValue().multiplier());
            }
            BlockState state = world.getBlockState(pos);
            if (RightClickTargets.isScheduledMachine(state.getBlock())) {
                ScheduledTicks.boost(world, pos, calls, entry.getValue().multiplier());
                continue;
            }
            BlockEntity be = world.getBlockEntity(pos);
            for (int i = 0; i < calls && be != null && !be.isRemoved() && world.getBlockEntity(pos) == be; i++) {
                if (!tick(world, pos, world.getBlockState(pos), be)) {
                    break;
                }
            }
        }
    }

    /** ticker がなければ false。 */
    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> boolean tick(Level world, BlockPos pos, BlockState state, T be) {
        BlockEntityTicker<T> ticker = state.getTicker(world, (BlockEntityType<T>) be.getType());
        if (ticker == null) {
            return false;
        }
        ticker.tick(world, pos, state, be);
        return true;
    }

    /** 色はコンボの段階（バニラの粉の粒子。近くのプレイヤー全員に見える）。 */
    private static void spawnParticles(ServerLevel world, BlockPos pos, int count, double multiplier) {
        int rgb = MachineParticles.colorFor(
                MachineParticles.comboFactor(multiplier, WeakSpotConfig.server.machine.machineBoostMultiplier));
        DustParticleOptions dust = new DustParticleOptions(
                new Vector3f((rgb >> 16 & 0xFF) / 255f, (rgb >> 8 & 0xFF) / 255f, (rgb & 0xFF) / 255f), 1.0f);
        VoxelShape shape = world.getBlockState(pos).getShape(world, pos);
        AABB box = (shape.isEmpty() ? new AABB(0, 0, 0, 1, 1, 1) : shape.bounds()).move(pos);
        RandomSource rand = world.random;
        for (int i = 0; i < count; i++) {
            double x = box.minX + rand.nextDouble() * (box.maxX - box.minX);
            double y = box.minY + rand.nextDouble() * (box.maxY - box.minY);
            double z = box.minZ + rand.nextDouble() * (box.maxZ - box.minZ);
            switch (rand.nextInt(5)) {
                case 0 -> y = box.maxY + PARTICLE_OUTSET;
                case 1 -> x = box.minX - PARTICLE_OUTSET;
                case 2 -> x = box.maxX + PARTICLE_OUTSET;
                case 3 -> z = box.minZ - PARTICLE_OUTSET;
                default -> z = box.maxZ + PARTICLE_OUTSET;
            }
            // 各自の設定（machineParticlesVisible）で見ない人には送らない
            for (ServerPlayer viewer : world.players()) {
                if (ServerSwitches.isParticlesVisible(viewer)
                        && viewer.distanceToSqr(x, y, z) <= PARTICLE_RANGE * PARTICLE_RANGE) {
                    world.sendParticles(viewer, dust, false, x, y, z, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    /** シングルプレイで別のワールドに入り直したときに持ち越さない。 */
    public static void clear() {
        BOOSTS.clear();
        ScheduledTicks.clear();
    }
}
