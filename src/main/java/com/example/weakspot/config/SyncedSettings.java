package com.example.weakspot.config;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.GrowthFilters;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.server.EnchantHits;
import com.example.weakspot.server.PortalHits;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraftforge.fml.common.network.ByteBufUtils;

/**
 * サーバーの値が正の設定のうち、クライアントが使うもの。サーバーが自分の WeakSpotConfig から作って送り、
 * クライアントは接続中この値を使う（WeakSpotConfig の static フィールドには書き込まない。書き込むと
 * ConfigManager.sync でサーバーの値がクライアントの weakspot.cfg に保存されてしまうため）。
 *
 * 作った後は読むだけにする。項目を足すと通信内容が変わる（マイナーを上げる）。
 */
public final class SyncedSettings {

    /** 値を送ったサーバーの Mod の版（1.6.0。ServerFeatures）。自分の設定の値なら、自分の版。 */
    public String serverVersion;

    // 弱点の置き方（全種類で共通）
    public double weakSpotRadiusRatio;
    public double edgeMargin;
    public double minMoveDistance;
    public double weakSpotMinRadius;
    public double weakSpotMaxRadiusRatio;
    public double minFaceSize;
    public int lingerTicks;

    // 採掘（全体のオン・オフとコンボ倍率のオン・オフは 1.9.0）
    public boolean miningWeakSpotEnabled;
    public boolean miningComboBonus;
    public double boostMultiplier;
    public int boostDurationTicks;
    public int minHitIntervalTicks;

    // 成長
    public boolean growthWeakSpotEnabled;
    public int growthMinHitIntervalTicks;
    public double growthMinRadius;
    /** 登録名（"minecraft:grass" など）。 */
    public Set<String> growthExcludedBlocks;
    /** 成長の弱点の追加リスト。1行は「登録名」か「登録名[状態の条件]」（1.4.0。GrowthFilters）。 */
    public Set<String> growthExtraBlocks;
    /** growthExtraBlocks を読んだもの。最初に使うときに作る（作った後は読むだけなので、使い回してよい）。送らない。 */
    private GrowthFilters growthFilters;

    // 機械（倍率と上限は、HUD に実際の速さを出すため）
    public boolean machineWeakSpotEnabled;
    public int machineMinHitIntervalTicks;
    /** 機械の加速の対象外。登録名。 */
    public Set<String> excludedBlocks;
    public double machineBoostMultiplier;
    public double machineBoostMaxMultiplier;

    // 動物
    public boolean animalWeakSpotEnabled;
    public int animalMinHitIntervalTicks;
    public boolean animalBabyEnabled;
    public boolean animalBreedingEnabled;
    public boolean sheepWoolEnabled;
    public boolean chickenEggEnabled;
    public boolean villagerTradeResetEnabled;
    public int animalBabyHits;
    public int animalBreedingHits;
    public int sheepWoolHits;
    public int chickenEggHits;
    public int villagerTradeResetHits;
    /** 動物の弱点の対象外。エンティティの ID。 */
    public Set<String> animalExcludedEntities;
    /** しゃがみを条件にする MOD の動物。エンティティの ID。 */
    public Set<String> animalSneakRequiredEntities;

    // 釣り
    public boolean fishingWeakSpotEnabled;
    public int fishingMinHitIntervalTicks;
    public int fishingHits;

    // 弓
    public boolean bowWeakSpotEnabled;
    public int bowMinHitIntervalTicks;
    public int bowHitTicks;

    // 近接（溜め。1.8.0）
    public boolean meleeWeakSpotEnabled;
    public int meleeMinHitIntervalTicks;
    public double meleeChargePerHit;
    public double meleeChargeMax;

    // 乗り物
    public boolean vehicleWeakSpotEnabled;
    public int vehicleMinHitIntervalTicks;
    public double vehicleBoostMultiplier;
    public double vehicleBoostMaxMultiplier;
    public int vehicleBoostDurationTicks;

    // 飲食
    public boolean eatWeakSpotEnabled;
    public int eatMinHitIntervalTicks;
    public int eatHitTicks;

    // 睡眠（進める時刻はサーバーだけが使うので送らない）
    public boolean sleepWeakSpotEnabled;
    public int sleepMinHitIntervalTicks;

    // はしご（速さはクライアントで足すので、倍率・上限・時間も送る）
    public boolean ladderWeakSpotEnabled;
    public int ladderMinHitIntervalTicks;
    public double ladderBoostMultiplier;
    public double ladderBoostMaxMultiplier;
    public int ladderBoostDurationTicks;

    // エリトラ（速さはクライアントで足す）
    public boolean elytraWeakSpotEnabled;
    public int elytraMinHitIntervalTicks;
    public double elytraBoostPower;

