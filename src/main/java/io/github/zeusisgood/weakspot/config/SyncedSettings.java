package io.github.zeusisgood.weakspot.config;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.GrowthFilters;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.server.EnchantHits;
import io.github.zeusisgood.weakspot.server.PortalHits;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraftforge.fml.common.network.ByteBufUtils;

/**
* クライアントは接続中この値を使う（WeakSpotConfig のフィールドには書き込まない。書き込むと
* ConfigManager.sync でサーバーの値がクライアントの weakspot.cfg に保存されてしまうため）。
* 作った後は読むだけにする。項目を足すと通信内容が変わる（マイナーを上げる）。
*/
public final class SyncedSettings {

    /** ServerFeatures が使う。自分の設定の値なら、自分の版。 */
    public String serverVersion;

    public double weakSpotRadiusRatio;
    public double edgeMargin;
    public double minMoveDistance;
    public double weakSpotMinRadius;
    public double weakSpotMaxRadiusRatio;
    public double minFaceSize;
    public int lingerTicks;
    /** 種類ごとの決まった倍率 HitKind.sizeFactor を掛けて使う。 */
    public double spotSize;
    public boolean heldItemsCountAsEmptyHand;
    /** 登録名。 */
    public Set<String> heldItemExcludes;

    public boolean miningWeakSpotEnabled;
    public boolean miningComboBonus;
    public double boostMultiplier;
    public int boostDurationTicks;
    public int minHitIntervalTicks;

    public boolean growthWeakSpotEnabled;
    public int growthMinHitIntervalTicks;
    public double growthMinRadius;
    /** 登録名。 */
    public Set<String> growthExcludedBlocks;
    public Set<String> growthExtraBlocks;
    /** 最初に使うときに作る（作った後は読むだけなので、使い回してよい）。送らない。 */
    private GrowthFilters growthFilters;

    // 機械の倍率と上限は、HUD に実際の速さを出すため
    public boolean machineWeakSpotEnabled;
    public int machineMinHitIntervalTicks;
    /** 登録名。 */
    public Set<String> excludedBlocks;
    public double machineBoostMultiplier;
    public double machineBoostMaxMultiplier;

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
    /** エンティティの ID。 */
    public Set<String> animalExcludedEntities;
    /** エンティティの ID。 */
    public Set<String> animalSneakRequiredEntities;

    public boolean fishingWeakSpotEnabled;
    public int fishingMinHitIntervalTicks;
    public int fishingHits;

    public boolean bowWeakSpotEnabled;
    public int bowMinHitIntervalTicks;
    public int bowHitTicks;

    public boolean meleeWeakSpotEnabled;
    public int meleeMinHitIntervalTicks;
    public double meleeChargePerHit;
    public double meleeChargeMax;

    public boolean vehicleWeakSpotEnabled;
    public int vehicleMinHitIntervalTicks;
    public double vehicleBoostMultiplier;
    public double vehicleBoostMaxMultiplier;
    public int vehicleBoostDurationTicks;

    public boolean eatWeakSpotEnabled;
    public int eatMinHitIntervalTicks;
    public int eatHitTicks;

    // 睡眠の進める時刻はサーバーだけが使うので送らない
    public boolean sleepWeakSpotEnabled;
    public int sleepMinHitIntervalTicks;

    // はしごの速さはクライアントで足すので、倍率・上限・時間も送る
    public boolean ladderWeakSpotEnabled;
    public int ladderMinHitIntervalTicks;
    public double ladderBoostMultiplier;
    public double ladderBoostMaxMultiplier;
    public int ladderBoostDurationTicks;

    // エリトラの速さはクライアントで足す
    public boolean elytraWeakSpotEnabled;
    public int elytraMinHitIntervalTicks;
    public double elytraBoostPower;

    // サーバーが種を読み書きできないときは false で送る
    public boolean enchantWeakSpotEnabled;
    public int enchantMinHitIntervalTicks;

    // 収穫のおまけの有無はサーバーだけが使うので送らない
    public boolean harvestWeakSpotEnabled;
    public int harvestMinHitIntervalTicks;

    // 投擲物の溜めのゲージはクライアントも進める
    public boolean throwWeakSpotEnabled;
    public int throwMinHitIntervalTicks;
    public double throwChargePerHit;

    // ダッシュの残り時間のゲージのため、倍率・時間も送る
    public boolean sprintWeakSpotEnabled;
    public int sprintMinHitIntervalTicks;
    public double sprintBoostMultiplier;
    public double sprintBoostMaxMultiplier;
    public int sprintBoostDurationTicks;

