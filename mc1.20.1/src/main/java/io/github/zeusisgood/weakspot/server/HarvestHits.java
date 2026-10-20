package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.common.ComboFactor;
import io.github.zeusisgood.weakspot.common.HarvestBonus;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;

/**
 * 保護の Mod が止められるように、壊すときの BreakEvent を出す。収穫物は、ほかの Mod も変えられるように BlockDropsEvent を通す。
 * 植え直しには収穫物の「種」（getCloneItemStack）を 1 つ使い、なければ使わずに植え直す（不運で畑に穴が空かないように）。
 */
final class HarvestHits {

    private static final Random RANDOM = new Random();

    private HarvestHits() {
    }

    /** できなければ false（ヒットにも数えない）。 */
    static boolean harvest(ServerPlayer player, ServerLevel world, BlockPos pos, int combo) {
        BlockState state = world.getBlockState(pos);
        BlockState replanted = replanted(state);
        if (replanted == null) {
            return false;
        }
        if (ForgeHooks.onBlockBreakEvent(world, player.gameMode.getGameModeForPlayer(), player, pos) < 0) {
            return false;
        }
        ItemStack tool = player.getMainHandItem();
        List<ItemStack> drops = Block.getDrops(state, world, pos, null, player, tool);
        ItemStack seed = state.getBlock().getCloneItemStack(world, pos, state);
        List<ItemEntity> result = new ArrayList<>();
        boolean seedUsed = false;
        double factor = WeakSpotConfig.server.harvest.harvestComboBonus ? ComboFactor.factor(combo) : 1;
        boolean seedIsCrop = !seed.isEmpty() && !containsOther(drops, seed);
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }
            ItemStack stack = drop.copy();
            if (!seedUsed && !seed.isEmpty() && ItemStack.isSameItem(stack, seed)) {
                stack.shrink(1);
                seedUsed = true;
            }
            boolean isSeedOnly = !seed.isEmpty() && ItemStack.isSameItem(stack, seed) && !seedIsCrop;
            if (!isSeedOnly) {
                stack.setCount(HarvestBonus.apply(stack.getCount(), factor, RANDOM));
            }
            if (!stack.isEmpty()) {
                ItemEntity entity = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                entity.setDefaultPickUpDelay();
                result.add(entity);
            }
        }
        world.levelEvent(2001, pos, Block.getId(state));
        world.setBlock(pos, replanted, Block.UPDATE_ALL);
        for (ItemEntity entity : result) {
            world.addFreshEntity(entity);
        }
        state.spawnAfterBreak(world, pos, tool, true);
        return true;
    }

    /** なければ、種そのものが収穫物（ニンジン・ジャガイモ・ネザーウォート・カカオ豆）。 */
    private static boolean containsOther(List<ItemStack> drops, ItemStack seed) {
        for (ItemStack drop : drops) {
            if (!drop.isEmpty() && !ItemStack.isSameItem(drop, seed)) {
                return true;
            }
        }
        return false;
    }

    /** 対象でなければ null。カカオ豆は向きを保つ。 */
    private static BlockState replanted(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crops) {
            return crops.isMaxAge(state) ? crops.getStateForAge(0) : null;
        }
        if (block instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE
                    ? state.setValue(NetherWartBlock.AGE, 0) : null;
        }
        if (block instanceof CocoaBlock) {
            return state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE ? state.setValue(CocoaBlock.AGE, 0) : null;
        }
        return null;
    }
}
