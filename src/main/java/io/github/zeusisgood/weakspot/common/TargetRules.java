package io.github.zeusisgood.weakspot.common;

/**
 * 的当て（1.11.0）の決まり。30 秒のラウンドの時間・難しさの段階・記録の数え方・ご褒美の段階。Minecraft に依存しない。
 * 大きさ・距離・動きの数字は、試してもらってから調整する（SPEC_v1.11.0.md の 1.4）。
 */
public final class TargetRules {

    /** カウントダウン（3・2・1）の長さ（tick）。 */
    public static final int COUNTDOWN_TICKS = 60;
    /** ラウンドの長さ（tick。30 秒）。 */
    public static final int ROUND_TICKS = 600;
    /** 難しさの段階の長さ（tick。10 秒ごとに 3 段階）。 */
    public static final int PHASE_TICKS = 200;
    /** ハズレに当てたときに減らすヒット数。 */
    public static final int MISS_PENALTY = 5;
    /** クライアントのヒットの最小間隔（tick）。サーバーは 2 tick 甘くする。 */
    public static final int MIN_HIT_INTERVAL_TICKS = 4;
    /** 始めてから、この tick の間のもう一度の右クリックは、やめる操作とみなさない（押しっぱなしの連打を除く）。 */
    public static final int CANCEL_GUARD_TICKS = 10;
    /** ハズレの位置を変える間隔（tick）。 */
    public static final int DECOY_MOVE_TICKS = 40;

    /** 段階ごとの、弱点の半径（GUI のピクセル）・照準からの距離（度）・動きの速さ（度 / tick）・ハズレの数。 */
    private static final double[] RADIUS = {16, 11, 8};
    private static final double[] MIN_OFFSET = {5, 10, 12};
    private static final double[] MAX_OFFSET = {12, 18, 24};
    private static final double[] SPEED = {0, 0.35, 0.8};
    private static final int[] DECOYS = {0, 0, 2};

    private TargetRules() {
    }

    /** ラウンドの始まり（カウントダウンの後）からの tick で、難しさの段階（0・1・2）。 */
    public static int phase(long ticksIntoRound) {
        return (int) Math.max(0, Math.min(2, ticksIntoRound / PHASE_TICKS));
    }

    public static double radius(int phase) {
        return RADIUS[clamp(phase)];
    }

    public static double minOffset(int phase) {
        return MIN_OFFSET[clamp(phase)];
    }

    public static double maxOffset(int phase) {
        return MAX_OFFSET[clamp(phase)];
    }

    public static double speed(int phase) {
        return SPEED[clamp(phase)];
    }

    public static int decoys(int phase) {
        return DECOYS[clamp(phase)];
    }

    private static int clamp(int phase) {
        return Math.max(0, Math.min(2, phase));
    }

    /** 当てたあとのヒット数（当たり +1、ハズレ −5。0 より下にはならない）。 */
    public static int afterHit(int hits, boolean decoy) {
        return decoy ? Math.max(0, hits - MISS_PENALTY) : hits + 1;
    }

    /** ご褒美の段階（自己ベストで決まる）。 */
    public enum Tier {
        NONE(0, 0xFFFFFF),
        BRONZE(15, 0xCD7F32),
        SILVER(30, 0xC0C0C0),
        GOLD(45, 0xFFD700),
        /** 色は巡る（描く側で時間から決める）。 */
        RAINBOW(60, 0xFFFFFF);

        /** この段階になる自己ベスト。 */
        public final int from;
        public final int rgb;

        Tier(int from, int rgb) {
            this.from = from;
            this.rgb = rgb;
        }

        public static Tier of(long best) {
            Tier[] all = values();
            for (int i = all.length - 1; i > 0; i--) {
                if (best >= all[i].from) {
                    return all[i];
                }
            }
            return NONE;
        }

        /** 次の段階（虹なら null）。 */
        public Tier next() {
            return this == RAINBOW ? null : values()[ordinal() + 1];
        }

        public boolean atLeast(Tier other) {
            return ordinal() >= other.ordinal();
        }
    }
}
