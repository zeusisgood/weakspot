package com.example.weakspot.config;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.MarkerShape;
import com.example.weakspot.server.SettingsSync;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;

/**
 * config/weakspot.cfg。
 * [サーバー] はサーバーの値が正で、ログイン時と変更時にクライアントへ送る（SyncedSettings）。
 * クライアントは接続中、自分の [サーバー] の値を使わない。[クライアント] は各プレイヤーの設定のまま使う。
 * 既存のキー名を変えないように、カテゴリは分けない。
 */
@Config(modid = WeakSpotMod.MODID)
public final class WeakSpotConfig {

    @Config.Comment("[サーバー] ヒット時の破壊速度の倍率")
    @Config.RangeDouble(min = 1.0, max = 100.0)
    public static double boostMultiplier = 4.0;

    @Config.Comment("[サーバー] 倍率を掛ける時間（tick）。1回のヒットで通常の (倍率-1)×この値 tick 分だけ進む")
    @Config.RangeInt(min = 1, max = 200)
    public static int boostDurationTicks = 4;

    @Config.Comment("[サーバー] 弱点の半径（面の短い辺に対する比率）")
    @Config.RangeDouble(min = 0.02, max = 0.5)
    public static double weakSpotRadiusRatio = 0.14;

    @Config.Comment({"[サーバー] 弱点の外周とブロック面の縁の間に空ける最小の距離（ブロック）",
            "大きくすると弱点が面の中央に寄る。大きくしすぎると minMoveDistance だけ離れた場所が取れず、移動距離が短くなる"})
    @Config.RangeDouble(min = 0.0, max = 0.5)
    public static double edgeMargin = 0.1;

    @Config.Comment("[サーバー] ヒット後に弱点が移動する最小距離（ブロック）")
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double minMoveDistance = 0.4;

    @Config.Comment({"[サーバー] 弱点の半径の下限（ブロック）。ボタンなどの小さい面でも当てやすくする",
            "上限（weakSpotMaxRadiusRatio）と食い違うときは上限を優先する"})
    @Config.RangeDouble(min = 0.0, max = 0.5)
    public static double weakSpotMinRadius = 0.08;

    @Config.Comment("[サーバー] 弱点の半径の上限（面の短い辺に対する比率）。小さい面で弱点が面からはみ出さないようにする")
    @Config.RangeDouble(min = 0.05, max = 0.5)
    public static double weakSpotMaxRadiusRatio = 0.35;

    @Config.Comment({"[サーバー] 弱点を出す面の、短い辺の最小の長さ（ブロック）。これより小さい面（カーペットの側面など）には弱点を出さない",
            "0 で、どんな小さい面にも出す"})
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double minFaceSize = 0.15;

    @Config.Comment("[サーバー] 長押しをやめた後に弱点が残る時間（tick）")
    @Config.RangeInt(min = 0, max = 1200)
    public static int lingerTicks = 40;

