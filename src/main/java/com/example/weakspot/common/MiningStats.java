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
     * 近接は、1.8.0 から「近接の弱点に当てた回数」（1.7.x まではクリティカルにした回数。保存のキーは今も critHits）。
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

    /**
     * その種類の保存のキー（PlayerPersisted の weakspot の中。1.8.6 までの名前のまま）。
     * 近接は critHits（中身は 1.8.0 から近接の弱点に当てた回数）。
     */
    public static String saveKey(HitKind kind) {
        if (kind == HitKind.MINING) {
            return "hits";
        }
        return kind == HitKind.MELEE ? "critHits" : kind.key() + "Hits";
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

    /**
     * 送る並び。1.8.5 までの手書きの並び（maxStreak が機械のあとに入る）と同じにして、通信の中身を変えない。
     * 種類を足すと通信が変わる（マイナー）。
     */
    public void writeTo(Writer out) {
        out.writeLong(hits);
        out.writeLong(blocksBroken);
        out.writeLong(blocksBrokenWithHit);
        out.writeLong(maxHitsOnBlock);
        out.writeDouble(savedTicks);
        for (HitKind kind : HitKind.values()) {
            if (kind == HitKind.MINING) {
                continue;
            }
            out.writeLong(kindHits[kind.ordinal()]);
            if (kind == HitKind.MACHINE) {
                out.writeLong(maxStreak);
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
        for (HitKind kind : HitKind.values()) {
            if (kind == HitKind.MINING) {
                continue;
            }
            stats.kindHits[kind.ordinal()] = in.readLong();
            if (kind == HitKind.MACHINE) {
                stats.maxStreak = in.readLong();
            }
        }
        return stats;
    }
}
