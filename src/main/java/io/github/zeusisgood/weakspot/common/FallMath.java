package io.github.zeusisgood.weakspot.common;

/** 落下のダメージの見込み（1.11.0。落下の弱点の表示）。バニラの EntityLivingBase#fall と同じ式（防具のエンチャントは数えない）。 */
public final class FallMath {

    /** これ以下の落下ではダメージを受けない（ブロック）。 */
    public static final double SAFE_DISTANCE = 3.0;

    private FallMath() {
    }

    /** 落ちた距離と跳躍力上昇のレベル（なければ 0）から、ダメージ（体力の点。ハート 1 つが 2 点）。 */
    public static int damagePoints(double fallDistance, int jumpBoostLevel) {
        return Math.max(0, (int) Math.ceil(fallDistance - SAFE_DISTANCE - jumpBoostLevel));
    }

    /** ハートの数の表示（2 点で 1。例 4.5）。 */
    public static String heartsLabel(int points) {
        return points % 2 == 0 ? Integer.toString(points / 2) : (points / 2) + ".5";
    }
}
