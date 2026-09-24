package com.example.weakspot;

import com.example.weakspot.common.HitKind;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.server.RightClickHits;
import net.minecraft.block.Block;
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
 * 右クリックの弱点（作物・苗木、機械）の対象の判定。クライアントとサーバーで同じ条件を使う。
 * しゃがんで両手が空なら機械、そうでなくメインハンドが空なら作物・苗木（しゃがんでいても、機械でなければ作物・苗木）。
 * 対象のブロックを条件どおりに右クリックしたときは、そのブロックの通常の右クリック動作（GUI など）を両側で止める。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class RightClickTargets {

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

    /** 成長できる状態の IGrowable で、対象外リストにないもの。 */
    public static boolean isGrowable(World world, BlockPos pos, IBlockState state, SyncedSettings settings) {
        Block block = state.getBlock();
        return block instanceof IGrowable
                && !settings.growthExcludedBlocks.contains(String.valueOf(block.getRegistryName()))
                && ((IGrowable) block).canGrow(world, pos, state, world.isRemote);
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