    // エンチャント（サーバーが種を読み書きできないときは false で送る）
    public boolean enchantWeakSpotEnabled;
    public int enchantMinHitIntervalTicks;

    // 収穫（おまけの有無はサーバーだけが使うので送らない）
    public boolean harvestWeakSpotEnabled;
    public int harvestMinHitIntervalTicks;

    // 投擲物（溜めのゲージをクライアントも進める）
    public boolean throwWeakSpotEnabled;
    public int throwMinHitIntervalTicks;
    public double throwChargePerHit;

    // ダッシュ（残り時間のゲージのため、倍率・時間も送る）
    public boolean sprintWeakSpotEnabled;
    public int sprintMinHitIntervalTicks;
    public double sprintBoostMultiplier;
    public double sprintBoostMaxMultiplier;
    public int sprintBoostDurationTicks;

    // ネザーゲート（サーバーが待ち時間を読み書きできないときは false で送る）
    public boolean portalWeakSpotEnabled;
    public int portalMinHitIntervalTicks;
    public int portalHitTicks;

    // 他のプレイヤーの弱点マーク
    public double markerShareRange;
    public int markerSendMinIntervalTicks;

    private SyncedSettings() {
    }

    /** 成長の弱点の追加リストを読んだもの。 */
    public GrowthFilters growthFilters() {
        GrowthFilters filters = growthFilters;
        if (filters == null) {
            filters = GrowthFilters.parse(growthExtraBlocks);
            growthFilters = filters;
        }
        return filters;
    }

    /**
     * 送る項目と並び（1.8.6 で一覧にした。1.9.0 で、フィールドと同じ種類ごとの素直な並びにした）。
     * 項目を足すときは、フィールドとこの一覧の両方に足す（通信が変わるので、マイナー）。フィールドの名前が
     * WeakSpotConfig の同じ名前の項目から値を取る（serverVersion は自分の版）。
     */
    static final String[] WIRE = {
            "serverVersion", "weakSpotRadiusRatio", "edgeMargin", "minMoveDistance", "weakSpotMinRadius",
            "weakSpotMaxRadiusRatio", "minFaceSize", "lingerTicks", "miningWeakSpotEnabled", "miningComboBonus",
            "boostMultiplier", "boostDurationTicks", "minHitIntervalTicks", "growthWeakSpotEnabled",
            "growthMinHitIntervalTicks", "growthMinRadius", "growthExcludedBlocks", "growthExtraBlocks",
            "machineWeakSpotEnabled", "machineMinHitIntervalTicks", "excludedBlocks", "machineBoostMultiplier",
            "machineBoostMaxMultiplier", "animalWeakSpotEnabled", "animalMinHitIntervalTicks", "animalBabyEnabled",
            "animalBreedingEnabled", "sheepWoolEnabled", "chickenEggEnabled", "villagerTradeResetEnabled",
            "animalBabyHits", "animalBreedingHits", "sheepWoolHits", "chickenEggHits", "villagerTradeResetHits",
            "animalExcludedEntities", "animalSneakRequiredEntities", "fishingWeakSpotEnabled",
            "fishingMinHitIntervalTicks", "fishingHits", "bowWeakSpotEnabled", "bowMinHitIntervalTicks",
            "bowHitTicks", "meleeWeakSpotEnabled", "meleeMinHitIntervalTicks", "meleeChargePerHit",
            "meleeChargeMax", "vehicleWeakSpotEnabled", "vehicleMinHitIntervalTicks", "vehicleBoostMultiplier",
            "vehicleBoostMaxMultiplier", "vehicleBoostDurationTicks", "eatWeakSpotEnabled",
            "eatMinHitIntervalTicks", "eatHitTicks", "sleepWeakSpotEnabled", "sleepMinHitIntervalTicks",
            "ladderWeakSpotEnabled", "ladderMinHitIntervalTicks", "ladderBoostMultiplier",
            "ladderBoostMaxMultiplier", "ladderBoostDurationTicks", "elytraWeakSpotEnabled",
            "elytraMinHitIntervalTicks", "elytraBoostPower", "enchantWeakSpotEnabled", "enchantMinHitIntervalTicks",
            "harvestWeakSpotEnabled", "harvestMinHitIntervalTicks", "throwWeakSpotEnabled",
            "throwMinHitIntervalTicks", "throwChargePerHit", "sprintWeakSpotEnabled", "sprintMinHitIntervalTicks",
            "sprintBoostMultiplier", "sprintBoostMaxMultiplier", "sprintBoostDurationTicks",
            "portalWeakSpotEnabled", "portalMinHitIntervalTicks", "portalHitTicks", "markerShareRange",
            "markerSendMinIntervalTicks"
    };
    private static final Field[] FIELDS = new Field[WIRE.length];

