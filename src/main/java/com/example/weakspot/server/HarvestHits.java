package com.example.weakspot.server;

import com.example.weakspot.common.HarvestBonus;
import com.example.weakspot.common.ComboFactor;
import com.example.weakspot.config.WeakSpotConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCocoa;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.world.BlockEvent;

/**
 * 収穫の弱点の効果（論理サーバー。1.7.0。検証は成長と同じ RightClickHits）。実った作物を収穫して植え直す。
 * 保護の Mod が止められるように、壊すときの BreakEvent を出す。収穫物は、ほかの Mod も変えられるように
 * HarvestDropsEvent を通す。植え直しには収穫物の「種」（ピックしたときの物。getPickBlock）を 1 つ使い、なければ使わずに植え直す
 * （不運で畑に穴が空かないように）。種と別の収穫物は、コンボの掛け数だけ増やす（harvestComboBonus）。
 */
final class HarvestHits {

    private HarvestHits() {
    }

    /** 収穫して植え直す。できなければ false（ヒットにも数えない）。 */
    static boolean harvest(EntityPlayerMP player, World world, BlockPos pos, int combo) {
        IBlockState state = world.getBlockState(pos);
        IBlockState replanted = replanted(state);
        if (replanted == null) {
            return false;
        }
        BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(world, pos, state, player);
        if (MinecraftForge.EVENT_BUS.post(breakEvent)) {
            return false;
        }
        Block block = state.getBlock();
        NonNullList<ItemStack> drops = NonNullList.create();
        block.getDrops(drops, world, pos, state, 0);
        float chance = ForgeEventFactory.fireBlockHarvesting(drops, world, pos, state, 0, 1.0F, false, player);
        ItemStack seed = block.getPickBlock(state, null, world, pos, player);
        List<ItemStack> result = new ArrayList<>();
        boolean seedUsed = false;
        double factor = WeakSpotConfig.harvestComboBonus ? ComboFactor.factor(combo) : 1;
        // 種と別の収穫物がないとき（ニンジンなど）は、種そのものが収穫物なので、植え直しに使った残りを増やす
        boolean seedIsCrop = !seed.isEmpty() && !containsOther(drops, seed);
        for (ItemStack drop : drops) {
            if (drop.isEmpty() || world.rand.nextFloat() > chance) {
                continue;
            }
            ItemStack stack = drop.copy();
            if (!seedUsed && !seed.isEmpty() && ItemStack.areItemsEqual(stack, seed)) {
                stack.shrink(1);
                seedUsed = true;
            }
            boolean isSeedOnly = !seed.isEmpty() && ItemStack.areItemsEqual(stack, seed) && !seedIsCrop;
            if (!isSeedOnly) {
                stack.setCount(HarvestBonus.apply(stack.getCount(), factor, world.rand));
            }
            if (!stack.isEmpty()) {
                result.add(stack);
            }
        }
        world.playEvent(2001, pos, Block.getStateId(state));
        world.setBlockState(pos, replanted, 3);
        for (ItemStack stack : result) {
            Block.spawnAsEntity(world, pos, stack);
        }
        return true;
    }

    /**
     * 収穫物の中に、種と別の物（小麦の種に対する小麦など）があるか。なければ、種そのものが収穫物
     * （ニンジン・ジャガイモ・ネザーウォート・カカオ豆）なので、植え直しに使った残りを増やす。
     */
    private static boolean containsOther(List<ItemStack> drops, ItemStack seed) {
        for (ItemStack drop : drops) {
            if (!drop.isEmpty() && !ItemStack.areItemsEqual(drop, seed)) {
                return true;
            }
        }
        return false;
    }

    /** 植え直した（年齢 0 の）状態。対象でなければ null。カカオ豆は向きを保つ。 */
    private static IBlockState replanted(IBlockState state) {
        Block block = state.getBlock();
        if (block instanceof BlockCrops) {
            BlockCrops crops = (BlockCrops) block;
            return crops.isMaxAge(state) ? crops.withAge(0) : null;
        }
        if (block instanceof BlockNetherWart) {
            return state.getValue(BlockNetherWart.AGE) >= 3 ? state.withProperty(BlockNetherWart.AGE, 0) : null;
        }
        if (block instanceof BlockCocoa) {
            return state.getValue(BlockCocoa.AGE) >= 2 ? state.withProperty(BlockCocoa.AGE, 0) : null;
        }
        return null;
    }
}
