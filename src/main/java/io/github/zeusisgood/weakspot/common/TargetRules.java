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
    /** ハズレの位置を変える間隔（tick）。 */
    public static final int DECOY_MOVE_TICKS = 40;

    /**
     * 段階ごとの、弱点の半径（GUI のピクセル）・始めたときの向きを中心にした枠の半分の幅（左右・上下、度）・
     * 動きの速さ（度 / tick）・ハズレの数。
     */
    private static final double[] RADIUS = {16, 11, 8};
    private static final double[] WINDOW_YAW = {15, 25, 30};
    private static final double[] WINDOW_PITCH = {10, 15, 18};
    /** 次の的を、前の的から最低でも離す角度（度）。 */
    public static final double MIN_SEPARATION = 8;
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

    public static double windowYaw(int phase) {
        return WINDOW_YAW[clamp(phase)];
    }

    public static double windowPitch(int phase) {
        return WINDOW_PITCH[clamp(phase)];
    }

    /** 枠の上下の端が真上・真下を越えないように、基準の pitch を寄せる。 */
    public static double anchorPitch(double lookPitch) {
        double limit = 85 - WINDOW_PITCH[WINDOW_PITCH.length - 1];
        return Math.max(-limit, Math.min(limit, lookPitch));
    }

    /**
     * 枠の中を動く的の 1 tick 分（offset は基準からのずれ、half は枠の半分の幅）。端を越えたら跳ね返る。
     * 返すのは {新しいずれ, 新しい速さ}。
     */
    public static double[] bounce(double offset, double velocity, double half) {
        double next = offset + velocity;
        if (next > half) {
            return new double[] {2 * half - next, -Math.abs(velocity)};
        }
        if (next < -half) {
            return new double[] {-2 * half - next, Math.abs(velocity)};
        }
        return new double[] {next, velocity};
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
    /** ご褒美の段階（1.11.1 で 15・30・45・60 から 25・50・75・100 に上げた。1.11.0 で取った段階は進捗で残す）。 */
    public enum Tier {
        NONE(0, 0xFFFFFF),
        BRONZE(25, 0xCD7F32),
        SILVER(50, 0xC0C0C0),
        GOLD(75, 0xFFD700),
        /** 色は巡る（描く側で時間から決める）。 */
        RAINBOW(100, 0xFFFFFF);

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
