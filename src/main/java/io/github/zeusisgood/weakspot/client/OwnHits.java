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

/**
 * 自分のヒットの共通の処理（1.8.9 で ClientWeakSpotHandler から分けた）: 連続ヒット（コンボ）、ヒット音、コンボの表示、
 * 種類ごとのヒット間隔。すべての種類の弱点が、当てたときに register を呼ぶ。
 */
final class OwnHits {

    /** 種類ごとの最後のヒット（ヒット間隔の制限に使う）。 */
    private static final long[] LAST_HIT_TICK = new long[HitKind.values().length];
    /** 連続ヒット数。ブロックや種類をまたいで続き、ヒット音のピッチとコンボの表示に使う。 */
    static final HitStreak STREAK = new HitStreak();

    static {
        Arrays.fill(LAST_HIT_TICK, Long.MIN_VALUE / 2);
    }

    private OwnHits() {
    }

    /** 前のヒットから minInterval tick あいているか。 */
    static boolean canHit(HitKind kind, int minInterval) {
        return ClientWeakSpotHandler.clientTick - LAST_HIT_TICK[kind.ordinal()] >= minInterval;
    }

    /**
     * どの種類のヒットにも共通の処理: 連続ヒット、ヒット音、コンボの表示、ヒット間隔の記録。ヒット後の連続ヒット数を返す。
     */
    static int register(HitKind kind) {
        int hitStreak = STREAK.hit(ClientWeakSpotHandler.clientTick);
        HitSounds.playHit(hitStreak);
        ComboHud.onHit(kind, hitStreak, ClientWeakSpotHandler.clientTick + ClientWeakSpotHandler.framePartialTicks);
        LAST_HIT_TICK[kind.ordinal()] = ClientWeakSpotHandler.clientTick;
        goldParticles();
        return hitStreak;
    }

    /** 的当ての金のご褒美（1.11.0）: 当てたとき、目の前に金の粒を散らす。 */
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

    /** ワールドを出たとき。 */
    static void clearIntervals() {
        Arrays.fill(LAST_HIT_TICK, Long.MIN_VALUE / 2);
    }

    /**
     * サーバーの数で連続ヒットを直す（1.11.3。ComboSync）。当て続けている間（最後のヒットから IDLE_TICKS まで）と、
     * もう途切れているときは何もしない。数字は黙って書き換える（音・弾み・段階の演出は出さない）。
     */
    static void correctStreak(int serverCount) {
        long now = ClientWeakSpotHandler.clientTick;
        if (!ComboSync.canCorrect(now, STREAK.lastHitTick(), STREAK.count(now)) || serverCount == STREAK.count(now)) {
            return;
        }
        STREAK.correct(serverCount);
        ComboHud.correct(serverCount);
    }

    /** 連続ヒットを最初に戻す（死亡・リスポーン・ディメンション移動・ワールドを出たとき）。 */
    static void resetStreak() {
        STREAK.reset();
        ComboHud.clear();
    }
}
