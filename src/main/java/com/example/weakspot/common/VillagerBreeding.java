package com.example.weakspot.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 村人が繁殖できない理由（1.8.4）。バニラの EntityAIVillagerMate と同じ条件を、上から順に調べる。
 * 村が見つからないときは、ほかの条件は調べない（村を作るのが先）。
 */
public final class VillagerBreeding {

    /** 村人の数の上限の、ドアの数に対する割合（EntityAIVillagerMate#checkSufficientDoorsPresentForNewVillager）。 */
    public static final double DOOR_RATIO = 0.35;
    /** その気になれる食べ物の、1 つのスロットの個数（EntityVillager#getIsWillingToMate(true) が減らすもの）。 */
    public static final int BREAD = 3;
    public static final int POTATO_OR_CARROT = 12;
    /** 相手を探す範囲（横・縦）。 */
    public static final double MATE_RANGE = 8.0;
    public static final double MATE_RANGE_Y = 3.0;

    public enum Reason {
        NO_VILLAGE, NOT_SEASON, DOORS, COOLDOWN, FOOD, NO_MATE, MATE_CHILD, MATE_COOLDOWN, MATE_FOOD
    }

    /** 一番近い村人（相手）の状態。 */
    public enum Mate {
        NONE, CHILD, COOLDOWN, NO_FOOD, READY
    }

    private VillagerBreeding() {
    }

    /** ドアの数で、繁殖が止まる村人の数（これより少なければ増える）。バニラと同じく float を経て切り捨てる。 */
    public static int villagerLimit(int doors) {
        return (int) ((double) (float) doors * DOOR_RATIO);
    }

    public static boolean enoughDoors(int villagers, int doors) {
        return villagers < villagerLimit(doors);
    }

    /** あと何枚ドアがあれば増えるか。足りていれば 0。 */
    public static int doorsNeeded(int villagers, int doors) {
        int extra = 0;
        while (!enoughDoors(villagers, doors + extra)) {
            extra++;
        }
        return extra;
    }

    /** 1 つのスロットの一番多い個数から、その気になれる食べ物を持っているか。ビートルート・小麦は減らされないので数えない。 */
    public static boolean hasFood(int maxBread, int maxPotato, int maxCarrot) {
        return maxBread >= BREAD || maxPotato >= POTATO_OR_CARROT || maxCarrot >= POTATO_OR_CARROT;
    }

    /** 残りの tick を「m:ss」に（秒は切り上げ）。 */
    public static String clock(int ticks) {
        int seconds = (Math.max(0, ticks) + 19) / 20;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    /**
     * 満たしていない条件の一覧（空なら、そろっている）。
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
        return reasons;
    }

    /** 相手の状態を、子ども → 待ち時間 → 食べ物の順に決める。 */
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
