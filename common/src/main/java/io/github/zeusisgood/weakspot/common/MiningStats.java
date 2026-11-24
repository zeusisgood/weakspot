package io.github.zeusisgood.weakspot.common;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/** 弱点の統計（プレイヤー 1 人分）。保存の形（save / load）と通信の形（writeTo / readFrom）はここ、置き場所は呼び出し側。 */
public final class MiningStats {

    /** サーバーが受け付けたもの。 */
    public long hits;
    /** 弱点が出るブロック（壊せて、一瞬では壊れないもの）だけを数える。 */
    public long blocksBroken;
    public long blocksBrokenWithHit;
    /** 壊しきらなかったブロックへのヒットは数えない。 */
    public long hitsOnBrokenBlocks;
    public long maxHitsOnBlock;
    /** 通常速度の tick。 */
    public double savedTicks;

    public long maxStreak;

    /** 「統計をリセット」では消さない。 */
    public long targetBest;
    public long targetRounds;

    /** 自己ベストを更新したら true。 */
    public boolean recordTargetRound(long hits) {
        targetRounds++;
        if (hits > targetBest) {
            targetBest = hits;
            return true;
        }
        return false;
    }

    /** count はヒット後の数。 */
    public void recordStreak(long count) {
        maxStreak = Math.max(maxStreak, count);
    }

    /** HitKind の番号で引く。採掘の欄は使わず、hits を使う。 */
    private final long[] kindHits = new long[HitKind.values().length];

    /** 採掘は recordHit で数える（短縮できた時間も足すため）。 */
    public void recordKindHit(HitKind kind) {
        if (kind != HitKind.MINING) {
            kindHits[kind.ordinal()]++;
        }
    }

    public long count(HitKind kind) {
        return kind == HitKind.MINING ? hits : kindHits[kind.ordinal()];
    }

    public void setCount(HitKind kind, long value) {
        if (kind == HitKind.MINING) {
            hits = value;
        } else {
            kindHits[kind.ordinal()] = value;
        }
    }

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

    public void recordBlockBroken(int hitsOnBlock) {
        blocksBroken++;
        if (hitsOnBlock > 0) {
            blocksBrokenWithHit++;
            hitsOnBrokenBlocks += hitsOnBlock;
        }
        maxHitsOnBlock = Math.max(maxHitsOnBlock, hitsOnBlock);
    }

    /** 壊したブロックだけで数える（最多ヒット数と同じ）。まだ 1 つも壊していなければ NaN。 */
    public double averageHitsPerBlock() {
        return blocksBroken == 0 ? Double.NaN : (double) hitsOnBrokenBlocks / blocksBroken;
    }

    public double savedSeconds() {
        return savedTicks / 20.0;
    }

    /** 近接は meleeHits（古いワールドは critHits）。 */
    public static String saveKey(HitKind kind) {
        return kind == HitKind.MINING ? "hits" : kind.key() + "Hits";
    }

    /** 新しいキーがないときだけ読む、古い版のキー。なければ null。 */
    public static String legacySaveKey(HitKind kind) {
        return kind == HitKind.MELEE ? "critHits" : null;
    }

    /** 保存先（各版の NBT）。ないキーの getLong / getDouble は 0。 */
    public interface Store {
        boolean has(String key);

        long getLong(String key);

        double getDouble(String key);

        void putLong(String key, long value);

        void putDouble(String key, double value);
    }

    /** キーの名前と型を変えると、古いワールドの統計が読めなくなる。 */
    public void save(Store out) {
        out.putLong("blocksBroken", blocksBroken);
        out.putLong("blocksBrokenWithHit", blocksBrokenWithHit);
        out.putLong("hitsOnBrokenBlocks", hitsOnBrokenBlocks);
        out.putLong("maxHitsOnBlock", maxHitsOnBlock);
        out.putDouble("savedTicks", savedTicks);
        out.putLong("maxStreak", maxStreak);
        out.putLong("targetBest", targetBest);
        out.putLong("targetRounds", targetRounds);
        for (HitKind kind : HitKind.values()) {
            out.putLong(saveKey(kind), count(kind));
        }
    }

    /** 古いワールドにないキー（hitsOnBrokenBlocks・新しい種類など）は 0 から数える。 */
    public static MiningStats load(Store in) {
        MiningStats stats = new MiningStats();
        stats.blocksBroken = in.getLong("blocksBroken");
        stats.blocksBrokenWithHit = in.getLong("blocksBrokenWithHit");
        stats.hitsOnBrokenBlocks = in.getLong("hitsOnBrokenBlocks");
        stats.maxHitsOnBlock = in.getLong("maxHitsOnBlock");
        stats.savedTicks = in.getDouble("savedTicks");
        stats.maxStreak = in.getLong("maxStreak");
        stats.targetBest = in.getLong("targetBest");
        stats.targetRounds = in.getLong("targetRounds");
        for (HitKind kind : HitKind.values()) {
            String key = saveKey(kind);
            String legacy = legacySaveKey(kind);
            if (!in.has(key) && legacy != null) {
                key = legacy;
            }
            stats.setCount(kind, in.getLong(key));
        }
        return stats;
    }

    /** 種類や項目を足すと通信が変わる（マイナー）。 */
    public void writeTo(DataOutput out) throws IOException {
        out.writeLong(hits);
        out.writeLong(blocksBroken);
        out.writeLong(blocksBrokenWithHit);
        out.writeLong(hitsOnBrokenBlocks);
        out.writeLong(maxHitsOnBlock);
        out.writeDouble(savedTicks);
        out.writeLong(maxStreak);
        for (HitKind kind : HitKind.values()) {
            if (kind != HitKind.MINING) {
                out.writeLong(kindHits[kind.ordinal()]);
            }
        }
        out.writeLong(targetBest);
        out.writeLong(targetRounds);
    }

    public static MiningStats readFrom(DataInput in) throws IOException {
        MiningStats stats = new MiningStats();
        stats.hits = in.readLong();
        stats.blocksBroken = in.readLong();
        stats.blocksBrokenWithHit = in.readLong();
        stats.hitsOnBrokenBlocks = in.readLong();
        stats.maxHitsOnBlock = in.readLong();
        stats.savedTicks = in.readDouble();
        stats.maxStreak = in.readLong();
        for (HitKind kind : HitKind.values()) {
            if (kind != HitKind.MINING) {
                stats.kindHits[kind.ordinal()] = in.readLong();
            }
        }
        stats.targetBest = in.readLong();
        stats.targetRounds = in.readLong();
        return stats;
    }
}