    // サーバーが待ち時間を読み書きできないときは false で送る
    public boolean portalWeakSpotEnabled;
    public int portalMinHitIntervalTicks;
    public int portalHitTicks;

    // 泳ぎの速さはクライアントで足す。残り時間のゲージのため、倍率・時間も送る
    public boolean swimWeakSpotEnabled;
    public int swimMinHitIntervalTicks;
    public double swimBoostMultiplier;
    public double swimBoostMaxMultiplier;
    public int swimBoostDurationTicks;
    public double swimDashDistance;

    // 落ちた距離を減らすのはサーバー。クライアントは出し始める距離と、見込みのダメージの表示に使う
    public boolean fallWeakSpotEnabled;
    public int fallMinHitIntervalTicks;
    public double fallMinDistance;
    public double fallReduceBlocks;

    public double markerShareRange;
    public int markerSendMinIntervalTicks;

    private SyncedSettings() {
    }

    public double spotSize(HitKind kind) {
        return (spotSize > 0 ? spotSize : 1) * kind.sizeFactor();
    }

    public GrowthFilters growthFilters() {
        GrowthFilters filters = growthFilters;
        if (filters == null) {
            filters = GrowthFilters.parse(growthExtraBlocks);
            growthFilters = filters;
        }
        return filters;
    }

    /**
    * 項目を足すときは、フィールドとこの一覧の両方に足す（通信が変わるので、マイナー）。フィールドの名前が
    * WeakSpotConfig の同じ名前の項目から値を取る（serverVersion は自分の版）。
    */
    static final String[] WIRE = {
            "serverVersion", "weakSpotRadiusRatio", "edgeMargin", "minMoveDistance", "weakSpotMinRadius",
            "weakSpotMaxRadiusRatio", "minFaceSize", "lingerTicks", "spotSize", "heldItemsCountAsEmptyHand",
            "heldItemExcludes", "miningWeakSpotEnabled",
            "miningComboBonus",
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
            "portalWeakSpotEnabled", "portalMinHitIntervalTicks", "portalHitTicks", "swimWeakSpotEnabled",
            "swimMinHitIntervalTicks", "swimBoostMultiplier", "swimBoostMaxMultiplier", "swimBoostDurationTicks",
            "swimDashDistance", "fallWeakSpotEnabled", "fallMinHitIntervalTicks", "fallMinDistance",
            "fallReduceBlocks", "markerShareRange",
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
    * フィールドの名前は、オン・オフが "<key>WeakSpotEnabled"、間隔が "<key>MinHitIntervalTicks"（採掘だけ "minHitIntervalTicks"）。
    * どちらかがない種類があれば、このクラスを読み込んだ時点で例外にする（種類を足して項目を忘れたときに気づくため）。
    */
    private static final Map<HitKind, Field> ENABLED = new EnumMap<>(HitKind.class);
    private static final Map<HitKind, Field> INTERVAL = new EnumMap<>(HitKind.class);

    static {
        for (HitKind kind : HitKind.values()) {
            String enabled = kind.key() + "WeakSpotEnabled";
            try {
                ENABLED.put(kind, SyncedSettings.class.getField(enabled));
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException("no " + enabled, e);
            }
            String interval = kind == HitKind.MINING ? "minHitIntervalTicks" : kind.key() + "MinHitIntervalTicks";
            try {
                INTERVAL.put(kind, SyncedSettings.class.getField(interval));
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException("no " + interval, e);
            }
        }
    }

    /** 読み書きできない種類は false（fromConfig の最後を見る）。 */
    public boolean enabled(HitKind kind) {
        Field field = ENABLED.get(kind);
        try {
            return field.getBoolean(this);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /** tick。 */
    public int minHitInterval(HitKind kind) {
        try {
            return INTERVAL.get(kind).getInt(this);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /** ヒットのたびに作り直さないため、invalidateServer のあとだけ作り直す。 */
    private static volatile SyncedSettings server;

    /** 読むだけにすること。 */
    public static SyncedSettings server() {
        SyncedSettings s = server;
        if (s == null) {
            s = fromConfig();
            server = s;
        }
        return s;
    }

    /** 起動・/weakspot reload・設定画面のあとに呼ぶ。 */
    public static void invalidateServer() {
        server = null;
    }

    /** 毎回作る。 */
    public static SyncedSettings fromConfig() {
        SyncedSettings s = new SyncedSettings();
        try {
            for (Field field : FIELDS) {
                if (field.getName().equals("serverVersion")) {
                    continue;
                }
                Object value = WeakSpotConfig.setting(field.getName()).get();
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