    @Config.Comment("[サーバー] ヒット通知を受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
    @Config.RangeInt(min = 0, max = 200)
    public static int minHitIntervalTicks = 6;

    @Config.Comment({"[サーバー] ブロックを壊したとき、そのブロックで当てた採掘ヒットを数え、この数ごとに手に持っているツールの耐久を回復する",
            "壊さずにやめたブロックのヒットは数えない。余りは次に壊すブロックへ持ち越す。0 で回復しない"})
    @Config.RangeInt(min = 0, max = 100000)
    public static int hitsPerRepair = 5;

    @Config.Comment("[サーバー] hitsPerRepair ごとに回復する耐久")
    @Config.RangeInt(min = 0, max = 100000)
    public static int repairPerStep = 1;

    @Config.Comment({"[サーバー] 1回の破壊で回復する耐久の上限。上限で回復できなかった分は持ち越さない",
            "0 で耐久回復（節目の報酬を除く）が無効になる"})
    @Config.RangeInt(min = 0, max = 100000)
    public static int maxRepairPerBreak = 1;

    @Config.Comment({"[サーバー] 節目にする採掘ヒットの累計。達した瞬間に1回だけ、祝いの演出と報酬が出る",
            "milestoneXp と milestoneRepair は、この順番に対応する"})
    public static int[] milestones = {100, 777, 1000, 10000};

    @Config.Comment({"[サーバー] 節目ごとにもらえる経験値（milestones の順に対応）",
            "数が足りない分は 0 として扱う"})
    public static int[] milestoneXp = {10, 77, 30, 100};

    @Config.Comment({"[サーバー] 節目ごとに回復する、手に持っているツールの耐久（milestones の順に対応）",
            "数が足りない分は 0 として扱う"})
    public static int[] milestoneRepair = {10, 77, 30, 100};

    @Config.Comment({"[サーバー] 作物・苗木の弱点に1回当てるごとに、そのブロックに余分に呼ぶ randomTick の回数",
            "骨粉と違い、明るさや農地の水分などの成長条件は守ったまま速くなる"})
    @Config.RangeInt(min = 0, max = 1000)
    public static int growthTicksPerHit = 5;

    @Config.Comment("[サーバー] 作物・苗木のヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
    @Config.RangeInt(min = 0, max = 200)
    public static int growthMinHitIntervalTicks = 6;

    @Config.Comment("[サーバー] 作物・苗木の弱点の最小の半径（ブロック）。小さい面でも当てやすくする")
    @Config.RangeDouble(min = 0.0, max = 0.5)
    public static double growthMinRadius = 0.08;

    @Config.Comment({"[サーバー] 成長の弱点を出さないブロックの登録名（サトウキビ・サボテン・ネザーウォートにも効く）",
            "IGrowable を持つブロックのうち、作物・苗木ではないもの（草ブロック、草など）を初期値で外している"})
    public static String[] growthExcludedBlocks = {
            "minecraft:grass", "minecraft:tallgrass", "minecraft:double_plant"};

    @Config.Comment({"[サーバー] キノコへの成長ヒット1回で、巨大キノコに育てようとする確率（0〜1。骨粉1回と同じ処理）",
            "育つ条件（下のブロック、上の空き）はバニラのまま。育たなければキノコが残る"})
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double mushroomGrowChance = 0.1;

    @Config.Comment({"[サーバー] IGrowable を持たない植物のうち、成長の弱点の対象にするブロックの登録名（追加リスト）",
            "育つ条件をコードで決めてあるのは、サトウキビ・サボテン・ネザーウォートだけ。それ以外を足しても弱点は出ない",
            "growthExcludedBlocks に入っているブロックは、ここにあっても対象外になる"})
    public static String[] growthExtraBlocks = {"minecraft:reeds", "minecraft:cactus", "minecraft:nether_wart"};

    @Config.Comment({"[サーバー] 機械の弱点に当てたとき、update() を毎tick何倍呼ぶか（4.0 なら毎tick 3回余分に呼ぶ）",
            "複数のプレイヤーが同じ機械を加速しても、足さずに大きいほうだけを使う"})
    @Config.RangeDouble(min = 1.0, max = 100.0)
    public static double machineBoostMultiplier = 4.0;

    @Config.Comment("[サーバー] 機械の加速が続く時間（tick）")
    @Config.RangeInt(min = 0, max = 200)
    public static int machineBoostDurationTicks = 4;

    @Config.Comment("[サーバー] 機械のヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
    @Config.RangeInt(min = 0, max = 200)
    public static int machineMinHitIntervalTicks = 6;

    @Config.Comment({"[サーバー] 機械の加速の対象外にするブロックの登録名。対象外のブロックは、しゃがんで素手で右クリックしても GUI が普通に開く",
            "加速で不具合が出た機械は、ここに登録名を足す"})
    public static String[] excludedBlocks = {
            "minecraft:chest", "minecraft:trapped_chest", "minecraft:ender_chest",
            "minecraft:enchanting_table", "minecraft:beacon",
            "minecraft:white_shulker_box", "minecraft:orange_shulker_box", "minecraft:magenta_shulker_box",
            "minecraft:light_blue_shulker_box", "minecraft:yellow_shulker_box", "minecraft:lime_shulker_box",
            "minecraft:pink_shulker_box", "minecraft:gray_shulker_box", "minecraft:silver_shulker_box",
            "minecraft:cyan_shulker_box", "minecraft:purple_shulker_box", "minecraft:blue_shulker_box",
            "minecraft:brown_shulker_box", "minecraft:green_shulker_box", "minecraft:red_shulker_box",
            "minecraft:black_shulker_box"};

    @Config.Comment("[サーバー] 動物の弱点で、子どもの成長を早めるか")
    public static boolean animalBabyEnabled = true;

    @Config.Comment("[サーバー] 動物の弱点で、繁殖の待ち時間を短くするか")
    public static boolean animalBreedingEnabled = true;

    @Config.Comment("[サーバー] 羊の弱点で、羊毛の再生を早めるか（草は要らない）")
    public static boolean sheepWoolEnabled = true;

    @Config.Comment("[サーバー] ニワトリの弱点で、次の卵までの時間を短くするか")
    public static boolean chickenEggEnabled = true;

    @Config.Comment("[サーバー] 村人の弱点で、ロックされた取引の上限をリセットするか")
    public static boolean villagerTradeResetEnabled = true;

    @Config.Comment({"[サーバー] 子どもが大人になるまでの目安のヒット数（1ヒット = 24000 tick ÷ この数）",
            "0 以下で、子どもの成長の加速を無効にする"})
    @Config.RangeInt(min = 0, max = 100000)
    public static int animalBabyHits = 40;

    @Config.Comment({"[サーバー] 繁殖の待ち時間が終わるまでの目安のヒット数（1ヒット = 6000 tick ÷ この数）",
            "0 以下で、繁殖の待ち時間の短縮を無効にする"})
    @Config.RangeInt(min = 0, max = 100000)
    public static int animalBreedingHits = 10;

    @Config.Comment({"[サーバー] 毛を刈られた羊の毛が生えるまでのヒット数", "0 以下で無効にする"})
    @Config.RangeInt(min = 0, max = 100000)
    public static int sheepWoolHits = 10;

    @Config.Comment({"[サーバー] 次の卵までの目安のヒット数（1ヒット = 平均 9000 tick ÷ この数）", "0 以下で無効にする"})
    @Config.RangeInt(min = 0, max = 100000)
    public static int chickenEggHits = 15;

    @Config.Comment({"[サーバー] 村人の、ロックされた取引の上限がリセットされるまでのヒット数", "0 以下で無効にする"})
    @Config.RangeInt(min = 0, max = 100000)
    public static int villagerTradeResetHits = 10;

    @Config.Comment("[サーバー] 動物のヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
    @Config.RangeInt(min = 0, max = 200)
    public static int animalMinHitIntervalTicks = 6;

    @Config.Comment({"[サーバー] 動物の弱点の対象外にするエンティティの ID（例: minecraft:cow）",
            "対象外の動物には弱点が出ず、右クリックも通常の動作になる"})
    public static String[] animalExcludedEntities = {};

    @Config.Comment({"[サーバー] しゃがみ+素手のときだけ弱点を出す MOD の動物のエンティティの ID",
            "素手の右クリックに別の動作がある動物（乗る、持ち物の画面、座るなど）を足す。バニラの馬・飼いならしたオオカミなどは、コードで判定する"})
    public static String[] animalSneakRequiredEntities = {};

    @Config.Comment({"[サーバー] 村人の取引上限のリセットで、新しい取引の段階も解放するか（バニラの補充と同じ）",
            "オフなら、ロックの解除だけ行う。サーバーだけが使う（クライアントには送らない）"})
    public static boolean villagerResetUnlocksNewTier = false;

    @Config.Comment("[サーバー] 釣りの弱点のオン・オフ")
    public static boolean fishingWeakSpotEnabled = true;

    @Config.Comment({"[サーバー] 釣りの、最長の待ち時間（600 tick）を 0 にするまでのヒット数（1ヒット = 600 tick ÷ この数）",
            "0 以下で、釣りの弱点を無効にする"})
    @Config.RangeInt(min = 0, max = 600)
    public static int fishingHits = 6;

    @Config.Comment("[サーバー] 釣りのヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
    @Config.RangeInt(min = 0, max = 200)
    public static int fishingMinHitIntervalTicks = 6;

    @Config.Comment({"[サーバー] 他のプレイヤーの弱点マークを転送する範囲（ブロック）。マークからこの距離以内のプレイヤーにだけ見える",
            "0 で転送しない（他のプレイヤーのマークは見えなくなる）"})
    @Config.RangeDouble(min = 0.0, max = 256.0)
    public static double markerShareRange = 16;

    @Config.Comment("[サーバー] 弱点マークの状態を、1プレイヤーあたり送る最小間隔（tick）。通信の多さを抑える")
    @Config.RangeInt(min = 0, max = 200)
    public static int markerSendMinIntervalTicks = 2;

    @Config.Comment({"[クライアント] 自分のヒット音に使うノートブロックの楽器（採掘・成長・機械で共通）",
            "XYLOPHONE, CHIME, BELL, FLUTE, GUITAR, HARP, BASS, HAT, SNARE, BASEDRUM, PLING",
            "統計画面（K キー）のサウンドでも変えられ、試聴できる"})
    public static HitSound myHitSound = HitSound.PLING;

    @Config.Comment({"[クライアント] 自分のヒット音の音量（0 で聞こえなくなる）",
            "バニラの「プレイヤー」音量も掛かる"})
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double myHitVolume = 0.25;

    @Config.Comment({"[クライアント] 他のプレイヤーのヒット音の音量（0 で聞こえなくなる）",
            "バニラの「プレイヤー」音量も掛かる"})
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double othersHitVolume = 0.4;

    @Config.Comment({"[クライアント] 他のプレイヤーのヒット音に使うノートブロックの楽器",
            "XYLOPHONE, CHIME, BELL, FLUTE, GUITAR, HARP, BASS, HAT, SNARE, BASEDRUM, PLING"})
    public static HitSound othersHitSound = HitSound.XYLOPHONE;

    @Config.Comment("[クライアント] 他のプレイヤーの弱点マークを表示するか")
    public static boolean otherMarkerEnabled = true;

    @Config.Comment({"[クライアント] 他のプレイヤーの弱点マークの色（#RRGGBB）。自分のマーク（オレンジ）と区別できる色にする",
            "読めない値のときは初期値の水色 #3FA9FF を使う"})
    public static String otherMarkerColor = "#3FA9FF";

    @Config.Comment("[クライアント] 他のプレイヤーの弱点マークの濃さ（自分のマークの濃さに掛ける。1 で同じ、0 で見えない）")
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double otherMarkerAlpha = 0.6;

    @Config.Comment({"[クライアント] 連続ヒット（コンボ）の数を画面に表示するか",
            "ヒット音の音階と同じ数え方（約2秒ヒットがないと途切れる）。2 以上で表示する"})
    public static boolean comboDisplayEnabled = true;

    @Config.Comment("[クライアント] コンボの数字の大きさの倍率")
    @Config.RangeDouble(min = 0.5, max = 2.0)
    public static double comboScale = 1.0;

    @Config.Comment({"[クライアント] コンボの表示の位置",
            "BELOW_CROSSHAIR（照準の下）, RIGHT_OF_CROSSHAIR（照準の右）, TOP_CENTER（画面の上の中央）"})
    public static ComboPosition comboPosition = ComboPosition.BELOW_CROSSHAIR;

    @Config.Comment("[クライアント] コンボが 10、25、50、100 に達したときの演出（強調音と光）")
    public static boolean comboMilestoneEffects = true;

    @Config.Comment({"[クライアント] 弱点が移動するときの演出（古い位置から素早く動き、残像を残す）",
            "見た目だけで、当たり判定は移動先で即時。オフにすると、その場で切り替わる。他のプレイヤーのマークにも効く"})
    public static boolean weakSpotTrailEnabled = true;

    @Config.Comment({"[クライアント] 掘っているブロックの残りの耐久を、面の下の余白に緑のバーで表示するか",
            "自分が左クリックの長押しで掘っている、弱点が出るブロックだけに出る"})
    public static boolean blockHealthBarEnabled = true;

    @Config.Comment({"[クライアント] 作物の足元に、成長の進み具合を黄色のバーで表示するか",
            "成長の弱点を出している間（素手で右クリックを押しっぱなしにしている間）だけ出る"})
    public static boolean growthBarEnabled = true;

    @Config.Comment({"[クライアント] 他のプレイヤーの弱点マークの形",
            "CIRCLE（塗りつぶした円）, RING（中抜きの輪）, DIAMOND（ひし形）, SQUARE（四角）。自分のマークは円のまま"})
    public static MarkerShape otherMarkerShape = MarkerShape.RING;

    @Config.Comment({"[クライアント] 自分の弱点のオン・オフ（J キーで切り替わる。操作設定で変えられる）",
            "オフの間は、自分の弱点が出ず、通常の遊び方になる。他のプレイヤーのマークは見える"})
    public static boolean weakSpotsEnabled = true;

    @Config.Comment({"[クライアント] 自分の動物の弱点が体に隠れたとき、隠れた部分を薄く透かして表示するか",
            "ニワトリなど、体が当たり判定より大きい動物で見やすくなる。他のプレイヤーのマークは透かさない"})
    public static boolean animalSpotSeeThrough = true;

    @Config.Comment("[内部] 設定ファイルの移行の済んだ版。書き換えないでください")
    public static int configVersion = 0;

    private WeakSpotConfig() {
    }

    /** 統計画面などから [クライアント] の値を書き換えた後に呼び、weakspot.cfg に保存する（Forge の設定画面と同じ値になる）。 */
    public static void save() {
        ConfigManager.sync(WeakSpotMod.MODID, Config.Type.INSTANCE);
    }

    /**
     * 古い版で作った weakspot.cfg を1回だけ移行する（サーバーの起動時に呼ぶ。preInit の間の ConfigManager.sync は
     * ファイルの値でフィールドを上書きする「読み込み」になるので、そこでは保存できない）。
     * 1: 1.2.2 でキノコを成長の対象にしたので、growthExcludedBlocks からキノコを取り除く（書き戻したら尊重する）。
     */
    public static void migrate() {
        if (configVersion >= 1) {
            return;
        }
        List<String> excluded = new ArrayList<>(Arrays.asList(growthExcludedBlocks));
        if (excluded.removeAll(Arrays.asList("minecraft:brown_mushroom", "minecraft:red_mushroom"))) {
            growthExcludedBlocks = excluded.toArray(new String[0]);
            LogManager.getLogger(WeakSpotMod.MODID).info(
                    "weakspot.cfg: removed mushrooms from growthExcludedBlocks (mushrooms can grow since 1.2.2)");
        }
        configVersion = 1;
        save();
    }

    private static boolean reloadWarned;

    /**
     * weakspot.cfg をファイルから読み直して、フィールドに反映する（/weakspot reload）。読み直せなければ false。
     * 起動後の ConfigManager.sync は、「変わった」（Property#hasChanged）項目だけファイルの値をフィールドへ読み込み、
     * ほかはフィールドの値をファイルへ書き出す。そこで ConfigManager が持っている Configuration を読み直し、
     * すべての項目に「変わった」の印を付けてから sync する（Forge の設定画面と同じ流れ）。
     */
    public static boolean reloadFromFile() {
        Configuration cfg = cachedConfiguration();
        if (cfg == null) {
            return false;
        }
        cfg.load();
        for (String name : cfg.getCategoryNames()) {
            ConfigCategory category = cfg.getCategory(name);
            for (Property property : category.values()) {
                if (property.isList()) {
                    property.set(property.getStringList());
                } else {
                    property.set(property.getString());
                }
            }
        }
        ConfigManager.sync(WeakSpotMod.MODID, Config.Type.INSTANCE);
        return true;
    }

    /** ConfigManager の非公開の CONFIGS（ファイルの絶対パス → Configuration）から weakspot.cfg を取り出す。Forge のクラスなので名前は1つ。 */
    @SuppressWarnings("unchecked")
    private static Configuration cachedConfiguration() {
        try {
            Field field = ConfigManager.class.getDeclaredField("CONFIGS");
            field.setAccessible(true);
            Map<String, Configuration> configs = (Map<String, Configuration>) field.get(null);
            File file = new File(Loader.instance().getConfigDir(), WeakSpotMod.MODID + ".cfg");
            return configs.get(file.getAbsolutePath());
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!reloadWarned) {
                reloadWarned = true;
                LogManager.getLogger(WeakSpotMod.MODID).warn("Could not reload weakspot.cfg", e);
            }
            return null;
        }
    }

    /** 節目と量の数が合っていなければ、ログに警告を出す（足りない分は 0 として扱う）。 */
    public static void warnIfMisconfigured() {
        if (milestoneXp.length != milestones.length || milestoneRepair.length != milestones.length) {
            LogManager.getLogger(WeakSpotMod.MODID).warn(
                    "weakspot.cfg: milestones has {} entries but milestoneXp has {} and milestoneRepair has {}; "
                            + "missing amounts are treated as 0",
                    milestones.length, milestoneXp.length, milestoneRepair.length);
        }
    }

    @Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
    public static final class Sync {

        private Sync() {
        }

        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (WeakSpotMod.MODID.equals(event.getModID())) {
                ConfigManager.sync(WeakSpotMod.MODID, Config.Type.INSTANCE);
                SettingsSync.resendToAll();
                warnIfMisconfigured();
            }
        }
    }
}
