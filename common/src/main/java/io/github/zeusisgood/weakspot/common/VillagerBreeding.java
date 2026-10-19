package io.github.zeusisgood.weakspot.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * reasons は 1.12 のバニラ（EntityAIVillagerMate）と同じ条件。村が見つからないときは、ほかの条件は調べない（村を作るのが先）。
 * reasonsWithBeds は 1.14 以降（村と扉の代わりに、空いているベッドが要る）。
 */
public final class VillagerBreeding {

    /** EntityAIVillagerMate#checkSufficientDoorsPresentForNewVillager。 */
    public static final double DOOR_RATIO = 0.35;
    /** 1 つのスロットの個数（EntityVillager#getIsWillingToMate(true) が減らすもの）。 */
    public static final int BREAD = 3;
    public static final int POTATO_OR_CARROT = 12;
    public static final double MATE_RANGE = 8.0;
    public static final double MATE_RANGE_Y = 3.0;
    /** 1.14 以降の Villager#canBreed（パン 4・ほかの作物 1 の合計）。 */
    public static final int FOOD_POINTS = 12;

    public enum Reason {
        NO_VILLAGE, NOT_SEASON, DOORS, COOLDOWN, FOOD, NO_MATE, MATE_CHILD, MATE_COOLDOWN, MATE_FOOD, NO_BED
    }

    /** 一番近い村人。 */
    public enum Mate {
        NONE, CHILD, COOLDOWN, NO_FOOD, READY
    }

    private VillagerBreeding() {
    }

    /** これより少なければ増える。バニラと同じく float を経て切り捨てる。 */
    public static int villagerLimit(int doors) {
        return (int) ((double) (float) doors * DOOR_RATIO);
    }

    public static boolean enoughDoors(int villagers, int doors) {
        return villagers < villagerLimit(doors);
    }

    public static int doorsNeeded(int villagers, int doors) {
        int extra = 0;
        while (!enoughDoors(villagers, doors + extra)) {
            extra++;
        }
        return extra;
    }

    /** ビートルート・小麦は減らされないので数えない。 */
    public static boolean hasFood(int maxBread, int maxPotato, int maxCarrot) {
        return maxBread >= BREAD || maxPotato >= POTATO_OR_CARROT || maxCarrot >= POTATO_OR_CARROT;
    }

    /** 「m:ss」。秒は切り上げ。 */
    public static String clock(int ticks) {
        int seconds = (Math.max(0, ticks) + 19) / 20;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    /**
    * 空なら、そろっている。
    *
    * @param ready その気か、その気になれる食べ物を持っている
    */
    public static List<Reason> reasons(boolean inVillage, boolean matingSeason, int villagers, int doors,
            boolean cooldown, boolean ready, Mate mate) {
        List<Reason> reasons = new ArrayList<>();
        if (!inVillage) {
            reasons.add(Reason.NO_VILLAGE);
            return reasons;
        }
        if (!matingSeason) {
            reasons.add(Reason.NOT_SEASON);
        }
        if (!enoughDoors(villagers, doors)) {
            reasons.add(Reason.DOORS);
        }
        if (cooldown) {
            reasons.add(Reason.COOLDOWN);
        }
        if (!ready) {
            reasons.add(Reason.FOOD);
        }
        addMate(reasons, mate);
        return reasons;
    }

    private static void addMate(List<Reason> reasons, Mate mate) {
        switch (mate) {
            case NONE:
                reasons.add(Reason.NO_MATE);
                break;
            case CHILD:
                reasons.add(Reason.MATE_CHILD);
                break;
            case COOLDOWN:
                reasons.add(Reason.MATE_COOLDOWN);
                break;
            case NO_FOOD:
                reasons.add(Reason.MATE_FOOD);
                break;
            default:
                break;
        }
    }

    /** 空なら、そろっている。 */
    public static List<Reason> reasonsWithBeds(boolean freeBed, boolean cooldown, boolean ready, Mate mate) {
        List<Reason> reasons = new ArrayList<>();
        if (!freeBed) {
            reasons.add(Reason.NO_BED);
        }
        if (cooldown) {
            reasons.add(Reason.COOLDOWN);
        }
        if (!ready) {
            reasons.add(Reason.FOOD);
        }
        addMate(reasons, mate);
        return reasons;
    }

    public static boolean hasFoodPoints(int points) {
        return points >= FOOD_POINTS;
    }

    public static Mate mate(boolean exists, boolean child, boolean cooldown, boolean ready) {
        if (!exists) {
            return Mate.NONE;
        }
        if (child) {
            return Mate.CHILD;
        }
        if (cooldown) {
            return Mate.COOLDOWN;
        }
        return ready ? Mate.READY : Mate.NO_FOOD;
    }
}
