package com.example.weakspot.config;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.GrowthFilters;
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
            "milestoneXp と milestoneRepair は、この順番に対応する。種類ごとの節目（kindMilestonesEnabled）も同じ数字を使う"})
    public static int[] milestones = {50, 100, 250, 500, 777, 1000, 1500, 2000, 2500, 3000, 4000, 5000, 6000, 7000,
            7777, 8000, 9000, 10000, 15000, 20000, 25000, 30000, 40000, 50000, 60000, 70000, 77777, 80000, 90000,
            100000};

    @Config.Comment({"[サーバー] 節目ごとにもらえる経験値（milestones の順に対応）",
            "数が足りない分は 0 として扱う"})
    public static int[] milestoneXp = {5, 10, 15, 20, 77, 30, 40, 40, 40, 40, 40, 50, 50, 50, 777, 50, 50, 100, 100,
            100, 150, 150, 150, 200, 200, 200, 7777, 200, 200, 1000};

    @Config.Comment({"[サーバー] 節目ごとに回復する、手に持っているツールの耐久（milestones の順に対応。採掘の節目だけ）",
            "数が足りない分は 0 として扱う"})
    public static int[] milestoneRepair = {5, 10, 15, 20, 77, 30, 40, 40, 40, 40, 40, 50, 50, 50, 777, 50, 50, 100,
            100, 100, 150, 150, 150, 200, 200, 200, 7777, 200, 200, 1000};

    @Config.Comment({"[サーバー] milestones の一番大きい数より先は、この数ごとに節目にする（1.7.0）。0 で繰り返さない",
            "ごほうびは milestoneXp / milestoneRepair の最後の値"})
    @Config.RangeInt(min = 0, max = 100000000)
    public static int milestoneRepeatInterval = 50000;

    @Config.Comment({"[サーバー] 採掘以外の種類ごとのヒット数でも、milestones の数字で節目にするか（1.7.0）",
            "ごほうびは経験値（milestoneXp）だけ"})
    public static boolean kindMilestonesEnabled = true;

    @Config.Comment("[サーバー] すべての種類のヒット数の合計でも節目にするか（1.7.0）。数字は totalMilestones")
    public static boolean totalMilestonesEnabled = true;

    @Config.Comment({"[サーバー] 合計の節目にする、すべての種類のヒット数の合計（1.7.0）",
            "totalMilestoneXp は、この順番に対応する"})
    public static int[] totalMilestones = {1000, 5000, 7777, 10000, 25000, 50000, 77777, 100000, 250000, 500000,
            777777, 1000000};

    @Config.Comment({"[サーバー] 合計の節目ごとにもらえる経験値（totalMilestones の順に対応。1.7.0）",
            "数が足りない分は 0 として扱う"})
    public static int[] totalMilestoneXp = {100, 200, 777, 300, 500, 700, 7777, 1000, 1500, 2000, 7777, 5000};

    @Config.Comment({"[サーバー] totalMilestones の一番大きい数より先は、この数ごとに合計の節目にする（1.7.0）。0 で繰り返さない",
            "ごほうびは totalMilestoneXp の最後の値"})
    @Config.RangeInt(min = 0, max = 100000000)
    public static int totalMilestoneRepeatInterval = 500000;

    @Config.Comment({"[サーバー] 作物・苗木の弱点に1回当てるごとに、そのブロックに余分に呼ぶ randomTick の回数",
            "骨粉と違い、明るさや農地の水分などの成長条件は守ったまま速くなる"})
    @Config.RangeInt(min = 0, max = 1000)
    public static int growthTicksPerHit = 5;

    @Config.Comment({"[サーバー] 作物・苗木の弱点に当てても育たないときに、そのプレイヤーのチャットで知らせるか",
            "暗い（作物・茎・苗木で明るさ 9 未満）ときは最初のヒットで、原因が分からないときは growthStuckHits 回続けて変わらなかったときに知らせる"})
    public static boolean growthWarnings = true;

    @Config.Comment("[サーバー] 何回続けて当てても状態が変わらなかったら「何らかの外部要因で育たない」と知らせるか")
    @Config.RangeInt(min = 5, max = 1000)
    public static int growthStuckHits = 30;

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
    public static double mushroomGrowChance = 0.2;

    @Config.Comment({"[サーバー] 成長の弱点の対象に足すブロック（追加リスト）。当てると randomTick を余分に呼んで早める",
            "1行に「登録名」か「登録名[プロパティ=条件,...]」。条件は 0-6（数の範囲）、dry_*（* は任意の文字列）、完全一致",
            "例: somemod:crop[age=0-6]。条件を書かないと、いつでも出す（ネザーウォートは age=0-2、ic2:rubber_wood は state=dry_* が初期の条件。",
            "サトウキビ・サボテンは柱の高さをコードで判定する）。葉・草ブロック・耕地・氷など、成長以外に randomTick を使うブロックは書かない",
            "growthExcludedBlocks に入っているブロックは、ここにあっても対象外になる"})
    public static String[] growthExtraBlocks = {"minecraft:reeds", "minecraft:cactus", "minecraft:nether_wart",
            "ic2:rubber_wood"};

    @Config.Comment({"[サーバー] 機械の弱点に当てたとき、update() を毎tick何倍呼ぶか（4.0 なら毎tick 3回余分に呼ぶ）",
            "複数のプレイヤーが同じ機械を加速しても、足さずに大きいほうだけを使う"})
    @Config.RangeDouble(min = 1.0, max = 100.0)
    public static double machineBoostMultiplier = 4.0;

    @Config.Comment({"[サーバー] 機械の倍率の上限。クライアントにも送る（HUD の「機械 n倍速」）。コンボが続くと machineBoostMultiplier に掛け数を掛ける",
            "（25 で ×1.25、50 で ×1.5、100 で ×2、250 で ×2.5、500 で ×3、1000 で ×4）。その結果をこの値で抑える",
            "機械 Mod の不具合やサーバーの負荷が気になるときに下げる。machineBoostMultiplier より小さいときも、こちらを優先する"})
    @Config.RangeDouble(min = 1.0, max = 100.0)
    public static double machineBoostMaxMultiplier = 16.0;

    @Config.Comment({"[サーバー] 加速中の機械のまわりに、速さに合わせた色の粒子を出すか（近くのプレイヤーに見える。",
            "各自の machineParticlesVisible がオフの人には送らない）。サーバーだけが使う（クライアントには送らない）"})
    public static boolean machineBoostParticles = true;

    @Config.Comment("[サーバー] 機械の加速が続く時間（tick）。machineMinHitIntervalTicks 以上なら、最短の間隔で当て続けると途切れない")
    @Config.RangeInt(min = 0, max = 200)
    public static int machineBoostDurationTicks = 6;

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

    @Config.Comment("[サーバー] 弓の弱点のオン・オフ")
    public static boolean bowWeakSpotEnabled = true;

    @Config.Comment({"[サーバー] 弓の弱点に1回当てるごとに進める、弓の引きの tick 数（バニラの弓は 20 tick で引き切る）",
            "引き切りは超えない。0 で、弓の弱点を無効にする"})
    @Config.RangeInt(min = 0, max = 20)
    public static int bowHitTicks = 5;

    @Config.Comment("[サーバー] 弓のヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
    @Config.RangeInt(min = 0, max = 200)
    public static int bowMinHitIntervalTicks = 4;

    @Config.Comment({"[サーバー] 寝ている間の弱点のオン・オフ（1.6.0）",
            "夜にベッドで寝ている間、寝ている画面にマーカーが出る。クリックで当てると、ワールドの時刻が進む（全員に共通）"})
    public static boolean sleepWeakSpotEnabled = true;

    @Config.Comment({"[サーバー] 寝ている間の弱点に1回当てるごとに進めるワールドの時刻（tick）。次の朝は越えない",
            "サーバーだけが使う（クライアントには送らない）"})
    @Config.RangeInt(min = 0, max = 12000)
    public static int sleepHitTicks = 200;

    @Config.Comment("[サーバー] 寝ている間のヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
    @Config.RangeInt(min = 0, max = 200)
    public static int sleepMinHitIntervalTicks = 6;

    @Config.Comment({"[サーバー] 食事・飲み物の弱点のオン・オフ（1.6.0）",
            "食べている・飲んでいる間、照準の近くに弱点が出る。当てると、食べ終わるまでの時間が縮む"})
    public static boolean eatWeakSpotEnabled = true;

    @Config.Comment("[サーバー] 食事・飲み物の弱点に1回当てるごとに縮める時間（tick）。バニラの食事は 32 tick なので、16 なら 2 ヒットで食べ終わる")
    @Config.RangeInt(min = 0, max = 64)
    public static int eatHitTicks = 16;

    @Config.Comment("[サーバー] 食事・飲み物のヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
    @Config.RangeInt(min = 0, max = 200)
    public static int eatMinHitIntervalTicks = 4;

    @Config.Comment({"[サーバー] 乗り物の弱点のオン・オフ（1.6.0）",
            "馬・豚・トロッコ・ボートに乗って動いている間、照準の近くに弱点が出る。当てると、その乗り物が少しの間速くなる"})
    public static boolean vehicleWeakSpotEnabled = true;

    @Config.Comment("[サーバー] 乗り物の弱点に当てたときの速さの倍率。コンボの掛け数（25 で ×1.25 … 1000 で ×4）を上乗せする")
    @Config.RangeDouble(min = 1.0, max = 100.0)
    public static double vehicleBoostMultiplier = 1.5;

    @Config.Comment({"[サーバー] 乗り物の速さの倍率の上限。0 なら上限なし（初期値。コンボ 1000 で 6 倍になる）",
            "速すぎて困るときに、3.0 などを書く"})
    @Config.RangeDouble(min = 0.0, max = 100.0)
    public static double vehicleBoostMaxMultiplier = 0.0;

    @Config.Comment("[サーバー] 乗り物の加速が続く時間（tick）。ヒットのたびに、この長さに戻す")
    @Config.RangeInt(min = 1, max = 1200)
    public static int vehicleBoostDurationTicks = 40;

    @Config.Comment("[サーバー] 乗り物のヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
    @Config.RangeInt(min = 0, max = 200)
    public static int vehicleMinHitIntervalTicks = 6;

    @Config.Comment({"[サーバー] はしごの弱点のオン・オフ（1.7.0）",
            "はしご・ツタを登り降りしている間、照準の真上か真下に弱点が出る。当てると、登り降りが少しの間速くなる"})
    public static boolean ladderWeakSpotEnabled = true;

    @Config.Comment("[サーバー] はしごの弱点に当てたときの速さの倍率。コンボの掛け数（25 で ×1.25 … 1000 で ×4）を上乗せする")
    @Config.RangeDouble(min = 1.0, max = 100.0)
    public static double ladderBoostMultiplier = 1.5;

    @Config.Comment({"[サーバー] はしごの速さの倍率の上限。0 なら上限なし（初期値）", "速すぎて困るときに、3.0 などを書く"})
    @Config.RangeDouble(min = 0.0, max = 100.0)
    public static double ladderBoostMaxMultiplier = 0.0;

    @Config.Comment("[サーバー] はしごの加速が続く時間（tick）。ヒットのたびに、この長さに戻す")
    @Config.RangeInt(min = 1, max = 1200)
    public static int ladderBoostDurationTicks = 40;

    @Config.Comment("[サーバー] はしごのヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
    @Config.RangeInt(min = 0, max = 200)
    public static int ladderMinHitIntervalTicks = 6;

    @Config.Comment({"[サーバー] エリトラの弱点のオン・オフ（1.7.0）",
            "エリトラで飛んでいる間、照準の近くに弱点が出る。当てると、見ている向きへ一気に飛び出す"})
    public static boolean elytraWeakSpotEnabled = true;

    @Config.Comment({"[サーバー] エリトラの弱点に1回当てるごとに足す速さ（ブロック/tick）。コンボの掛け数を上乗せする",
            "速さの上限はない"})
    @Config.RangeDouble(min = 0.1, max = 10.0)
    public static double elytraBoostPower = 1.5;

    @Config.Comment("[サーバー] エリトラのヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
    @Config.RangeInt(min = 0, max = 200)
    public static int elytraMinHitIntervalTicks = 6;

    @Config.Comment({"[サーバー] エンチャントの弱点のオン・オフ（1.7.0）",
            "エンチャント台に物を置くと、画面にマーカーが出る。クリックで当てると、3 つの候補が引き直される（何も減らない）"})
    public static boolean enchantWeakSpotEnabled = true;

    @Config.Comment("[サーバー] エンチャントのヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
    @Config.RangeInt(min = 0, max = 200)
    public static int enchantMinHitIntervalTicks = 6;

    @Config.Comment({"[サーバー] 収穫の弱点のオン・オフ（1.7.0）",
            "実った作物を素手で右クリックしたままにすると弱点が出る。当てると、収穫して植え直す。",
            "Quark など、右クリックで収穫する Mod と重なるときはオフにする"})
    public static boolean harvestWeakSpotEnabled = true;

    @Config.Comment("[サーバー] 収穫のヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
    @Config.RangeInt(min = 0, max = 200)
    public static int harvestMinHitIntervalTicks = 4;

    @Config.Comment({"[サーバー] 収穫で、コンボの掛け数（25 で ×1.25 … 1000 で ×4）だけ収穫物を増やすか（種は増やさない）",
            "サーバーだけが使う（クライアントには送らない）"})
    public static boolean harvestComboBonus = true;

    @Config.Comment({"[サーバー] 投げる物の弱点のオン・オフ（1.7.0）",
            "エンダーパール・雪玉・卵・ポーション・エンチャントの瓶を持っている間、照準の近くに弱点が出る。",
            "当てるたびに溜まり、次に投げた物が速く遠くへ飛ぶ（持ち替えるまで残り、投げたら使い切る）"})
    public static boolean throwWeakSpotEnabled = true;

    @Config.Comment({"[サーバー] 投げる物の弱点に1回当てるごとに溜まる量（投げる速さの倍率に足す）。コンボの掛け数を上乗せする",
            "溜めの上限はない"})
    @Config.RangeDouble(min = 0.1, max = 10.0)
    public static double throwChargePerHit = 0.5;

    @Config.Comment("[サーバー] 投げる物のヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
    @Config.RangeInt(min = 0, max = 200)
    public static int throwMinHitIntervalTicks = 4;

    @Config.Comment({"[サーバー] 走りの弱点のオン・オフ（1.7.0）",
            "地面を走っている間、照準の真上か真下に弱点が出る。当てると、少しの間速く走れる"})
    public static boolean sprintWeakSpotEnabled = true;

    @Config.Comment("[サーバー] 走りの弱点に当てたときの速さの倍率。コンボの掛け数（25 で ×1.25 … 1000 で ×4）を上乗せする")
    @Config.RangeDouble(min = 1.0, max = 100.0)
    public static double sprintBoostMultiplier = 1.5;

    @Config.Comment({"[サーバー] 走りの速さの倍率の上限。0 なら上限なし（初期値）", "速すぎて困るときに、3.0 などを書く"})
    @Config.RangeDouble(min = 0.0, max = 100.0)
    public static double sprintBoostMaxMultiplier = 0.0;

    @Config.Comment("[サーバー] 走りの加速が続く時間（tick）。ヒットのたびに、この長さに戻す")
    @Config.RangeInt(min = 1, max = 1200)
    public static int sprintBoostDurationTicks = 40;

    @Config.Comment("[サーバー] 走りのヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
    @Config.RangeInt(min = 0, max = 200)
    public static int sprintMinHitIntervalTicks = 6;

    @Config.Comment({"[サーバー] 近接の弱点のオン・オフ",
            "敵に出た弱点を、攻撃のゲージが溜まった状態で殴ると、クリティカルヒット（ジャンプ攻撃と同じ 1.5 倍）になる"})
    public static boolean meleeWeakSpotEnabled = true;

    @Config.Comment("[サーバー] 近接のヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
    @Config.RangeInt(min = 0, max = 200)
    public static int meleeMinHitIntervalTicks = 4;

    @Config.Comment({"[サーバー] 近接の弱点の大きさの倍率。weakSpotRadiusRatio と weakSpotMinRadius に掛ける",
            "上限（weakSpotMaxRadiusRatio）には掛けない（弱点が面からはみ出さないように）。ほかの種類の弱点は変わらない"})
    @Config.RangeDouble(min = 1.0, max = 3.0)
    public static double meleeWeakSpotScale = 1.5;

    @Config.Comment({"[サーバー] 近接の弱点のクリティカル何回ごとに、手に持っている物の耐久を回復するか。0 で回復しない",
            "余りはログアウトまで持ち越す。サーバーだけが使う（クライアントには送らない）"})
    @Config.RangeInt(min = 0, max = 100000)
    public static int critsPerRepair = 5;

    @Config.Comment("[サーバー] critsPerRepair ごとに回復する耐久。サーバーだけが使う（クライアントには送らない）")
    @Config.RangeInt(min = 0, max = 100000)
    public static int critRepairPerStep = 1;

    @Config.Comment({"[サーバー] 初めてログインしたプレイヤーに、遊び方のガイドの本を渡すか（1人1回）",
            "統計画面（K キー）の「ガイド」ボタンでも読める。サーバーだけが使う（クライアントには送らない）"})
    public static boolean giveGuideBook = true;

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

    @Config.Comment("[クライアント] コンボが 10、25、50、100、250、500、1000（以降 1000 ごと）に達したときの演出（強調音・光・花火・タイトル）")
    public static boolean comboMilestoneEffects = true;

    @Config.Comment({"[クライアント] 弱点が移動するときの演出（古い位置から素早く動き、残像を残す）",
            "見た目だけで、当たり判定は移動先で即時。オフにすると、その場で切り替わる。他のプレイヤーのマークにも効く"})
    public static boolean weakSpotTrailEnabled = true;

    @Config.Comment({"[クライアント] 加速中の機械のまわりの色の粒子を、自分の画面に出すか（1.6.0）",
            "自分の機械の粒子も、ほかのプレイヤーの機械の粒子も、同じ設定で消える"})
    public static boolean machineParticlesVisible = true;

    @Config.Comment({"[クライアント] 掘っているブロックの残りの耐久を、面の下の余白に緑のバーで表示するか",
            "自分が左クリックの長押しで掘っている、弱点が出るブロックだけに出る"})
    public static boolean blockHealthBarEnabled = true;

    @Config.Comment({"[クライアント] 近くのほかのプレイヤーのコンボ（10 以上）を、頭の上に「n HIT」と表示するか（1.6.0）"})
    public static boolean othersComboDisplay = true;

    @Config.Comment({"[クライアント] かまど・醸造台・スポナーの上に、進み具合（黄）と燃料（橙）のバーを表示するか（1.6.0）",
            "機械の弱点を出している間（しゃがんで素手で右クリックを押しっぱなしにしている間）だけ出る"})
    public static boolean machineBarEnabled = true;

    @Config.Comment({"[クライアント] 作物の足元に、成長の進み具合を黄色のバーで表示するか",
            "成長の弱点を出している間（素手で右クリックを押しっぱなしにしている間）だけ出る"})
    public static boolean growthBarEnabled = true;

    @Config.Comment({"[クライアント] 他のプレイヤーの弱点マークの形",
            "CIRCLE（塗りつぶした円）, RING（中抜きの輪）, DIAMOND（ひし形）, SQUARE（四角）。自分のマークは円のまま"})
    public static MarkerShape otherMarkerShape = MarkerShape.RING;

    @Config.Comment({"[クライアント] 自分の弱点のオン・オフ（HOME キーで切り替わる。操作設定で変えられる）",
            "オフの間は、自分の弱点が出ず、通常の遊び方になる。他のプレイヤーのマークは見える"})
    public static boolean weakSpotsEnabled = true;

    @Config.Comment({"[クライアント] 自分の動物・敵の弱点が体に隠れたとき、隠れた部分を薄く透かして表示するか",
            "ニワトリやゾンビの腕など、体が当たり判定より大きいときに見やすくなる。他のプレイヤーのマークは透かさない"})
    public static boolean animalSpotSeeThrough = true;

    @Config.Comment({"[クライアント] 弓を引いている間、照準の下に引き具合のゲージを表示するか",
            "弓の弱点や、弱点の一時オフ（HOME キー）に関係なく出る"})
    public static boolean bowDrawBarEnabled = true;

    @Config.Comment({"[クライアント] 乗り物を加速している間、照準の上に残り時間のゲージ（水色）を表示するか（1.6.0）"})
    public static boolean vehicleBoostBarEnabled = true;

    @Config.Comment({"[クライアント] はしごを加速している間、照準の上に残り時間のゲージ（茶色）を表示するか（1.7.0）"})
    public static boolean ladderBoostBarEnabled = true;

    @Config.Comment({"[クライアント] 投げる物の溜めのゲージ（青緑）を、照準の下に表示するか（1.7.0）"})
    public static boolean throwChargeBarEnabled = true;

    @Config.Comment({"[クライアント] 走りを加速している間、照準の上に残り時間のゲージ（赤）を表示するか（1.7.0）"})
    public static boolean sprintBoostBarEnabled = true;

    @Config.Comment({"[クライアント] 自分でオフにした弱点の種類（1.7.0。統計画面の「弱点」タブで変えられる）",
            "1 行に 1 つ、mining, growth, machine, animal, fishing, bow, melee, vehicle, eat, sleep, ladder, elytra,",
            "enchant, harvest, throw, sprint のどれか。オフの種類は弱点が出ず、バニラの動きになる"})
    public static String[] disabledKinds = {};

    @Config.Comment({"[クライアント] 自分の弱点の色（1.7.0）。1 行に「種類=#RRGGBB」（例: harvest=#FF3DCB）",
            "書いていない種類は初期値の色。統計画面の「弱点」タブで変えられる"})
    public static String[] myMarkerColors = {};

    @Config.Comment({"[クライアント] 自分の弱点の形（1.7.0）。1 行に「種類=形」（例: bow=diamond）",
            "形は circle, ring, diamond, square。書いていない種類は円。統計画面の「弱点」タブで変えられる"})
    public static String[] myMarkerShapes = {};

    @Config.Comment("[クライアント] 版が変わって初めてワールドに入ったときに、チャットに更新のお知らせを出すか（1.7.1）")
    public static boolean showUpdateNotes = true;

    @Config.Comment("[クライアント] 最後に更新のお知らせを見た版（1.7.1）。Mod が書き換えます。書き換えないでください")
    public static String lastSeenVersion = "";

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
     * 2: 1.2.4 で mushroomGrowChance の初期値を 0.2 にしたので、古い初期値 0.1 のままなら 0.2 にする（ほかの値は残す）。
     * 3: 1.3.7 で IC2 のゴムの木を成長の対象にしたので、growthExtraBlocks に ic2:rubber_wood を足す（書き戻したら尊重する）。
     * 4: 1.4.2 で machineBoostDurationTicks の初期値を 6 にしたので、古い初期値 4 のままなら 6 にする（ほかの値は残す）。
     * 5: 1.7.0 で節目の数字を細かくしたので、milestones / milestoneXp / milestoneRepair が古い初期値のままなら、
     *    新しい初期値にする（どれかを書き換えてあれば、3 つとも残す）。
     */
    public static void migrate() {
        if (configVersion >= 5) {
            return;
        }
        if (configVersion < 1) {
            List<String> excluded = new ArrayList<>(Arrays.asList(growthExcludedBlocks));
            if (excluded.removeAll(Arrays.asList("minecraft:brown_mushroom", "minecraft:red_mushroom"))) {
                growthExcludedBlocks = excluded.toArray(new String[0]);
                LogManager.getLogger(WeakSpotMod.MODID).info(
                        "weakspot.cfg: removed mushrooms from growthExcludedBlocks (mushrooms can grow since 1.2.2)");
            }
        }
        if (configVersion < 2 && mushroomGrowChance == 0.1) {
            mushroomGrowChance = 0.2;
            LogManager.getLogger(WeakSpotMod.MODID).info(
                    "weakspot.cfg: changed mushroomGrowChance from the old default 0.1 to 0.2 (default since 1.2.4)");
        }
        if (configVersion < 3) {
            List<String> extra = new ArrayList<>(Arrays.asList(growthExtraBlocks));
            if (!extra.contains("ic2:rubber_wood")) {
                extra.add("ic2:rubber_wood");
                growthExtraBlocks = extra.toArray(new String[0]);
                LogManager.getLogger(WeakSpotMod.MODID).info(
                        "weakspot.cfg: added ic2:rubber_wood to growthExtraBlocks (IC2 rubber wood resin holes since 1.3.7)");
            }
        }
        if (configVersion < 4 && machineBoostDurationTicks == 4) {
            machineBoostDurationTicks = 6;
            LogManager.getLogger(WeakSpotMod.MODID).info(
                    "weakspot.cfg: changed machineBoostDurationTicks from the old default 4 to 6 (default since 1.4.2)");
        }
        if (configVersion < 5 && Arrays.equals(milestones, OLD_MILESTONES) && Arrays.equals(milestoneXp, OLD_MILESTONE_XP)
                && Arrays.equals(milestoneRepair, OLD_MILESTONE_XP)) {
            milestones = NEW_MILESTONES.clone();
            milestoneXp = NEW_MILESTONE_XP.clone();
            milestoneRepair = NEW_MILESTONE_XP.clone();
            LogManager.getLogger(WeakSpotMod.MODID).info(
                    "weakspot.cfg: replaced the old default milestones (100, 777, 1000, 10000) with the finer ones (since 1.7.0)");
        }
        configVersion = 5;
        save();
    }

    /** 1.6.x までの節目の初期値（configVersion 5 の移行で使う）。milestoneRepair も milestoneXp と同じ値だった。 */
    private static final int[] OLD_MILESTONES = {100, 777, 1000, 10000};
    private static final int[] OLD_MILESTONE_XP = {10, 77, 30, 100};
    /** 1.7.0 の節目の初期値（フィールドの初期値と同じ。移行で使う）。 */
    private static final int[] NEW_MILESTONES = milestones.clone();
    private static final int[] NEW_MILESTONE_XP = milestoneXp.clone();

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
        for (String entry : GrowthFilters.parse(Arrays.asList(growthExtraBlocks)).invalid()) {
            LogManager.getLogger(WeakSpotMod.MODID).warn(
                    "weakspot.cfg: ignored an unreadable growthExtraBlocks entry: \"{}\" "
                            + "(write modid:block or modid:block[property=pattern,...])", entry);
        }
        if (milestoneXp.length != milestones.length || milestoneRepair.length != milestones.length) {
            LogManager.getLogger(WeakSpotMod.MODID).warn(
                    "weakspot.cfg: milestones has {} entries but milestoneXp has {} and milestoneRepair has {}; "
                            + "missing amounts are treated as 0",
                    milestones.length, milestoneXp.length, milestoneRepair.length);
        }
        if (totalMilestoneXp.length != totalMilestones.length) {
            LogManager.getLogger(WeakSpotMod.MODID).warn(
                    "weakspot.cfg: totalMilestones has {} entries but totalMilestoneXp has {}; missing amounts are treated as 0",
                    totalMilestones.length, totalMilestoneXp.length);
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
