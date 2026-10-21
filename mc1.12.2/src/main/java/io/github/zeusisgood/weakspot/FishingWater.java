package io.github.zeusisgood.weakspot;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.util.math.BlockPos;

/**
 * 浮きが水に浮いているか。サーバー（待ち時間を減らす・弱点の段階）とクライアント（弱点を出す）で同じ判定を使う。
 * バニラは、浮いている状態（BOBBING）でも、浮きのブロックが水で液体の高さ &gt; 0 のときしか待ち時間を減らさない。
 * 水面ぎりぎりやブロックの縁に引っかかると、浮いている状態のまま位置が水の外になり、魚が来なくなる。
 * isInWater() は当たり判定の箱の下端と水面を比べるので、水源で浮きが揺れると約半分の tick が false になり、使えない。
 */
public final class FishingWater {

    private FishingWater() {
    }

    public static boolean isFloating(EntityFishHook hook) {
        BlockPos pos = new BlockPos(hook);
        IBlockState state = hook.world.getBlockState(pos);
        return state.getMaterial() == Material.WATER && BlockLiquid.getBlockLiquidHeight(state, hook.world, pos) > 0;
    }
}
