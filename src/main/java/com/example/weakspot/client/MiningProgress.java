package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.BlockHealthBar;
import java.lang.reflect.Field;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.LogManager;

/**
 * 自分の破壊の進み具合（PlayerControllerMP の非公開の curBlockDamageMP）と、掘っているブロック（currentBlock）を読む。
 * 開発環境は MCP 名、実際の環境（難読化された名前）は SRG 名なので、両方の名前を順に試す。
 * （Forge の ReflectionHelper.findField の3引数の版は、起動環境の判定で片方の名前しか試さず、非推奨でもあるので使わない。）
 * 見つからないときは警告を1回出し、耐久バーを出さない（ゲームは止めない）。
 *
 * 進み具合は tick ごとに進むので、ClientTickEvent の START（その tick の進捗の前）で前の値を覚え、
 * フレームごとに前の値と今の値の間を補間する。
 */
final class MiningProgress {

    private static final Field DAMAGE = find("curBlockDamageMP", "field_78770_f");
    private static final Field BLOCK = find("currentBlock", "field_178895_c");

    private static BlockPos prevPos;
    private static float prevDamage;

    private MiningProgress() {
    }

    private static Field find(String mcpName, String srgName) {
        for (String name : new String[] {mcpName, srgName}) {
            try {
                Field field = PlayerControllerMP.class.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException | RuntimeException e) {
                // 次の名前を試す
            }
        }
        LogManager.getLogger(WeakSpotMod.MODID).warn(
                "Cannot read PlayerControllerMP.{} ({}); the block health bar is disabled", mcpName, srgName);
        return null;
    }

    /** tick の START で呼ぶ。この tick の進捗が積まれる前の値を覚える。 */
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

    /**
     * 今のフレームの、ブロック pos の破壊の進み具合（前の tick の値と今の値の補間）。
     * 読めないとき、または今掘っているブロックが pos でないときは -1。
     */
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
