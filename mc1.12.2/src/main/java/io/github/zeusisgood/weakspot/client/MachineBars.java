package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.QueryThrottle;
import io.github.zeusisgood.weakspot.network.QueryMessage;
import net.minecraft.util.math.BlockPos;

/** 値はサーバーだけが持つので、問い合わせる。届いた値は、なめらかに進める（加速中は一気に進むので）。 */
final class MachineBars {

    static final int QUERY_INTERVAL_TICKS = 4;
    private static final int LERP_TICKS = 4;
    /** この tick 以上返事がなければ、バーを消す。 */
    private static final int STALE_TICKS = 20;

    /** ブロック。 */
    private static final double HEIGHT = 0.25;
    /** 進み具合のバーに対する割合。 */
    private static final double FUEL_THICKNESS_RATIO = 0.6;
    private static final double GAP = 0.02;
    /** 進み具合は成長バーと同じ黄。 */
    private static final float[] FILL = {0xFF / 255F, 0xD2 / 255F, 0x3F / 255F, 1.0F};
    private static final float[] FUEL = {0xFF / 255F, 0x6A / 255F, 0x00 / 255F, 1.0F};
    private static final float[] BACK = {0x1E / 255F, 0x1E / 255F, 0x1E / 255F, 0.5F};

    private static BlockPos pos;
    private static double progressFrom;
    private static double progressTo;
    private static double fuelFrom;
    private static double fuelTo;
    private static long receivedTick = Long.MIN_VALUE / 2;
    private static final QueryThrottle<BlockPos> QUERIES = new QueryThrottle<>(QUERY_INTERVAL_TICKS, false);

    private MachineBars() {
    }

    /** force ならすぐに送る。 */
    static void query(BlockPos target, long tick, boolean force) {
        if (!QUERIES.due(target, tick, force)) {
            return;
        }
        WeakSpotMod.network.sendToServer(QueryMessage.machine(target));
    }

    /** クライアントのスレッドで呼ぶ。fuel が負なら燃料のない機械。 */
    static void receive(BlockPos target, float progress, float fuel, long tick) {
        if (!target.equals(pos)) {
            pos = target;
            progressFrom = progress;
            fuelFrom = fuel;
        } else {
            progressFrom = shown(progressFrom, progressTo, tick);
            fuelFrom = shown(fuelFrom, fuelTo, tick);
        }
        progressTo = progress;
        fuelTo = fuel;
        receivedTick = tick;
    }

    /** 減ったとき（焼き上がった、湧いた）は、すぐに切り替える。 */
    private static double shown(double from, double to, double now) {
        if (to < from) {
            return to;
        }
        double t = Math.max(0, Math.min(1, (now - receivedTick) / LERP_TICKS));
        return from + (to - from) * t;
    }

    /** 値が届いていない・古いときは描かない。 */
    static void draw(BlockPos target, double now, double cx, double cy, double cz) {
        if (!target.equals(pos) || now - receivedTick > STALE_TICKS) {
            return;
        }
        double x = target.getX() + 0.5;
        double y = target.getY() + 1 + HEIGHT;
        double z = target.getZ() + 0.5;
        WorldBar.draw(x, y, z, GrowthBar.WIDTH, GrowthBar.THICKNESS, shown(progressFrom, progressTo, now),
                FILL, BACK, cx, cy, cz);
        if (fuelTo >= 0) {
            double thickness = GrowthBar.THICKNESS * FUEL_THICKNESS_RATIO;
            double fuelY = y - GrowthBar.THICKNESS / 2 - GAP - thickness / 2;
            WorldBar.draw(x, fuelY, z, GrowthBar.WIDTH, thickness, shown(fuelFrom, fuelTo, now), FUEL, BACK,
                    cx, cy, cz);
        }
    }

    static void clear() {
        pos = null;
        receivedTick = Long.MIN_VALUE / 2;
        QUERIES.reset();
    }
}
