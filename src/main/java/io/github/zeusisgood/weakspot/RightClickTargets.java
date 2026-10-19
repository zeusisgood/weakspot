package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.common.GrowthFilters;
import io.github.zeusisgood.weakspot.common.GrowthRoom;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.server.PlayerText;
import io.github.zeusisgood.weakspot.server.RightClickHits;
import io.github.zeusisgood.weakspot.server.ServerSwitches;
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
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * クライアントとサーバーで同じ条件を使う。しゃがんで両手が空なら機械、そうでなくメインハンドが空なら作物・苗木。
 * 対象のブロックを条件どおりに右クリックしたときは、そのブロックの通常の右クリック動作（GUI など）を両側で止める。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class RightClickTargets {

    /**
    * 育つのは柱の一番上の節だけなので、育てる余地（柱の高さ）をコードで判定し、効果は柱の一番上にかける。
    * 追加リスト（growthExtraBlocks）にあるときだけ対象。
    */
    private static final Set<String> COLUMN_BLOCKS = new HashSet<>(Arrays.asList("minecraft:reeds", "minecraft:cactus"));

    private RightClickTargets() {
    }

    /** 対象でなければ null。 */
    public static HitKind classify(World world, EntityPlayer player, BlockPos pos, SyncedSettings settings) {
        if (!PlayerRules.canUse(player) || !HeldItems.isFree(player.getHeldItemMainhand(), settings)) {
            return null;
        }
        IBlockState state = world.getBlockState(pos);
        // 収穫は isHarvestable の中で見る
        if (settings.enabled(HitKind.MACHINE) && player.isSneaking()
                && HeldItems.isFree(player.getHeldItemOffhand(), settings)
                && isMachine(world, pos, state, settings)) {
            return HitKind.MACHINE;
        }
        if (settings.enabled(HitKind.GROWTH) && isGrowable(world, pos, state, settings)) {
            return HitKind.GROWTH;
        }
        if (isHarvestable(state, settings)) {
            return HitKind.HARVEST;
        }
        if (settings.enabled(HitKind.GROWTH) && player.isSneaking() && isAutoGrowable(state, settings)
                && autoGrowthSupported(world, player)) {
            return HitKind.GROWTH;
        }
        return null;
    }

    /**
    * Random Things の Time in a Bottle と同じ考え方。追加リストのブロックは書いた条件だけで決め、IGrowable は canGrow で決めるので、
    * どちらも除く。
    */
    public static boolean isAutoGrowable(IBlockState state, SyncedSettings settings) {
        Block block = state.getBlock();
        String name = String.valueOf(block.getRegistryName());
        return block.getTickRandomly() && !(block instanceof IGrowable)
                && !settings.growthExcludedBlocks.contains(name) && !settings.growthFilters().contains(name);
    }

    /** 出す条件（しゃがみ）は classify で見る。 */
    public static boolean isGrowthTarget(World world, BlockPos pos, IBlockState state, SyncedSettings settings) {
        return isGrowable(world, pos, state, settings) || isAutoGrowable(state, settings);
    }

    /** サーバーとクライアントの両方が 1.11.2 以上のときだけ（古い側が右クリックを止めないので）。 */
    private static boolean autoGrowthSupported(World world, EntityPlayer player) {
        if (world.isRemote) {
            return WeakSpotMod.proxy.serverSince("1.11.2");
        }
        return player instanceof EntityPlayerMP && PlayerText.clientSince((EntityPlayerMP) player, "1.11.2");
    }

    /** BlockCrops（と、それを継承した Mod の作物）は isMaxAge、ネザーウォートは age 3、カカオ豆は age 2。 */
    public static boolean isHarvestable(IBlockState state, SyncedSettings settings) {
        if (!settings.enabled(HitKind.HARVEST)) {
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

    /** 追加リストのブロックは、書いた状態の条件（柱の植物は育てる余地も）を満たすとき。追加リストにない IGrowable は、成長できる状態のとき。 */
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

    /** F3 の画面と同じ書き方。 */
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

    /** バニラの updateTick と同じ条件。土台はサトウキビだけ確かめる。 */
    private static boolean hasColumnRoom(World world, BlockPos pos, Block block) {
        int above = blocksAbove(world, pos, block);
        int below = GrowthRoom.countRun(k -> world.getBlockState(pos.down(k)).getBlock() == block,
                GrowthRoom.MAX_COLUMN_HEIGHT);
        int height = above + 1 + below;
        boolean baseHolds = height != 1 || !(block instanceof BlockReed) || ((BlockReed) block).canBlockStay(world, pos);
        return GrowthRoom.hasColumnRoom(height, world.isAirBlock(pos.up(above + 1)), baseHolds);
    }

    /** 柱の高さの上限まで数えれば足りる。 */
    private static int blocksAbove(World world, BlockPos pos, Block block) {
        return GrowthRoom.countRun(k -> world.getBlockState(pos.up(k)).getBlock() == block,
                GrowthRoom.MAX_COLUMN_HEIGHT);
    }

    /** isGrowable が true のときに呼ぶ。 */
    public static BlockPos growthTarget(World world, BlockPos pos, IBlockState state) {
        Block block = state.getBlock();
        if (!COLUMN_BLOCKS.contains(String.valueOf(block.getRegistryName()))) {
            return pos;
        }
        return pos.up(blocksAbove(world, pos, block));
    }

    public static boolean isMachine(World world, BlockPos pos, IBlockState state, SyncedSettings settings) {
        return (world.getTileEntity(pos) instanceof ITickable || isScheduledMachine(state.getBlock()))
                && !settings.excludedBlocks.contains(String.valueOf(state.getBlock().getRegistryName()));
    }

    /** リピーター・コンパレーター、オブザーバー、ディスペンサー・ドロッパー（と、それを継承したブロック）。 */
    public static boolean isScheduledMachine(Block block) {
        return block instanceof BlockRedstoneDiode || block instanceof BlockObserver || block instanceof BlockDispenser;
    }

    /** クライアントは接続中ならサーバーの値。 */
    public static SyncedSettings settings(World world) {
        return world.isRemote ? WeakSpotMod.proxy.clientSettings() : SyncedSettings.server();
    }

    /**
    * クライアントで止めても、バニラは右クリックのパケットをサーバーへ送るので、サーバーでもこのイベントが発火し、
    * そこで「直前に右クリックした」ことを記録する。メインハンドは SUCCESS で止めて、オフハンドの処理（設置など）に進ませない。
    */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        World world = event.getWorld();
        HitKind kind = classify(world, player, event.getPos(), settings(world));
        if (kind == null
                || (world.isRemote ? !WeakSpotMod.proxy.isKindEnabled(kind) : !ServerSwitches.isEnabled(player, kind))) {
            // 弱点の一時オフ・その種類のオフは、通常の右クリックのまま。サーバーは、クライアントから届いた状態を見る
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        if (!world.isRemote) {
            RightClickHits.onRightClick(player, event.getPos());
        }
    }
}
