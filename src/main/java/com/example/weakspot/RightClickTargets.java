package com.example.weakspot;

import com.example.weakspot.common.GrowthFilters;
import com.example.weakspot.common.GrowthRoom;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.server.RightClickHits;
import com.example.weakspot.server.ServerSwitches;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCocoa;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.BlockObserver;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockReed;
import net.minecraft.block.IGrowable;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 右クリックの弱点（作物・苗木などの植物、機械）の対象の判定。クライアントとサーバーで同じ条件を使う。
 * しゃがんで両手が空なら機械、そうでなくメインハンドが空なら作物・苗木（しゃがんでいても、機械でなければ作物・苗木）。
 * 実った作物は収穫（1.7.0）。
 * 対象のブロックを条件どおりに右クリックしたときは、そのブロックの通常の右クリック動作（GUI など）を両側で止める。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class RightClickTargets {

    /**
     * 同じブロックが縦に伸びる植物（サトウキビ、サボテン）。育つのは柱の一番上の節だけなので、育てる余地（柱の高さ）を
     * コードで判定し、効果は柱の一番上にかける。追加リスト（growthExtraBlocks）にあるときだけ対象（条件を書けば、それも満たすとき）。
     */
    private static final Set<String> COLUMN_BLOCKS = new HashSet<>(Arrays.asList("minecraft:reeds", "minecraft:cactus"));

    private RightClickTargets() {
    }

    /** 右クリックで出る弱点の種類。対象でなければ null。 */
    public static HitKind classify(World world, EntityPlayer player, BlockPos pos, SyncedSettings settings) {
        if (!PlayerRules.canUse(player) || !player.getHeldItemMainhand().isEmpty()) {
            return null;
        }
        IBlockState state = world.getBlockState(pos);
        if (player.isSneaking() && player.getHeldItemOffhand().isEmpty() && isMachine(world, pos, state, settings)) {
            return HitKind.MACHINE;
        }
        if (isGrowable(world, pos, state, settings)) {
            return HitKind.GROWTH;
        }
        if (isHarvestable(state, settings)) {
            return HitKind.HARVEST;
        }
        return null;
    }

    /**
     * 収穫の弱点の対象か（1.7.0）。実った作物: BlockCrops（と、それを継承した Mod の作物）で isMaxAge、
     * ネザーウォートの age 3、カカオ豆の age 2。
     */
    public static boolean isHarvestable(IBlockState state, SyncedSettings settings) {
        if (!settings.harvestWeakSpotEnabled) {
            return false;
        }
        Block block = state.getBlock();
        if (block instanceof BlockCrops) {
            return ((BlockCrops) block).isMaxAge(state);
        }
        if (block instanceof BlockNetherWart) {
            return state.getValue(BlockNetherWart.AGE) >= 3;
        }
        if (block instanceof BlockCocoa) {
            return state.getValue(BlockCocoa.AGE) >= 2;
        }
        return false;
    }

    /**
     * 成長の弱点の対象か。対象外リスト（growthExcludedBlocks）になく、次のどちらか（1.4.0）。
     * 追加リスト（growthExtraBlocks）にあるブロックは、書いた状態の条件（GrowthFilters。柱の植物は育てる余地も）を満たすとき。
     * 追加リストにない IGrowable は、成長できる状態のとき。
     */
    public static boolean isGrowable(World world, BlockPos pos, IBlockState state, SyncedSettings settings) {
        Block block = state.getBlock();
        String name = String.valueOf(block.getRegistryName());
        if (settings.growthExcludedBlocks.contains(name)) {
            return false;
        }
        GrowthFilters filters = settings.growthFilters();
        if (filters.contains(name)) {
            if (!filters.matches(name, propertyNames(state))) {
                return false;
            }
            return !COLUMN_BLOCKS.contains(name) || hasColumnRoom(world, pos, block);
        }
        if (block instanceof IGrowable) {
            return ((IGrowable) block).canGrow(world, pos, state, world.isRemote);
        }
        return false;
    }

    /** 状態のプロパティの名前 → 値の名前（F3 の画面と同じ書き方）。 */
    private static Map<String, String> propertyNames(IBlockState state) {
        Map<String, String> names = new HashMap<>();
        for (IProperty<?> property : state.getPropertyKeys()) {
            names.put(property.getName(), valueName(state, property));
        }
        return names;
    }

    private static <T extends Comparable<T>> String valueName(IBlockState state, IProperty<T> property) {
        return property.getName(state.getValue(property));
    }

    /** 柱の植物に育てる余地があるか（バニラの updateTick と同じ条件。土台はサトウキビだけ確かめる）。 */
    private static boolean hasColumnRoom(World world, BlockPos pos, Block block) {
        int above = blocksAbove(world, pos, block);
        int below = GrowthRoom.countRun(k -> world.getBlockState(pos.down(k)).getBlock() == block,
                GrowthRoom.MAX_COLUMN_HEIGHT);
        int height = above + 1 + below;
        boolean baseHolds = height != 1 || !(block instanceof BlockReed) || ((BlockReed) block).canBlockStay(world, pos);
        return GrowthRoom.hasColumnRoom(height, world.isAirBlock(pos.up(above + 1)), baseHolds);
    }

    /** pos の上に同じブロックがいくつ続くか（柱の高さの上限まで数えれば足りる）。 */
    private static int blocksAbove(World world, BlockPos pos, Block block) {
        return GrowthRoom.countRun(k -> world.getBlockState(pos.up(k)).getBlock() == block,
                GrowthRoom.MAX_COLUMN_HEIGHT);
    }

    /**
     * 成長ヒットの効果をかけるブロック。柱の植物は、育つのが一番上の節だけなので、柱の一番上。
     * それ以外は pos のまま。isGrowable が true のときに呼ぶ。
     */
    public static BlockPos growthTarget(World world, BlockPos pos, IBlockState state) {
        Block block = state.getBlock();
        if (!COLUMN_BLOCKS.contains(String.valueOf(block.getRegistryName()))) {
            return pos;
        }
        return pos.up(blocksAbove(world, pos, block));
    }

    /**
     * ITickable のタイルエンティティを持つか、予約された tick で動くレッドストーンの部品（1.4.4）で、対象外リストにないもの。
     */
    public static boolean isMachine(World world, BlockPos pos, IBlockState state, SyncedSettings settings) {
        return (world.getTileEntity(pos) instanceof ITickable || isScheduledMachine(state.getBlock()))
                && !settings.excludedBlocks.contains(String.valueOf(state.getBlock().getRegistryName()));
    }

    /**
     * 予約された tick（スケジュール tick）で動く、機械の加速の対象のレッドストーンの部品（1.4.4）。
     * リピーター・コンパレーター、オブザーバー、ディスペンサー・ドロッパー（と、それを継承したブロック）。
     */
    public static boolean isScheduledMachine(Block block) {
        return block instanceof BlockRedstoneDiode || block instanceof BlockObserver || block instanceof BlockDispenser;
    }

    /** この側で今使う設定値（クライアントは接続中ならサーバーの値）。 */
    public static SyncedSettings settings(World world) {
        return world.isRemote ? WeakSpotMod.proxy.clientSettings() : SyncedSettings.fromConfig();
    }

    /**
     * 通常の右クリック動作を止める。クライアントで止めても、バニラは右クリックのパケットをサーバーへ送るので、
     * サーバーでもこのイベントが発火し、そこで「直前に右クリックした」ことを記録する。
     * メインハンドは SUCCESS で止めて、オフハンドの処理（設置など）に進ませない。
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        World world = event.getWorld();
        HitKind kind = classify(world, player, event.getPos(), settings(world));
        if (kind == null
                || (world.isRemote ? !WeakSpotMod.proxy.isKindEnabled(kind) : !ServerSwitches.isEnabled(player, kind))) {
            // 弱点の一時オフ（HOME キー）、その種類のオフ（1.7.0）: 通常の右クリックのままにする。
            // サーバーは、クライアントから届いた状態を見る
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        if (!world.isRemote) {
            RightClickHits.onRightClick(player, event.getPos());
        }
    }
}
