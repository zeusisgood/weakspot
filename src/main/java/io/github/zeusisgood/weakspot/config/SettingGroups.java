package io.github.zeusisgood.weakspot.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 設定画面のまとめ（1.10.2）。ファイル（weakspot.cfg）の中は server / client のカテゴリのままで、設定画面だけを
 * 種類・用途でまとめ直す（サーバーの項目とクライアントの項目を混ぜる）。まとめの名前は翻訳キー weakspot.gui.&lt;まとめ&gt;。
 * 設定を足したら、ここにも入れる（入っていないと ConfigScreenTest が落ちる）。
 */
public final class SettingGroups {

    /** 設定画面に出さない項目（Mod が書き換える。ファイルには残す）。 */
    public static final Set<String> HIDDEN = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("configVersion", "lastSeenVersion")));

    /** まとめ（画面に出る順）→ 項目の名前（画面に出る順）。 */
    public static final Map<String, List<String>> GROUPS;

    static {
        Map<String, List<String>> g = new LinkedHashMap<>();
        g.put("general", list("spotSize", "heldItemsCountAsEmptyHand", "heldItemExcludes", "giveGuideBook",
                "showUpdateNotes", "checkForUpdates", "skippedUpdateVersion"));
        g.put("sound", list("myHitSound", "myHitVolume", "hitChordEnabled", "hitScaleDirection", "hitScaleType",
                "hitScaleOctaves", "othersHitSound", "othersHitVolume"));
        g.put("markers", list("weakSpotsEnabled", "disabledKinds", "myMarkerColors", "myMarkerShapes",
                "goldHitParticles", "weakSpotTrailEnabled", "otherMarkerEnabled", "otherMarkerColor", "otherMarkerAlpha", "otherMarkerShape",
                "markerShareRange", "markerSendMinIntervalTicks"));
        g.put("combo", list("comboDisplayEnabled", "comboScale", "comboPosition", "comboMilestoneEffects",
                "othersComboDisplay", "comboFactorGaugeEnabled"));
        g.put("milestones", list("showOthersMilestones", "milestones", "milestoneXp", "milestoneRepair",
                "milestoneRepeatInterval", "kindMilestonesEnabled", "totalMilestonesEnabled", "totalMilestones",
                "totalMilestoneXp", "totalMilestoneRepeatInterval"));
        g.put("mining", list("miningWeakSpotEnabled", "miningComboBonus", "boostMultiplier", "boostDurationTicks",
                "weakSpotRadiusRatio", "edgeMargin", "minMoveDistance", "weakSpotMinRadius", "weakSpotMaxRadiusRatio",
                "minFaceSize", "lingerTicks", "minHitIntervalTicks", "hitsPerRepair", "repairPerStep",
                "maxRepairPerBreak", "blockHealthBarEnabled"));
        g.put("growth", list("growthWeakSpotEnabled", "growthTicksPerHit", "growthWarnings", "growthStuckHits",
                "growthMinHitIntervalTicks", "growthMinRadius", "growthExcludedBlocks", "growthExtraBlocks",
                "mushroomGrowChance", "growthBarEnabled"));
        g.put("harvest", list("harvestWeakSpotEnabled", "harvestMinHitIntervalTicks", "harvestComboBonus"));
        g.put("machine", list("machineWeakSpotEnabled", "machineBoostMultiplier", "machineBoostMaxMultiplier",
                "machineBoostParticles", "machineBoostDurationTicks", "machineMinHitIntervalTicks", "excludedBlocks",
                "machineBarEnabled", "machineParticlesVisible"));
        g.put("animal", list("animalWeakSpotEnabled", "animalBabyEnabled", "animalBreedingEnabled", "sheepWoolEnabled",
                "chickenEggEnabled", "villagerTradeResetEnabled", "animalBabyHits", "animalBreedingHits",
                "sheepWoolHits", "chickenEggHits", "villagerTradeResetHits", "animalMinHitIntervalTicks",
                "animalExcludedEntities", "animalSneakRequiredEntities", "villagerResetUnlocksNewTier",
                "animalSpotSeeThrough"));
        g.put("fishing", list("fishingWeakSpotEnabled", "fishingHits", "fishingMinHitIntervalTicks"));
        g.put("bow", list("bowWeakSpotEnabled", "bowHitTicks", "bowMinHitIntervalTicks", "bowDrawBarEnabled"));
        g.put("melee", list("meleeWeakSpotEnabled", "meleeMinHitIntervalTicks", "meleeChargePerHit", "meleeChargeMax",
                "critsPerRepair", "critRepairPerStep", "meleeChargeBarEnabled"));
        g.put("throw", list("throwWeakSpotEnabled", "throwChargePerHit", "throwMinHitIntervalTicks",
                "throwChargeBarEnabled"));
        g.put("eat", list("eatWeakSpotEnabled", "eatHitTicks", "eatMinHitIntervalTicks"));
        g.put("vehicle", list("vehicleWeakSpotEnabled", "vehicleBoostMultiplier", "vehicleBoostMaxMultiplier",
                "vehicleBoostDurationTicks", "vehicleMinHitIntervalTicks", "vehicleBoostBarEnabled"));
        g.put("ladder", list("ladderWeakSpotEnabled", "ladderBoostMultiplier", "ladderBoostMaxMultiplier",
                "ladderBoostDurationTicks", "ladderMinHitIntervalTicks", "ladderBoostBarEnabled"));
        g.put("sprint", list("sprintWeakSpotEnabled", "sprintBoostMultiplier", "sprintBoostMaxMultiplier",
                "sprintBoostDurationTicks", "sprintMinHitIntervalTicks", "sprintBoostBarEnabled"));
        g.put("elytra", list("elytraWeakSpotEnabled", "elytraBoostPower", "elytraMinHitIntervalTicks"));
        g.put("portal", list("portalWeakSpotEnabled", "portalHitTicks", "portalMinHitIntervalTicks"));
        g.put("swim", list("swimWeakSpotEnabled", "swimBoostMultiplier", "swimBoostMaxMultiplier",
                "swimBoostDurationTicks", "swimDashDistance", "swimMinHitIntervalTicks", "swimBoostBarEnabled"));
        g.put("fall", list("fallWeakSpotEnabled", "fallMinDistance", "fallReduceBlocks", "fallMinHitIntervalTicks",
                "fallDamageHintEnabled"));
        g.put("sleep", list("sleepWeakSpotEnabled", "sleepHitTicks", "sleepMinHitIntervalTicks"));
        g.put("enchant", list("enchantWeakSpotEnabled", "enchantMinHitIntervalTicks"));
        GROUPS = Collections.unmodifiableMap(g);
    }

    private SettingGroups() {
    }

    private static List<String> list(String... names) {
        return Collections.unmodifiableList(new ArrayList<>(Arrays.asList(names)));
    }

    /** まとめの名前の翻訳キー。 */
    public static String langKey(String group) {
        return "weakspot.gui." + group;
    }
}
