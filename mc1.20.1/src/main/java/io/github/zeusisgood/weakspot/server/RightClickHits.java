package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.RightClickTargets;
import io.github.zeusisgood.weakspot.common.HitBudget;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class RightClickHits {

    /**
    * 右クリックを押しっぱなしにすると、バニラのクライアントは 4 tick ごとに右クリックを送る。
    * 最後の右クリックからこの tick 以内のヒットだけを受け付ける（ネットワークの揺らぎを見込んだ余裕）。
    */
    private static final int RIGHT_CLICK_WINDOW_TICKS = 10;
    /** バニラが右クリックを受け付ける距離の余裕（ServerGamePacketListenerImpl#handleUseItemOn と同じ）。 */
    private static final double REACH_MARGIN = 1.5;

    private static final Map<UUID, Clicking> CLICKING = new HashMap<>();

    private RightClickHits() {
    }

    private static final class Clicking {
        BlockPos pos;
        long clickTick;
        final Map<HitKind, HitBudget> budgets = new EnumMap<>(HitKind.class);
    }

    /** RightClickTargets から、サーバー側でだけ呼ばれる。 */
    public static void onRightClick(Player player, BlockPos pos) {
        Clicking clicking = CLICKING.computeIfAbsent(player.getUUID(), id -> new Clicking());
        clicking.pos = pos.immutable();
        clicking.clickTick = player.level().getGameTime();
    }

    /** streak は他のプレイヤーのヒット音の音階に使う。 */
    public static void onHit(ServerPlayer player, HitKind kind, BlockPos pos, int streak) {
        if (!HitGate.allowed(player, kind)) {
            return;
        }
        Clicking clicking = CLICKING.get(player.getUUID());
        ServerLevel world = player.serverLevel();
        long now = world.getGameTime();
        if (clicking == null || !pos.equals(clicking.pos) || now - clicking.clickTick > RIGHT_CLICK_WINDOW_TICKS) {
            return;
        }
        if (!player.canReach(pos, REACH_MARGIN)) {
            return;
        }
        SyncedSettings settings = SyncedSettings.server();
        if (RightClickTargets.classify(world, player, pos, settings) != kind) {
            return;
        }
        if (!HitGate.take(clicking.budgets.computeIfAbsent(kind, k -> new HitBudget()), now, kind)) {
            return;
        }

        // 機械の倍率・収穫のおまけはこのヒットを数えたあとのコンボで決めるので、先に数える
        if (kind == HitKind.HARVEST) {
            // 収穫できなかった（保護された土地など）ときは、ヒットに数えない
            int combo = ServerStats.peekStreak(player);
            if (!HarvestHits.harvest(player, world, pos, combo)) {
                return;
            }
        }
        int combo = HitGate.accept(player, kind, pos, streak);
        if (kind == HitKind.GROWTH) {
            BlockState before = world.getBlockState(pos);
            BlockPos target = RightClickTargets.growthTarget(world, pos, before);
            BlockState targetBefore = world.getBlockState(target);
            // 自動の対象は、変わらないことも多いので「育たない」の知らせを出さない
            boolean auto = !RightClickTargets.isGrowable(world, pos, before, settings);
            grow(world, pos, settings);
            // 柱が伸びると一番上の節が変わるので、前の一番上の節と、叩いたブロックの両方を比べる
            boolean changed = world.getBlockState(pos) != before || world.getBlockState(target) != targetBefore;
            if (!auto) {
                GrowthWarnings.onGrowthHit(player, world, pos, changed);
            }
        } else if (kind == HitKind.MACHINE) {
            MachineAccelerator.hit(world, pos, combo);
        }
    }

    /**
    * randomTick を余分に呼ぶ。成長の条件（明るさ、水分など）はブロック自身が確かめるので、骨粉のように一気には育たない。
    * キノコは randomTick では広がるだけなので、代わりに確率で骨粉と同じ育ち方をさせる（巨大キノコ）。
    */
    private static void grow(ServerLevel world, BlockPos pos, SyncedSettings settings) {
        BlockState first = world.getBlockState(pos);
        if (first.getBlock() instanceof MushroomBlock mushroom) {
            if (world.random.nextDouble() < WeakSpotConfig.server.growth.mushroomGrowChance
                    && mushroom.isValidBonemealTarget(world, pos, first, false)) {
                mushroom.growMushroom(world, pos, first, world.random);
            }
            return;
        }
        if (!RightClickTargets.isGrowable(world, pos, first, settings)
                && world.getGameRules().getInt(GameRules.RULE_RANDOMTICKING) <= 0) {
            // 自動の対象は、ランダム tick を止めたワールドでは早めない（Mod のブロックには、そのとき
            // ランダム tick を受けると例外を投げるものがある）
            return;
        }
        for (int i = 0; i < WeakSpotConfig.server.growth.growthTicksPerHit; i++) {
            BlockState state = world.getBlockState(pos);
            if (!RightClickTargets.isGrowthTarget(world, pos, state, settings)) {
                return;
            }
            BlockPos target = RightClickTargets.growthTarget(world, pos, state);
            BlockState targetState = target.equals(pos) ? state : world.getBlockState(target);
            targetState.randomTick(world, target, world.random);
        }
    }

    static void forget(Player player, HitGate.Leave leave) {
        if (leave != HitGate.Leave.RESPAWN) {
            CLICKING.remove(player.getUUID());
        }
    }
}
