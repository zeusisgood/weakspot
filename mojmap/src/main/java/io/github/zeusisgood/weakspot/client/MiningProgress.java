package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.BlockHealthBar;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;

/**
 * MultiPlayerGameMode の非公開の destroyProgress と destroyBlockPos を読む（accesstransformer.cfg）。
 * 進み具合は tick ごとに進むので、tick の START で前の値を覚え、フレームごとに補間する。
 */
final class MiningProgress {

    private static BlockPos prevPos;
    private static float prevDamage;

    private MiningProgress() {
    }

    /** tick の START で呼ぶ。 */
    static void sample(MultiPlayerGameMode controller) {
        if (controller == null) {
            prevPos = null;
            return;
        }
        prevPos = controller.destroyBlockPos;
        prevDamage = controller.destroyProgress;
    }

    static void clear() {
        prevPos = null;
    }

    /** 今掘っているブロックが pos でないときは -1。 */
    static double progress(MultiPlayerGameMode controller, BlockPos pos, float partialTicks) {
        if (controller == null || !controller.isDestroying()) {
            return -1;
        }
        BlockPos current = controller.destroyBlockPos;
        float damage = controller.destroyProgress;
        if (!pos.equals(current)) {
            return -1;
        }
        // 前の tick と別のブロック、または進み具合が戻った（掘り直した）ときは補間しない
        if (!current.equals(prevPos) || damage < prevDamage) {
            return damage;
        }
        return BlockHealthBar.interpolate(prevDamage, damage, partialTicks);
    }
}
