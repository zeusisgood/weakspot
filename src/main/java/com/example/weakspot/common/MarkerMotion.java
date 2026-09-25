package com.example.weakspot.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 弱点のマーカーの表示位置（見た目だけ）。当たり判定の位置（移動先）とは別に持ち、古い位置から移動先へ
 * MOVE_MS かけて ease-out で動かし、通った道に残像を残す。時間は実時間（ミリ秒）。座標は面の (u, v)。
 */
public final class MarkerMotion {

    /** 古い位置から移動先まで動く時間。 */
    public static final long MOVE_MS = 80;
    /** 残像が透明になって消えるまでの時間。 */
    public static final long AFTERIMAGE_FADE_MS = 150;
    /** 1回の移動で残す残像の数。 */
    public static final int AFTERIMAGE_COUNT = 4;

    /** 残像。生まれてから AFTERIMAGE_FADE_MS で消える。 */
    public static final class Afterimage {
        public final double u;
        public final double v;
        public final long bornMs;

        Afterimage(double u, double v, long bornMs) {
            this.u = u;
            this.v = v;
            this.bornMs = bornMs;
        }

        public double alpha(long now) {
            return afterimageAlpha(now - bornMs);
        }
    }

    private double fromU;
    private double fromV;
    private double toU;
    private double toV;
    private long startMs;
    private boolean moving;
    private final List<Afterimage> afterimages = new ArrayList<>();

    public MarkerMotion(double u, double v) {
        jumpTo(u, v);
    }

    /** その場で切り替える（動かさず、残像も出さない）。 */
    public void jumpTo(double u, double v) {
        fromU = toU = u;
        fromV = toV = v;
        moving = false;
    }

    /**
     * 移動の途中も残像も含めて、全体を (du, dv) だけずらす（1.8.2。ボートが曲がったとき、弱点を一緒に回す）。
     */
    public void shift(double du, double dv) {
        fromU += du;
        fromV += dv;
        toU += du;
        toV += dv;
        for (int i = 0; i < afterimages.size(); i++) {
            Afterimage image = afterimages.get(i);
            afterimages.set(i, new Afterimage(image.u + du, image.v + dv, image.bornMs));
        }
    }

    /**
     * 移動先 (u, v) へ動かし始める。始点は前の移動先（判定上の位置）。移動中なら前の移動は打ち切るが、
     * 前の移動の残像はそのまま消えていく。移動先が今と同じなら何もしない。
     */
    public void moveTo(double u, double v, long now) {
        if (u == toU && v == toV) {
            return;
        }
        fromU = toU;
        fromV = toV;
        toU = u;
        toV = v;
        startMs = now;
        moving = true;
        // 始点から移動先の手前まで等間隔に置き、マーカーがそこを通る瞬間に生まれたことにする（古い残像ほど先に消える）
        for (int i = 0; i < AFTERIMAGE_COUNT; i++) {
            double fraction = (double) i / AFTERIMAGE_COUNT;
            afterimages.add(new Afterimage(fromU + (toU - fromU) * fraction, fromV + (toV - fromV) * fraction,
                    now + timeToReach(fraction)));
        }
    }

    /** 今の表示位置 {u, v}。 */
    public double[] position(long now) {
        if (moving && now - startMs >= MOVE_MS) {
            moving = false;
        }
        if (!moving) {
            return new double[] {toU, toV};
        }
        double p = progress(now - startMs);
        return new double[] {fromU + (toU - fromU) * p, fromV + (toV - fromV) * p};
    }

    /** 動いている最中のマーカーの先頭を明るくする濃さ（1〜0。動き始めが 1、止まると 0）。 */
    public double headHighlight(long now) {
        if (!moving || now - startMs >= MOVE_MS) {
            return 0;
        }
        return Math.max(0, 1 - (double) (now - startMs) / MOVE_MS);
    }

    /** 見えている残像（まだ生まれていないものを除く）。消えたものは捨てる。 */
    public List<Afterimage> afterimages(long now) {
        List<Afterimage> visible = new ArrayList<>();
        for (Iterator<Afterimage> it = afterimages.iterator(); it.hasNext(); ) {
            Afterimage a = it.next();
            if (now - a.bornMs >= AFTERIMAGE_FADE_MS) {
                it.remove();
            } else if (now >= a.bornMs) {
                visible.add(a);
            }
        }
        return visible;
    }

    /** 移動の進み具合（0〜1）。ease-out（3次）で、前半でほとんどの距離を動く。 */
    public static double progress(long elapsedMs) {
        double x = Math.max(0, Math.min(1, (double) elapsedMs / MOVE_MS));
        double rest = 1 - x;
        return 1 - rest * rest * rest;
    }

    /** 進み具合が fraction になるまでの時間（progress の逆）。 */
    static long timeToReach(double fraction) {
        return Math.round(MOVE_MS * (1 - Math.cbrt(1 - fraction)));
    }

    /** 残像の濃さ。生まれた直後が 1 で、AFTERIMAGE_FADE_MS で 0。生まれる前も 0。 */
    public static double afterimageAlpha(long ageMs) {
        if (ageMs < 0) {
            return 0;
        }
        return Math.max(0, 1 - (double) ageMs / AFTERIMAGE_FADE_MS);
    }

    /**
     * 動かすか、その場で切り替えるか。前の位置がないとき、前の位置と面またはブロックが違うとき、演出がオフのときは、
     * その場で切り替える。
     */
    public static boolean animates(boolean enabled, boolean hasPrevious, boolean sameSurface) {
        return enabled && hasPrevious && sameSurface;
    }
}
