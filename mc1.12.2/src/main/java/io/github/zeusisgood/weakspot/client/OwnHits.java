package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.ComboSync;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.HitStreak;
import io.github.zeusisgood.weakspot.common.TargetRules;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import java.util.Arrays;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.Vec3d;

/** すべての種類の弱点が、当てたときに register を呼ぶ。 */
final class OwnHits {

    private static final long[] LAST_HIT_TICK = new long[HitKind.values().length];
    /** ブロックや種類をまたいで続く。 */
    static final HitStreak STREAK = new HitStreak();

    static {
        Arrays.fill(LAST_HIT_TICK, Long.MIN_VALUE / 2);
    }

    private OwnHits() {
    }

    static boolean canHit(HitKind kind, int minInterval) {
        return ClientWeakSpotHandler.clientTick - LAST_HIT_TICK[kind.ordinal()] >= minInterval;
    }

    /** ヒット後の連続ヒット数を返す。 */
    static int register(HitKind kind) {
        int hitStreak = STREAK.hit(ClientWeakSpotHandler.clientTick);
        HitSounds.playHit(hitStreak);
        ComboHud.onHit(kind, hitStreak, ClientWeakSpotHandler.clientTick + ClientWeakSpotHandler.framePartialTicks);
        LAST_HIT_TICK[kind.ordinal()] = ClientWeakSpotHandler.clientTick;
        goldParticles();
        return hitStreak;
    }

    /** 的当ての金のご褒美。 */
    private static void goldParticles() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!WeakSpotConfig.client.markers.goldHitParticles || !TargetRecords.unlocked(TargetRules.Tier.GOLD)
                || mc.player == null || mc.world == null) {
            return;
        }
        Vec3d eye = mc.player.getPositionEyes(1);
        Vec3d at = eye.add(mc.player.getLookVec().scale(1.5));
        for (int i = 0; i < GOLD_PARTICLES; i++) {
            // 赤石の粉の粒は、速さの欄が色（赤・緑・青）になる
            mc.world.spawnParticle(EnumParticleTypes.REDSTONE, at.x + (RANDOM.nextDouble() - 0.5) * 0.6,
                    at.y + (RANDOM.nextDouble() - 0.5) * 0.6, at.z + (RANDOM.nextDouble() - 0.5) * 0.6, 1.0, 0.84, 0.0);
        }
    }

    private static final int GOLD_PARTICLES = 8;
    private static final Random RANDOM = new Random();

    static void clearIntervals() {
        Arrays.fill(LAST_HIT_TICK, Long.MIN_VALUE / 2);
    }

    /** 当て続けている間と、もう途切れているときは何もしない。音・弾み・段階の演出は出さない。 */
    static void correctStreak(int serverCount) {
        long now = ClientWeakSpotHandler.clientTick;
        if (!ComboSync.canCorrect(now, STREAK.lastHitTick(), STREAK.count(now)) || serverCount == STREAK.count(now)) {
            return;
        }
        STREAK.correct(serverCount);
        ComboHud.correct(serverCount);
    }

    /** 死亡・リスポーン・ディメンション移動・ワールドを出たとき。 */
    static void resetStreak() {
        STREAK.reset();
        ComboHud.clear();
    }
}
