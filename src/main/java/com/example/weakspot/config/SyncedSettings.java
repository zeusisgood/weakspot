package com.example.weakspot.config;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.GrowthFilters;
import com.example.weakspot.server.EnchantHits;
import com.example.weakspot.server.PortalHits;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
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

    public double boostMultiplier;
    public int boostDurationTicks;
    public int minHitIntervalTicks;
    public double weakSpotRadiusRatio;
    public double edgeMargin;
    public double minMoveDistance;
    public double weakSpotMinRadius;
    public double weakSpotMaxRadiusRatio;
    public double minFaceSize;
    public int lingerTicks;
    public int growthMinHitIntervalTicks;
    public double growthMinRadius;
    /** 登録名（"minecraft:grass" など）。 */
    public Set<String> growthExcludedBlocks;
    /** 成長の弱点の追加リスト。1行は「登録名」か「登録名[状態の条件]」（1.4.0。GrowthFilters）。 */
    public Set<String> growthExtraBlocks;
    /** growthExtraBlocks を読んだもの。最初に使うときに作る（作った後は読むだけなので、使い回してよい）。送らない。 */
    private GrowthFilters growthFilters;
    public int machineMinHitIntervalTicks;
    /** 機械の加速の対象外。登録名。 */
    public Set<String> excludedBlocks;
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
    public int animalMinHitIntervalTicks;
    /** 動物の弱点の対象外。エンティティの ID。 */
    public Set<String> animalExcludedEntities;
    /** しゃがみを条件にする MOD の動物。エンティティの ID。 */
    public Set<String> animalSneakRequiredEntities;
    public boolean fishingWeakSpotEnabled;
    public int fishingHits;
    public int fishingMinHitIntervalTicks;
    public boolean bowWeakSpotEnabled;
    public int bowHitTicks;
    public int bowMinHitIntervalTicks;
    public boolean meleeWeakSpotEnabled;
    public int meleeMinHitIntervalTicks;
    /** 近接の溜め（1.8.0。1.7.x までの meleeWeakSpotScale はなくした）。 */
    public double meleeChargePerHit;
    public double meleeChargeMax;
    /** ネザーゲートの弱点（1.8.0）。サーバーが待ち時間を読み書きできないときは false で送る。 */
    public boolean portalWeakSpotEnabled;
    public int portalHitTicks;
    public int portalMinHitIntervalTicks;
    /** 機械の倍率と上限（1.6.0。HUD に実際の速さを出すため）。 */
    public double machineBoostMultiplier;
    public double machineBoostMaxMultiplier;
    /** 寝ている間の弱点（1.6.0）。進める時刻はサーバーだけが使うので送らない。 */
    public boolean sleepWeakSpotEnabled;
    public int sleepMinHitIntervalTicks;
    /** 食事・飲み物の弱点（1.6.0）。 */
    public boolean eatWeakSpotEnabled;
    public int eatHitTicks;
    public int eatMinHitIntervalTicks;
    /** 乗り物の弱点（1.6.0）。 */
    public boolean vehicleWeakSpotEnabled;
    public double vehicleBoostMultiplier;
    public double vehicleBoostMaxMultiplier;
    public int vehicleBoostDurationTicks;
    public int vehicleMinHitIntervalTicks;
    /** はしごの弱点（1.7.0。速さはクライアントで足すので、倍率・上限・時間も送る）。 */
    public boolean ladderWeakSpotEnabled;
    public double ladderBoostMultiplier;
    public double ladderBoostMaxMultiplier;
    public int ladderBoostDurationTicks;
    public int ladderMinHitIntervalTicks;
    /** エリトラの弱点（1.7.0。速さはクライアントで足す）。 */
    public boolean elytraWeakSpotEnabled;
    public double elytraBoostPower;
    public int elytraMinHitIntervalTicks;
    /** エンチャントの弱点（1.7.0）。サーバーが種を読み書きできないときは false で送る。 */
    public boolean enchantWeakSpotEnabled;
    public int enchantMinHitIntervalTicks;
    /** 収穫の弱点（1.7.0）。おまけの有無はサーバーだけが使うので送らない。 */
    public boolean harvestWeakSpotEnabled;
    public int harvestMinHitIntervalTicks;
    /** 投げる物の弱点（1.7.0。溜めのゲージをクライアントも進める）。 */
    public boolean throwWeakSpotEnabled;
    public double throwChargePerHit;
    public int throwMinHitIntervalTicks;
    /** 走りの弱点（1.7.0。残り時間のゲージのため、倍率・時間も送る）。 */
    public boolean sprintWeakSpotEnabled;
    public double sprintBoostMultiplier;
    public double sprintBoostMaxMultiplier;
    public int sprintBoostDurationTicks;
    public int sprintMinHitIntervalTicks;
    /** 値を送ったサーバーの Mod の版（1.6.0。ServerFeatures）。自分の設定の値なら、自分の版。 */
    public String serverVersion;
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
     * 送る項目と並び（1.8.6 で一覧にした。並びは 1.8.5 までの手書きの write と同じで、通信の中身を変えない）。
     * 項目を足すときは、フィールドとこの一覧の両方に足す（通信が変わるので、マイナー）。フィールドの名前が
     * WeakSpotConfig の同じ名前の項目から値を取る（serverVersion は自分の版）。
     */
    static final String[] WIRE = {
            "boostMultiplier", "boostDurationTicks", "minHitIntervalTicks",
            "weakSpotRadiusRatio", "edgeMargin", "minMoveDistance",
            "weakSpotMinRadius", "weakSpotMaxRadiusRatio", "minFaceSize",
            "lingerTicks", "growthMinHitIntervalTicks", "growthMinRadius",
            "growthExcludedBlocks", "growthExtraBlocks", "machineMinHitIntervalTicks",
            "excludedBlocks", "animalBabyEnabled", "animalBreedingEnabled",
            "sheepWoolEnabled", "chickenEggEnabled", "villagerTradeResetEnabled",
            "animalBabyHits", "animalBreedingHits", "sheepWoolHits",
            "chickenEggHits", "villagerTradeResetHits", "animalMinHitIntervalTicks",
            "animalExcludedEntities", "animalSneakRequiredEntities", "fishingWeakSpotEnabled",
            "fishingHits", "fishingMinHitIntervalTicks", "bowWeakSpotEnabled",
            "bowHitTicks", "bowMinHitIntervalTicks", "meleeWeakSpotEnabled",
            "meleeMinHitIntervalTicks", "meleeChargePerHit", "meleeChargeMax",
            "portalWeakSpotEnabled", "portalHitTicks", "portalMinHitIntervalTicks",
            "machineBoostMultiplier", "machineBoostMaxMultiplier", "serverVersion",
            "sleepWeakSpotEnabled", "sleepMinHitIntervalTicks", "eatWeakSpotEnabled",
            "eatHitTicks", "eatMinHitIntervalTicks", "vehicleWeakSpotEnabled",
            "vehicleBoostMultiplier", "vehicleBoostMaxMultiplier", "vehicleBoostDurationTicks",
            "vehicleMinHitIntervalTicks", "markerShareRange", "markerSendMinIntervalTicks",
            "ladderWeakSpotEnabled", "ladderBoostMultiplier", "ladderBoostMaxMultiplier",
            "ladderBoostDurationTicks", "ladderMinHitIntervalTicks", "elytraWeakSpotEnabled",
            "elytraBoostPower", "elytraMinHitIntervalTicks", "enchantWeakSpotEnabled",
            "enchantMinHitIntervalTicks", "harvestWeakSpotEnabled", "harvestMinHitIntervalTicks",
            "throwWeakSpotEnabled", "throwChargePerHit", "throwMinHitIntervalTicks",
            "sprintWeakSpotEnabled", "sprintBoostMultiplier", "sprintBoostMaxMultiplier",
            "sprintBoostDurationTicks", "sprintMinHitIntervalTicks"
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

    /** この側の weakspot.cfg の値。 */
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
