package com.example.weakspot.common;

/**
 * 弱点の統計（プレイヤー1人分）。Minecraft に依存しない集計だけを持ち、保存と通信は呼び出し側が行う。
 */
public final class MiningStats {

    /** 採掘の弱点に当てた回数（サーバーが受け付けたもの）。 */
    public long hits;
    /** 弱点が出るブロック（壊せて、一瞬では壊れないもの）を壊した数。 */
    public long blocksBroken;
    /** そのうち、弱点に1回以上当てて壊した数。 */
    public long blocksBrokenWithHit;
    /** 1つのブロックで当てた回数の最大。 */
    public long maxHitsOnBlock;
    /** ヒットで得た追加進捗の合計（通常速度の tick 数）。短縮できた時間の推定に使う。 */
    public double savedTicks;
    /** 作物・苗木の弱点に当てた回数。 */
    public long growthHits;
    /** 機械の弱点に当てた回数。 */
    public long machineHits;

    /** 種類を問わない連続ヒット（コンボ）の最大。サーバーが HitStreak で数える。 */
    public long maxStreak;

    /** 連続ヒット数 count（ヒット後の数）を記録する。最大だけを残す。 */
    public void recordStreak(long count) {
        maxStreak = Math.max(maxStreak, count);
    }

    /** 動物の弱点に当てた回数。 */
    public long animalHits;

    public void recordAnimalHit() {
        animalHits++;
    }

    /** 釣りの弱点に当てた回数。 */
    public long fishingHits;

    public void recordFishingHit() {
        fishingHits++;
    }

    /** 弓の弱点に当てた回数。 */
    public long bowHits;

    public void recordBowHit() {
        bowHits++;
    }

    /** 近接の弱点に当てた回数（1.8.0。保存のキーは今までどおり critHits。1.7.x まではクリティカルにした回数）。 */
    public long critHits;

    public void recordCritHit() {
        critHits++;
    }

    /** 乗り物の弱点に当てた回数（1.6.0）。 */
    public long vehicleHits;

    public void recordVehicleHit() {
        vehicleHits++;
    }

    /** 食事・飲み物の弱点に当てた回数（1.6.0）。 */
    public long eatHits;

    public void recordEatHit() {
        eatHits++;
    }

    /** 寝ている間の弱点に当てた回数（1.6.0）。 */
    public long sleepHits;

    public void recordSleepHit() {
        sleepHits++;
    }

    /** はしご・エリトラ・エンチャント・収穫・投げる物・走りの弱点に当てた回数（1.7.0）。 */
    public long ladderHits;
    public long elytraHits;
    public long enchantHits;
    public long harvestHits;
    public long throwHits;
    public long sprintHits;
    /** ネザーゲートの弱点に当てた回数（1.8.0）。 */
    public long portalHits;

    /**
     * 採掘以外の種類のヒットを 1 つ数える（1.7.0）。近接はクリティカルにした回数。採掘は recordHit で数える
     * （短縮できた時間も足すため）。
     */
    public void recordKindHit(HitKind kind) {
        switch (kind) {
            case GROWTH: growthHits++; break;
            case MACHINE: machineHits++; break;
            case ANIMAL: animalHits++; break;
            case FISHING: fishingHits++; break;
            case BOW: bowHits++; break;
            case MELEE: critHits++; break;
            case VEHICLE: vehicleHits++; break;
            case EAT: eatHits++; break;
            case SLEEP: sleepHits++; break;
            case LADDER: ladderHits++; break;
            case ELYTRA: elytraHits++; break;
            case ENCHANT: enchantHits++; break;
            case HARVEST: harvestHits++; break;
            case THROW: throwHits++; break;
            case SPRINT: sprintHits++; break;
            case PORTAL: portalHits++; break;
            default: break;
        }
    }

    /** その種類のヒット数（採掘は hits、近接は critHits）。種類ごとの節目に使う（1.7.0）。 */
    public long count(HitKind kind) {
        switch (kind) {
            case MINING: return hits;
            case GROWTH: return growthHits;
            case MACHINE: return machineHits;
            case ANIMAL: return animalHits;
            case FISHING: return fishingHits;
            case BOW: return bowHits;
            case MELEE: return critHits;
            case VEHICLE: return vehicleHits;
            case EAT: return eatHits;
            case SLEEP: return sleepHits;
            case LADDER: return ladderHits;
            case ELYTRA: return elytraHits;
            case ENCHANT: return enchantHits;
            case HARVEST: return harvestHits;
            case THROW: return throwHits;
            case SPRINT: return sprintHits;
            case PORTAL: return portalHits;
            default: return 0;
        }
    }

    /** すべての種類のヒット数の合計。合計の節目に使う（1.7.0）。 */
    public long totalHits() {
        long sum = 0;
        for (HitKind kind : HitKind.values()) {
            sum += count(kind);
        }
        return sum;
    }

    public void recordHit(double extraTicks) {
        hits++;
        savedTicks += Math.max(0, extraTicks);
    }

    public void recordGrowthHit() {
        growthHits++;
    }

    public void recordMachineHit() {
        machineHits++;
    }

    public void recordBlockBroken(int hitsOnBlock) {
        blocksBroken++;
        if (hitsOnBlock > 0) {
            blocksBrokenWithHit++;
        }
        maxHitsOnBlock = Math.max(maxHitsOnBlock, hitsOnBlock);
    }

    /** 壊したブロック1つあたりの平均ヒット数。まだ1つも壊していなければ NaN。 */
    public double averageHitsPerBlock() {
        return blocksBroken == 0 ? Double.NaN : (double) hits / blocksBroken;
    }

    public double savedSeconds() {
        return savedTicks / 20.0;
    }

    public void reset() {
        hits = 0;
        blocksBroken = 0;
        blocksBrokenWithHit = 0;
        maxHitsOnBlock = 0;
        savedTicks = 0;
        growthHits = 0;
        machineHits = 0;
        maxStreak = 0;
        animalHits = 0;
        fishingHits = 0;
        bowHits = 0;
        critHits = 0;
        vehicleHits = 0;
        eatHits = 0;
        sleepHits = 0;
        ladderHits = 0;
        elytraHits = 0;
        enchantHits = 0;
        harvestHits = 0;
        throwHits = 0;
        sprintHits = 0;
        portalHits = 0;
    }
}
