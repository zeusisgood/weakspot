package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.common.GrowthFilters;
import io.github.zeusisgood.weakspot.common.GrowthRoom;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.server.RightClickHits;
import io.github.zeusisgood.weakspot.server.ServerSwitches;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
    private static final Set<String> COLUMN_BLOCKS =
            new HashSet<>(Arrays.asList("minecraft:sugar_cane", "minecraft:cactus"));

    private RightClickTargets() {
    }

    public static String id(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    /** 対象でなければ null。 */
    public static HitKind classify(Level world, Player player, BlockPos pos, SyncedSettings settings) {
        if (!PlayerRules.canUse(player) || !HeldItems.isFree(player.getMainHandItem(), settings)) {
            return null;
        }
        BlockState state = world.getBlockState(pos);
        // 収穫は isHarvestable の中で見る
        if (settings.enabled(HitKind.MACHINE) && player.isShiftKeyDown()
                && HeldItems.isFree(player.getOffhandItem(), settings)
                && isMachine(world, pos, state, settings)) {
            return HitKind.MACHINE;
        }
        if (settings.enabled(HitKind.GROWTH) && isGrowable(world, pos, state, settings)) {
            return HitKind.GROWTH;
        }
        if (isHarvestable(state, settings)) {
            return HitKind.HARVEST;
        }
        if (settings.enabled(HitKind.GROWTH) && player.isShiftKeyDown() && isAutoGrowable(state, settings)) {
            return HitKind.GROWTH;
        }
        return null;
    }

    /**
     * Random Things の Time in a Bottle と同じ考え方。追加リストのブロックは書いた条件だけで決め、骨粉で育つブロックは
     * isValidBonemealTarget で決めるので、どちらも除く（銅の錆び・アメジストの芽・鍾乳石もここに入る）。
     */
    public static boolean isAutoGrowable(BlockState state, SyncedSettings settings) {
        Block block = state.getBlock();
        String name = id(block);
        return state.isRandomlyTicking() && !(block instanceof BonemealableBlock)
                && !settings.growthExcludedBlocks.contains(name) && !settings.growthFilters().contains(name);
    }

    /** 出す条件（しゃがみ）は classify で見る。 */
    public static boolean isGrowthTarget(Level world, BlockPos pos, BlockState state, SyncedSettings settings) {
        return isGrowable(world, pos, state, settings) || isAutoGrowable(state, settings);
    }

    /** CropBlock（と、それを継承した Mod の作物）は isMaxAge、ネザーウォートは age 3、カカオ豆は age 2。 */
    public static boolean isHarvestable(BlockState state, SyncedSettings settings) {
        if (!settings.enabled(HitKind.HARVEST)) {
            return false;
        }
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (block instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= 3;
        }
        if (block instanceof CocoaBlock) {
            return state.getValue(CocoaBlock.AGE) >= 2;
        }
        return false;
    }

    /** 追加リストのブロックは、書いた状態の条件（柱の植物は育てる余地も）を満たすとき。追加リストにない骨粉で育つブロックは、育てられる状態のとき。 */
    public static boolean isGrowable(Level world, BlockPos pos, BlockState state, SyncedSettings settings) {
        Block block = state.getBlock();
        String name = id(block);
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
        if (block instanceof BonemealableBlock growable) {
            return growable.isValidBonemealTarget(world, pos, state, world.isClientSide);
        }
        return false;
    }

    /** F3 の画面と同じ書き方。 */
    private static Map<String, String> propertyNames(BlockState state) {
        Map<String, String> names = new HashMap<>();
        for (Property<?> property : state.getProperties()) {
            names.put(property.getName(), valueName(state, property));
        }
        return names;
    }

    private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    /** バニラの randomTick と同じ条件。土台はサトウキビだけ確かめる。 */
    private static boolean hasColumnRoom(Level world, BlockPos pos, Block block) {
        int above = blocksAbove(world, pos, block);
        int below = GrowthRoom.countRun(k -> world.getBlockState(pos.below(k)).getBlock() == block,
                GrowthRoom.MAX_COLUMN_HEIGHT);
        int height = above + 1 + below;
        boolean baseHolds = height != 1 || !(block instanceof SugarCaneBlock)
                || world.getBlockState(pos).canSurvive(world, pos);
        return GrowthRoom.hasColumnRoom(height, world.isEmptyBlock(pos.above(above + 1)), baseHolds);
    }

    /** 柱の高さの上限まで数えれば足りる。 */
    private static int blocksAbove(Level world, BlockPos pos, Block block) {
        return GrowthRoom.countRun(k -> world.getBlockState(pos.above(k)).getBlock() == block,
                GrowthRoom.MAX_COLUMN_HEIGHT);
    }

    /** isGrowable が true のときに呼ぶ。 */
    public static BlockPos growthTarget(Level world, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (!COLUMN_BLOCKS.contains(id(block))) {
            return pos;
        }
        return pos.above(blocksAbove(world, pos, block));
    }

    /**
     * 毎 tick 動くブロックエンティティの判定は、サーバーが作って送る一覧（SyncedSettings.tickingBlocks）で両側同じにする
     * （1.17 以降のかまどなどはサーバーにしか ticker がなく、クライアントだけでは見分けられないため）。
     */
    public static boolean isMachine(Level world, BlockPos pos, BlockState state, SyncedSettings settings) {
        String name = id(state.getBlock());
        return ((world.getBlockEntity(pos) != null && settings.tickingBlocks.contains(name))
                || isScheduledMachine(state.getBlock()))
                && !settings.excludedBlocks.contains(name);
    }

    /** サーバーで、毎 tick 動くブロックエンティティを持つブロックの登録名を集める（既定の状態で ticker があるもの）。 */
    public static Set<String> tickingBlocks(ServerLevel level) {
        Set<String> names = new HashSet<>();
        for (BlockEntityType<?> type : BuiltInRegistries.BLOCK_ENTITY_TYPE) {
            for (Block block : type.validBlocks) {
                if (block.defaultBlockState().getTicker(level, type) != null) {
                    names.add(id(block));
                }
            }
        }
        return names;
    }

    /** リピーター・コンパレーター、オブザーバー、ディスペンサー・ドロッパー（と、それを継承したブロック）。 */
    public static boolean isScheduledMachine(Block block) {
        return block instanceof DiodeBlock || block instanceof ObserverBlock || isBurstMachine(block);
    }

    /** 1 回の信号で複数回動かす部品（ディスペンサー・ドロッパー。1.20.1 にクラフターはない）。 */
    public static boolean isBurstMachine(Block block) {
        return block instanceof DispenserBlock;
    }

    /** クライアントは接続中ならサーバーの値。 */
    public static SyncedSettings settings(Level world) {
        return world.isClientSide ? WeakSpotMod.proxy.clientSettings() : SyncedSettings.server();
    }

    /**
     * クライアントで止めても、バニラは右クリックのパケットをサーバーへ送るので、サーバーでもこのイベントが発火し、
     * そこで「直前に右クリックした」ことを記録する。メインハンドは SUCCESS で止めて、オフハンドの処理（設置など）に進ませない。
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level world = event.getLevel();
        HitKind kind = classify(world, player, event.getPos(), settings(world));
        if (kind == null
                || (world.isClientSide ? !WeakSpotMod.proxy.isKindEnabled(kind) : !ServerSwitches.isEnabled(player, kind))) {
            // 弱点の一時オフ・その種類のオフは、通常の右クリックのまま。サーバーは、クライアントから届いた状態を見る
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!world.isClientSide) {
            RightClickHits.onRightClick(player, event.getPos());
        }
    }
}