    static {
        try {
            for (int i = 0; i < WIRE.length; i++) {
                FIELDS[i] = SyncedSettings.class.getField(WIRE[i]);
            }
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 種類ごとのオン・オフと、ヒットの最小間隔の表（1.8.9）。フィールドの名前は、オン・オフが
     * "<key>WeakSpotEnabled"（ない種類 = 採掘・成長・機械・動物は、いつでもオン）、間隔が "<key>MinHitIntervalTicks"
     * （採掘だけ "minHitIntervalTicks"）。クライアントもサーバーも、この表から引く。
     */
    private static final Map<HitKind, Field> ENABLED = new EnumMap<>(HitKind.class);
    private static final Map<HitKind, Field> INTERVAL = new EnumMap<>(HitKind.class);

    static {
        for (HitKind kind : HitKind.values()) {
            try {
                ENABLED.put(kind, SyncedSettings.class.getField(kind.key() + "WeakSpotEnabled"));
            } catch (NoSuchFieldException e) {
                // この種類には、サーバーの全体のオン・オフがない
            }
            String interval = kind == HitKind.MINING ? "minHitIntervalTicks" : kind.key() + "MinHitIntervalTicks";
            try {
                INTERVAL.put(kind, SyncedSettings.class.getField(interval));
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException("no " + interval, e);
            }
        }
    }

    /** サーバーの設定で、その種類の弱点を出すか（読み書きできない種類は false。fromConfig の最後を見る）。 */
    public boolean enabled(HitKind kind) {
        Field field = ENABLED.get(kind);
        try {
            return field == null || field.getBoolean(this);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /** その種類のヒットの最小間隔（tick）。 */
    public int minHitInterval(HitKind kind) {
        try {
            return INTERVAL.get(kind).getInt(this);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /** サーバーが使う自分の値（1.8.9。作り直すのは invalidateServer のあと。ヒットのたびに作り直さないため）。 */
    private static volatile SyncedSettings server;

    /** この側の weakspot.cfg の値を、使い回す。読むだけにすること。 */
    public static SyncedSettings server() {
        SyncedSettings s = server;
        if (s == null) {
            s = fromConfig();
            server = s;
        }
        return s;
    }

    /** 設定が変わった（起動・/weakspot reload・設定画面）。次の server() で作り直す。 */
    public static void invalidateServer() {
        server = null;
    }

    /** この側の weakspot.cfg の値（毎回作る）。 */
    public static SyncedSettings fromConfig() {
        SyncedSettings s = new SyncedSettings();
        try {
            for (Field field : FIELDS) {
                if (field.getName().equals("serverVersion")) {
                    continue;
                }
                Object value = WeakSpotConfig.class.getField(field.getName()).get(null);
                field.set(s, value instanceof String[] ? new HashSet<>(Arrays.asList((String[]) value)) : value);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        s.serverVersion = WeakSpotMod.VERSION;
        // 読み書きできないときは、クライアントに弱点を出させない
        s.portalWeakSpotEnabled &= PortalHits.isAvailable();
        s.enchantWeakSpotEnabled &= EnchantHits.isAvailable();
        return s;
    }

    public void write(ByteBuf buf) {
        try {
            for (Field field : FIELDS) {
                Class<?> type = field.getType();
                if (type == double.class) {
                    buf.writeDouble(field.getDouble(this));
                } else if (type == int.class) {
                    buf.writeInt(field.getInt(this));
                } else if (type == boolean.class) {
                    buf.writeBoolean(field.getBoolean(this));
                } else if (type == String.class) {
                    ByteBufUtils.writeUTF8String(buf, (String) field.get(this));
                } else {
                    @SuppressWarnings("unchecked")
                    Set<String> strings = (Set<String>) field.get(this);
                    writeStrings(buf, strings);
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public static SyncedSettings read(ByteBuf buf) {
        SyncedSettings s = new SyncedSettings();
        try {
            for (Field field : FIELDS) {
                Class<?> type = field.getType();
                if (type == double.class) {
                    field.setDouble(s, buf.readDouble());
                } else if (type == int.class) {
                    field.setInt(s, buf.readInt());
                } else if (type == boolean.class) {
                    field.setBoolean(s, buf.readBoolean());
                } else if (type == String.class) {
                    field.set(s, ByteBufUtils.readUTF8String(buf));
                } else {
                    field.set(s, readStrings(buf));
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        return s;
    }

    private static void writeStrings(ByteBuf buf, Set<String> strings) {
        buf.writeInt(strings.size());
        for (String string : strings) {
            ByteBufUtils.writeUTF8String(buf, string);
        }
    }

    private static Set<String> readStrings(ByteBuf buf) {
        int size = buf.readInt();
        Set<String> strings = new HashSet<>();
        for (int i = 0; i < size; i++) {
            strings.add(ByteBufUtils.readUTF8String(buf));
        }
        return strings;
    }
}
