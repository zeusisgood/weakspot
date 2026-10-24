package io.github.zeusisgood.weakspot.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** 弱点のマーカーの見た目だけの位置（当たり判定は移動先）。時間はミリ秒の実時間、座標は面の (u, v)。 */
public final class MarkerMotion {

    public static final long MOVE_MS = 80;
    public static final long AFTERIMAGE_FADE_MS = 150;
    public static final int AFTERIMAGE_COUNT = 4;

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

    /** 動かさず、残像も出さない。 */
    public void jumpTo(double u, double v) {
        fromU = toU = u;
        fromV = toV = v;
        moving = false;
    }

    /** 移動の途中も残像も含めて全体をずらす（ボートが曲がったとき、弱点を一緒に回す）。 */
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

    /** 始点は前の移動先（判定上の位置）。移動中なら前の移動は打ち切るが、その残像はそのまま消えていく。 */
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
        // マーカーがそこを通る瞬間に生まれたことにする（古い残像ほど先に消える）
        for (int i = 0; i < AFTERIMAGE_COUNT; i++) {
            double fraction = (double) i / AFTERIMAGE_COUNT;
            afterimages.add(new Afterimage(fromU + (toU - fromU) * fraction, fromV + (toV - fromV) * fraction,
                    now + timeToReach(fraction)));
        }
    }

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

    public boolean isMoving(long now) {
        return moving && now - startMs < MOVE_MS;
    }

    /** 1（動き始め）〜0（止まった）。 */
    public double headHighlight(long now) {
        if (!moving || now - startMs >= MOVE_MS) {
            return 0;
        }
        return Math.max(0, 1 - (double) (now - startMs) / MOVE_MS);
    }

    /** まだ生まれていないものを除く。 */
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

    /** 0〜1。ease-out（3 次）。 */
    public static double progress(long elapsedMs) {
        double x = Math.max(0, Math.min(1, (double) elapsedMs / MOVE_MS));
        double rest = 1 - x;
        return 1 - rest * rest * rest;
    }

    /** progress の逆。 */
    static long timeToReach(double fraction) {
        return Math.round(MOVE_MS * (1 - Math.cbrt(1 - fraction)));
    }

    /** 1（生まれた直後）〜0。生まれる前も 0。 */
    public static double afterimageAlpha(long ageMs) {
        if (ageMs < 0) {
            return 0;
        }
        return Math.max(0, 1 - (double) ageMs / AFTERIMAGE_FADE_MS);
    }

    /** 前の位置と面またはブロックが違えば、その場で切り替える。 */
    public static boolean animates(boolean enabled, boolean hasPrevious, boolean sameSurface) {
        return enabled && hasPrevious && sameSurface;
    }
}
