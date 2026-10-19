package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.Reflect;
import io.github.zeusisgood.weakspot.common.BlockHealthBar;
import java.lang.reflect.Field;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.math.BlockPos;

/**
 * PlayerControllerMP の非公開の curBlockDamageMP と currentBlock を読む。見つからなければ耐久バーを出さない（ゲームは止めない）。
 * 進み具合は tick ごとに進むので、tick の START で前の値を覚え、フレームごとに補間する。
 */
final class MiningProgress {

    private static final Field DAMAGE = Reflect.field(PlayerControllerMP.class, "the block health bar",
            "curBlockDamageMP", "field_78770_f");
    private static final Field BLOCK = Reflect.field(PlayerControllerMP.class, "the block health bar",
            "currentBlock", "field_178895_c");

    private static BlockPos prevPos;
    private static float prevDamage;

    private MiningProgress() {
    }

    /** tick の START で呼ぶ。 */
    static void sample(PlayerControllerMP controller) {
        if (DAMAGE == null || BLOCK == null) {
            return;
        }
        try {
            prevPos = (BlockPos) BLOCK.get(controller);
            prevDamage = DAMAGE.getFloat(controller);
        } catch (IllegalAccessException e) {
            prevPos = null;
        }
    }

    static void clear() {
        prevPos = null;
    }

    /** 読めないとき、または今掘っているブロックが pos でないときは -1。 */
    static double progress(PlayerControllerMP controller, BlockPos pos, float partialTicks) {
        if (DAMAGE == null || BLOCK == null || !controller.getIsHittingBlock()) {
            return -1;
        }
        BlockPos current;
        float damage;
        try {
            current = (BlockPos) BLOCK.get(controller);
            damage = DAMAGE.getFloat(controller);
        } catch (IllegalAccessException e) {
            return -1;
        }
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
