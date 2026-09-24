package com.example.weakspot.common;

/** 弱点の一時オフ（J キー）のオン・オフの切り替え。 */
public final class WeakSpotSwitch {

    private WeakSpotSwitch() {
    }

    /** 切り替えた後の状態。 */
    public static boolean toggled(boolean on) {
        return !on;
    }
}
