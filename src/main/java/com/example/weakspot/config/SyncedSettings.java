package com.example.weakspot.config;

import com.example.weakspot.common.GrowthFilters;
import io.netty.buffer.ByteBuf;
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

    /** この側の weakspot.cfg の値。 */
    public static SyncedSettings fromConfig() {
        SyncedSettings s = new SyncedSettings();
        s.boostMultiplier = WeakSpotConfig.boostMultiplier;
        s.boostDurationTicks = WeakSpotConfig.boostDurationTicks;
        s.minHitIntervalTicks = WeakSpotConfig.minHitIntervalTicks;
        s.weakSpotRadiusRatio = WeakSpotConfig.weakSpotRadiusRatio;
        s.edgeMargin = WeakSpotConfig.edgeMargin;
        s.minMoveDistance = WeakSpotConfig.minMoveDistance;
        s.weakSpotMinRadius = WeakSpotConfig.weakSpotMinRadius;
        s.weakSpotMaxRadiusRatio = WeakSpotConfig.weakSpotMaxRadiusRatio;
        s.minFaceSize = WeakSpotConfig.minFaceSize;
        s.lingerTicks = WeakSpotConfig.lingerTicks;
        s.growthMinHitIntervalTicks = WeakSpotConfig.growthMinHitIntervalTicks;
        s.growthMinRadius = WeakSpotConfig.growthMinRadius;
        s.growthExcludedBlocks = new HashSet<>(Arrays.asList(WeakSpotConfig.growthExcludedBlocks));
        s.growthExtraBlocks = new HashSet<>(Arrays.asList(WeakSpotConfig.growthExtraBlocks));
        s.machineMinHitIntervalTicks = WeakSpotConfig.machineMinHitIntervalTicks;
        s.excludedBlocks = new HashSet<>(Arrays.asList(WeakSpotConfig.excludedBlocks));
        s.animalBabyEnabled = WeakSpotConfig.animalBabyEnabled;
        s.animalBreedingEnabled = WeakSpotConfig.animalBreedingEnabled;
        s.sheepWoolEnabled = WeakSpotConfig.sheepWoolEnabled;
        s.chickenEggEnabled = WeakSpotConfig.chickenEggEnabled;
        s.villagerTradeResetEnabled = WeakSpotConfig.villagerTradeResetEnabled;
        s.animalBabyHits = WeakSpotConfig.animalBabyHits;
        s.animalBreedingHits = WeakSpotConfig.animalBreedingHits;
        s.sheepWoolHits = WeakSpotConfig.sheepWoolHits;
        s.chickenEggHits = WeakSpotConfig.chickenEggHits;
        s.villagerTradeResetHits = WeakSpotConfig.villagerTradeResetHits;
        s.animalMinHitIntervalTicks = WeakSpotConfig.animalMinHitIntervalTicks;
        s.animalExcludedEntities = new HashSet<>(Arrays.asList(WeakSpotConfig.animalExcludedEntities));
        s.animalSneakRequiredEntities = new HashSet<>(Arrays.asList(WeakSpotConfig.animalSneakRequiredEntities));
        s.fishingWeakSpotEnabled = WeakSpotConfig.fishingWeakSpotEnabled;
        s.fishingHits = WeakSpotConfig.fishingHits;
        s.fishingMinHitIntervalTicks = WeakSpotConfig.fishingMinHitIntervalTicks;
        s.bowWeakSpotEnabled = WeakSpotConfig.bowWeakSpotEnabled;
        s.bowHitTicks = WeakSpotConfig.bowHitTicks;
        s.bowMinHitIntervalTicks = WeakSpotConfig.bowMinHitIntervalTicks;
        s.meleeWeakSpotEnabled = WeakSpotConfig.meleeWeakSpotEnabled;
        s.meleeMinHitIntervalTicks = WeakSpotConfig.meleeMinHitIntervalTicks;
        s.markerShareRange = WeakSpotConfig.markerShareRange;
        s.markerSendMinIntervalTicks = WeakSpotConfig.markerSendMinIntervalTicks;
        return s;
    }

    public void write(ByteBuf buf) {
        buf.writeDouble(boostMultiplier);
        buf.writeInt(boostDurationTicks);
        buf.writeInt(minHitIntervalTicks);
        buf.writeDouble(weakSpotRadiusRatio);
        buf.writeDouble(edgeMargin);
        buf.writeDouble(minMoveDistance);
        buf.writeDouble(weakSpotMinRadius);
        buf.writeDouble(weakSpotMaxRadiusRatio);
        buf.writeDouble(minFaceSize);
        buf.writeInt(lingerTicks);
        buf.writeInt(growthMinHitIntervalTicks);
        buf.writeDouble(growthMinRadius);
        writeStrings(buf, growthExcludedBlocks);
        writeStrings(buf, growthExtraBlocks);
        buf.writeInt(machineMinHitIntervalTicks);
        writeStrings(buf, excludedBlocks);
        buf.writeBoolean(animalBabyEnabled);
        buf.writeBoolean(animalBreedingEnabled);
        buf.writeBoolean(sheepWoolEnabled);
        buf.writeBoolean(chickenEggEnabled);
        buf.writeBoolean(villagerTradeResetEnabled);
        buf.writeInt(animalBabyHits);
        buf.writeInt(animalBreedingHits);
        buf.writeInt(sheepWoolHits);
        buf.writeInt(chickenEggHits);
        buf.writeInt(villagerTradeResetHits);
        buf.writeInt(animalMinHitIntervalTicks);
        writeStrings(buf, animalExcludedEntities);
        writeStrings(buf, animalSneakRequiredEntities);
        buf.writeBoolean(fishingWeakSpotEnabled);
        buf.writeInt(fishingHits);
        buf.writeInt(fishingMinHitIntervalTicks);
        buf.writeBoolean(bowWeakSpotEnabled);
        buf.writeInt(bowHitTicks);
        buf.writeInt(bowMinHitIntervalTicks);
        buf.writeBoolean(meleeWeakSpotEnabled);
        buf.writeInt(meleeMinHitIntervalTicks);
        buf.writeDouble(markerShareRange);
        buf.writeInt(markerSendMinIntervalTicks);
    }

    public static SyncedSettings read(ByteBuf buf) {
        SyncedSettings s = new SyncedSettings();
        s.boostMultiplier = buf.readDouble();
        s.boostDurationTicks = buf.readInt();
        s.minHitIntervalTicks = buf.readInt();
        s.weakSpotRadiusRatio = buf.readDouble();
        s.edgeMargin = buf.readDouble();
        s.minMoveDistance = buf.readDouble();
        s.weakSpotMinRadius = buf.readDouble();
        s.weakSpotMaxRadiusRatio = buf.readDouble();
        s.minFaceSize = buf.readDouble();
        s.lingerTicks = buf.readInt();
        s.growthMinHitIntervalTicks = buf.readInt();
        s.growthMinRadius = buf.readDouble();
        s.growthExcludedBlocks = readStrings(buf);
        s.growthExtraBlocks = readStrings(buf);
        s.machineMinHitIntervalTicks = buf.readInt();
        s.excludedBlocks = readStrings(buf);
        s.animalBabyEnabled = buf.readBoolean();
        s.animalBreedingEnabled = buf.readBoolean();
        s.sheepWoolEnabled = buf.readBoolean();
        s.chickenEggEnabled = buf.readBoolean();
        s.villagerTradeResetEnabled = buf.readBoolean();
        s.animalBabyHits = buf.readInt();
        s.animalBreedingHits = buf.readInt();
        s.sheepWoolHits = buf.readInt();
        s.chickenEggHits = buf.readInt();
        s.villagerTradeResetHits = buf.readInt();
        s.animalMinHitIntervalTicks = buf.readInt();
        s.animalExcludedEntities = readStrings(buf);
        s.animalSneakRequiredEntities = readStrings(buf);
        s.fishingWeakSpotEnabled = buf.readBoolean();
        s.fishingHits = buf.readInt();
        s.fishingMinHitIntervalTicks = buf.readInt();
        s.bowWeakSpotEnabled = buf.readBoolean();
        s.bowHitTicks = buf.readInt();
        s.bowMinHitIntervalTicks = buf.readInt();
        s.meleeWeakSpotEnabled = buf.readBoolean();
        s.meleeMinHitIntervalTicks = buf.readInt();
        s.markerShareRange = buf.readDouble();
        s.markerSendMinIntervalTicks = buf.readInt();
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
