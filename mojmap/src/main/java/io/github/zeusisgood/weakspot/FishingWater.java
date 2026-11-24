package io.github.zeusisgood.weakspot;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.material.FluidState;

/**
 * 浮きが水に浮いているか。サーバー（待ち時間を減らす・弱点の段階）とクライアント（弱点を出す）で同じ判定を使う。
 * バニラは、浮いている状態（BOBBING）でも、浮きのブロックが水で液体の高さ &gt; 0 のときしか待ち時間を減らさない。
 * 水面ぎりぎりやブロックの縁に引っかかると、浮いている状態のまま位置が水の外になり、魚が来なくなる。
 * isInWater() は当たり判定の箱の下端と水面を比べるので、水源で浮きが揺れると約半分の tick が false になり、使えない。
 */
public final class FishingWater {

    private FishingWater() {
    }

    public static boolean isFloating(FishingHook hook) {
        BlockPos pos = hook.blockPosition();
        FluidState fluid = hook.level().getFluidState(pos);
        return fluid.is(FluidTags.WATER) && fluid.getHeight(hook.level(), pos) > 0;
    }
}
