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

    /** 種類を問わない連続ヒット（コンボ）の最大。サーバーが HitStreak で数える。 */
    public long maxStreak;

    /** 連続ヒット数 count（ヒット後の数）を記録する。最大だけを残す。 */
    public void recordStreak(long count) {
        maxStreak = Math.max(maxStreak, count);
    }

    /**
     * 採掘以外の種類ごとの、弱点に当てた回数（HitKind の番号で引く。採掘の欄は使わず、hits を使う。1.8.6 で配列にした）。
     * 近接は、1.8.0 から「近接の弱点に当てた回数」（1.7.x まではクリティカルにした回数。保存のキーは 1.9.0 から meleeHits）。
     */
    private final long[] kindHits = new long[HitKind.values().length];

    /** 採掘以外の種類のヒットを 1 つ数える（1.7.0）。採掘は recordHit で数える（短縮できた時間も足すため）。 */
    public void recordKindHit(HitKind kind) {
        if (kind != HitKind.MINING) {
            kindHits[kind.ordinal()]++;
        }
    }

    /** その種類のヒット数（採掘は hits）。種類ごとの節目に使う（1.7.0）。 */
    public long count(HitKind kind) {
        return kind == HitKind.MINING ? hits : kindHits[kind.ordinal()];
    }

    /** 読み込み用。 */
    public void setCount(HitKind kind, long value) {
        if (kind == HitKind.MINING) {
            hits = value;
        } else {
            kindHits[kind.ordinal()] = value;
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
        maxStreak = 0;
        java.util.Arrays.fill(kindHits, 0);
    }

    /** 保存のキー（採掘は hits、ほかは "<key>Hits"。近接は 1.9.0 から meleeHits で、1.8.x までは critHits）。 */
    public static String saveKey(HitKind kind) {
        return kind == HitKind.MINING ? "hits" : kind.key() + "Hits";
    }

    /** 1.8.x までの保存のキー（読み込みで、新しいキーがないときだけ読む）。なければ null。 */
    public static String legacySaveKey(HitKind kind) {
        return kind == HitKind.MELEE ? "critHits" : null;
    }

    /** 送るときの書き先（StatsMessage が ByteBuf につなぐ）。 */
    public interface Writer {
        void writeLong(long value);

        void writeDouble(double value);
    }

    /** 受け取るときの読み元。 */
    public interface Reader {
        long readLong();

        double readDouble();
    }

    /** 送る並び（1.9.0 から、全体の数のあとに種類の順）。種類を足すと通信が変わる（マイナー）。 */
    public void writeTo(Writer out) {
        out.writeLong(hits);
        out.writeLong(blocksBroken);
        out.writeLong(blocksBrokenWithHit);
        out.writeLong(maxHitsOnBlock);
        out.writeDouble(savedTicks);
        out.writeLong(maxStreak);
        for (HitKind kind : HitKind.values()) {
            if (kind != HitKind.MINING) {
                out.writeLong(kindHits[kind.ordinal()]);
            }
        }
    }

    public static MiningStats readFrom(Reader in) {
        MiningStats stats = new MiningStats();
        stats.hits = in.readLong();
        stats.blocksBroken = in.readLong();
        stats.blocksBrokenWithHit = in.readLong();
        stats.maxHitsOnBlock = in.readLong();
        stats.savedTicks = in.readDouble();
        stats.maxStreak = in.readLong();
        for (HitKind kind : HitKind.values()) {
            if (kind != HitKind.MINING) {
                stats.kindHits[kind.ordinal()] = in.readLong();
            }
        }
        return stats;
    }
}
