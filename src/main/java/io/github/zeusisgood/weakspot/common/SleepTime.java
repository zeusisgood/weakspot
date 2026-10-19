package io.github.zeusisgood.weakspot.common;

/** 時刻は 1 日 24000 tick。進めるのは、次の朝（次の日の 0 tick）まで。 */
public final class SleepTime {

    public static final int DAY_TICKS = 24000;
    /** バニラで、晴れていればベッドで寝られる時刻の始まりと終わり。 */
    public static final int NIGHT_START = 12541;
    public static final int NIGHT_END = 23460;

    private SleepTime() {
    }

    /** worldTime に負の値は来ない前提。 */
    public static boolean isNight(long worldTime) {
        long t = Math.floorMod(worldTime, DAY_TICKS);
        return t >= NIGHT_START && t < NIGHT_END;
    }

    public static long advance(long worldTime, int ticks) {
        long nextMorning = worldTime - Math.floorMod(worldTime, DAY_TICKS) + DAY_TICKS;
        return Math.min(worldTime + Math.max(0, ticks), nextMorning);
    }
}
