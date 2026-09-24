package com.example.weakspot.server;

import com.example.weakspot.RightClickTargets;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.config.WeakSpotConfig;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.block.BlockMushroom;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/** 右クリックの弱点（作物・苗木、機械）のヒット通知の検証と効果（論理サーバー）。 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class RightClickHits {

    /**
     * 右クリックを押しっぱなしにすると、バニラのクライアントは4tickごとに右クリックを送る。
     * 最後の右クリックからこの tick 以内のヒットだけを受け付ける（ネットワークの揺らぎを見込んだ余裕）。
     */
    private static final int RIGHT_CLICK_WINDOW_TICKS = 10;
    /** 通知の間隔はネットワークの揺らぎで縮むので、この tick 数だけ甘く見る。 */
    private static final int INTERVAL_JITTER_TICKS = 2;

    private static final Map<UUID, Clicking> CLICKING = new HashMap<>();

    private RightClickHits() {
    }

    /** プレイヤーが最後に右クリックしたブロックと、種類ごとの最後のヒット。 */
    private static final class Clicking {
        BlockPos pos;
        long clickTick;
        final long[] lastHitTick = new long[HitKind.values().length];

        Clicking() {
            Arrays.fill(lastHitTick, Long.MIN_VALUE / 2);
        }
    }

    /** 対象のブロックを右クリックした（RightClickTargets から、サーバー側でだけ呼ばれる）。 */
    public static void onRightClick(EntityPlayer player, BlockPos pos) {
        Clicking clicking = CLICKING.computeIfAbsent(player.getUniqueID(), id -> new Clicking());
        clicking.pos = pos;
        clicking.clickTick = player.world.getTotalWorldTime();
    }

    /** クライアントからのヒット通知（サーバースレッドで実行される）。 */
    public static void onHit(EntityPlayerMP player, HitKind kind, BlockPos pos) {
        if (!ServerSwitches.isEnabled(player)) {
            return;
        }
        Clicking clicking = CLICKING.get(player.getUniqueID());
        World world = player.world;
        long now = world.getTotalWorldTime();
        if (clicking == null || !pos.equals(clicking.pos) || now - clicking.clickTick > RIGHT_CLICK_WINDOW_TICKS) {
            return;
        }
        if (!withinReach(player, pos)) {
            return;
        }
        SyncedSettings settings = SyncedSettings.fromConfig();
        if (RightClickTargets.classify(world, player, pos, settings) != kind) {
            return;
        }
        int minInterval = Math.max(0, minHitInterval(kind) - INTERVAL_JITTER_TICKS);
        if (now - clicking.lastHitTick[kind.ordinal()] < minInterval) {
            return;
        }
        clicking.lastHitTick[kind.ordinal()] = now;

        if (kind == HitKind.GROWTH) {
            grow(world, pos, settings);
            ServerStats.record(player, stats -> stats.recordGrowthHit());
        } else if (kind == HitKind.MACHINE) {
            MachineAccelerator.hit(world, pos);
            ServerStats.record(player, stats -> stats.recordMachineHit());
        }
        ServerStats.countStreak(player);
    }

    private static int minHitInterval(HitKind kind) {
        return kind == HitKind.MACHINE ? WeakSpotConfig.machineMinHitIntervalTicks : WeakSpotConfig.growthMinHitIntervalTicks;
    }

    /** バニラが右クリックを受け付ける距離と同じ（REACH_DISTANCE + 3 をブロックの中心から測る）。 */
    private static boolean withinReach(EntityPlayerMP player, BlockPos pos) {
        double reach = player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue() + 3;
        return player.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < reach * reach;
    }

    /**
     * randomTick を余分に呼ぶ。成長の条件（明るさ、水分など）はブロック自身が確かめるので、骨粉のように一気には育たない。
     * サトウキビ・サボテンは柱の一番上の節にかける（伸びると一番上が変わるので、毎回探し直す）。
     * キノコは randomTick では広がるだけなので、代わりに確率で骨粉と同じ grow を呼ぶ（巨大キノコ）。
     */
    private static void grow(World world, BlockPos pos, SyncedSettings settings) {
        IBlockState first = world.getBlockState(pos);
        if (first.getBlock() instanceof BlockMushroom) {
            BlockMushroom mushroom = (BlockMushroom) first.getBlock();
            if (world.rand.nextDouble() < WeakSpotConfig.mushroomGrowChance
                    && mushroom.canGrow(world, pos, first, false)) {
                mushroom.grow(world, world.rand, pos, first);
            }
            return;
        }
        for (int i = 0; i < WeakSpotConfig.growthTicksPerHit; i++) {
            IBlockState state = world.getBlockState(pos);
            if (!RightClickTargets.isGrowable(world, pos, state, settings)) {
                return;
            }
            BlockPos target = RightClickTargets.growthTarget(world, pos, state);
            IBlockState targetState = target.equals(pos) ? state : world.getBlockState(target);
            targetState.getBlock().randomTick(world, target, targetState, world.rand);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CLICKING.remove(event.player.getUniqueID());
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        CLICKING.remove(event.player.getUniqueID());
    }
}
