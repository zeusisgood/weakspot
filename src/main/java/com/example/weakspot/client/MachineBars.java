package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.network.QueryMessage;
import net.minecraft.util.math.BlockPos;

/**
 * 機械（かまど・醸造台・スポナー）の進み具合と燃料のバー（1.6.0）。値はサーバーだけが持つので、機械の弱点に照準を
 * 合わせている間、QUERY_INTERVAL_TICKS ごとと、ヒットのたびに問い合わせる。届いた値は、前の値から
 * LERP_TICKS かけてなめらかに進める（加速中は一気に進むので）。描き方は WorldBar。
 */
final class MachineBars {

    static final int QUERY_INTERVAL_TICKS = 4;
    private static final int LERP_TICKS = 4;
    /** この tick 以上返事がなければ、バーを消す。 */
    private static final int STALE_TICKS = 20;

    /** ブロックの上面からバーの中心までの高さ。 */
    private static final double HEIGHT = 0.25;
    /** 燃料のバーの太さ（進み具合のバーに対する割合）と、2 本の間。 */
    private static final double FUEL_THICKNESS_RATIO = 0.6;
    private static final double GAP = 0.02;
    /** 進み具合（黄 #FFD23F。成長バーと同じ）、燃料（炎の橙 #FF6A00）、背景（黒 #1E1E1E、半透明）。 */
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

    /** pos の機械の値を問い合わせる。間隔があいていなければ送らない（force ならすぐに送る）。 */
    static void query(BlockPos target, long tick, boolean force) {
        if (!QUERIES.due(target, tick, force)) {
            return;
        }
        WeakSpotMod.network.sendToServer(QueryMessage.machine(target));
    }

    /** サーバーの返事が届いた（クライアントのスレッドで呼ぶ）。fuel が負なら燃料のない機械。 */
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

    /** 前の値から、届いた値へ進める。減ったとき（焼き上がった、湧いた）は、すぐに切り替える。 */
    private static double shown(double from, double to, double now) {
        if (to < from) {
            return to;
        }
        double t = Math.max(0, Math.min(1, (now - receivedTick) / LERP_TICKS));
        return from + (to - from) * t;
    }

    /** target の機械のバーを描く。値が届いていない・古いときは描かない。 */
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
