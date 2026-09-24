package com.example.weakspot;

import com.example.weakspot.common.GrowthRoom;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.server.RightClickHits;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.BlockReed;
import net.minecraft.block.IGrowable;
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
 * 対象のブロックを条件どおりに右クリックしたときは、そのブロックの通常の右クリック動作（GUI など）を両側で止める。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class RightClickTargets {

    /** IGrowable を持たない植物の育ち方。 */
    private enum ExtraPlant {
        /** 同じブロックが縦に伸びる（サトウキビ、サボテン）。育つのは柱の一番上の節だけ。 */
        COLUMN,
        /** 成長段階が進む（ネザーウォート）。 */
        STAGE
    }

    /**
     * IGrowable を持たないが、成長の弱点の対象にする植物（登録名）。コード内の固定のリスト（設定化は 1.2.0）。
     * 対象外は設定 growthExcludedBlocks で、このリストのブロックにも効く。
     */
    private static final Map<String, ExtraPlant> EXTRA_GROWTH_BLOCKS = new HashMap<>();

    /** ネザーウォートの最後の成長段階（BlockNetherWart.AGE の最大値）。 */
    private static final int NETHER_WART_LAST_STAGE = 3;

    static {
        EXTRA_GROWTH_BLOCKS.put("minecraft:reeds", ExtraPlant.COLUMN);
        EXTRA_GROWTH_BLOCKS.put("minecraft:cactus", ExtraPlant.COLUMN);
        EXTRA_GROWTH_BLOCKS.put("minecraft:nether_wart", ExtraPlant.STAGE);
    }

    private RightClickTargets() {
    }

    /** 右クリックで出る弱点の種類。対象でなければ null。 */
    public static HitKind classify(World world, EntityPlayer player, BlockPos pos, SyncedSettings settings) {
        if (player.capabilities.isCreativeMode || player.isSpectator() || !player.getHeldItemMainhand().isEmpty()) {
            return null;
        }
        IBlockState state = world.getBlockState(pos);
        if (player.isSneaking() && player.getHeldItemOffhand().isEmpty() && isMachine(world, pos, state, settings)) {
            return HitKind.MACHINE;
        }
        if (isGrowable(world, pos, state, settings)) {
            return HitKind.GROWTH;
        }
        return null;
    }

    /**
     * 成長の弱点の対象か。対象外リスト（growthExcludedBlocks）になく、次のどちらか。
     * 成長できる状態の IGrowable、または追加リストの植物で育てる余地があるもの（GrowthRoom）。
     */
    public static boolean isGrowable(World world, BlockPos pos, IBlockState state, SyncedSettings settings) {
        Block block = state.getBlock();
        String name = String.valueOf(block.getRegistryName());
        if (settings.growthExcludedBlocks.contains(name)) {
            return false;
        }
        if (block instanceof IGrowable) {
            return ((IGrowable) block).canGrow(world, pos, state, world.isRemote);
        }
        ExtraPlant extra = EXTRA_GROWTH_BLOCKS.get(name);
        if (extra == ExtraPlant.COLUMN) {
            return hasColumnRoom(world, pos, block);
        }
        if (extra == ExtraPlant.STAGE && block instanceof BlockNetherWart) {
            return GrowthRoom.hasStageRoom(state.getValue(BlockNetherWart.AGE), NETHER_WART_LAST_STAGE);
        }
        return false;
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
        if (block instanceof IGrowable
                || EXTRA_GROWTH_BLOCKS.get(String.valueOf(block.getRegistryName())) != ExtraPlant.COLUMN) {
            return pos;
        }
        return pos.up(blocksAbove(world, pos, block));
    }

    /** ITickable のタイルエンティティを持ち、対象外リストにないもの。 */
    public static boolean isMachine(World world, BlockPos pos, IBlockState state, SyncedSettings settings) {
        return world.getTileEntity(pos) instanceof ITickable
                && !settings.excludedBlocks.contains(String.valueOf(state.getBlock().getRegistryName()));
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
        if (world.isRemote && !WeakSpotConfig.weakSpotsEnabled) {
            // 弱点の一時オフ（J キー）: 自分のクライアントでは通常の右クリックのままにする
            return;
        }
        HitKind kind = classify(world, player, event.getPos(), settings(world));
        if (kind == null) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        if (!world.isRemote) {
            RightClickHits.onRightClick(player, event.getPos());
        }
    }
}
