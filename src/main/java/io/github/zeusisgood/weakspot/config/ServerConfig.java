package io.github.zeusisgood.weakspot.config;

import net.minecraftforge.common.config.Config;

/**
 * [サーバー] の設定（weakspot.cfg の server）。サーバーの値が正で、クライアントが使うものは SyncedSettings で送る。
 * Forge の @Config の決まりで、入れ子のカテゴリの項目は static ではないインスタンスのフィールドにする。
 */
public final class ServerConfig {

    @Config.Comment("共通")
    public final General general = new General();

    @Config.Comment("採掘")
    public final Mining mining = new Mining();

    @Config.Comment("道具の修理")
    public final Repair repair = new Repair();

    @Config.Comment("節目")
    public final Milestones milestones = new Milestones();

    @Config.Comment("成長")
    public final Growth growth = new Growth();

    @Config.Comment("機械")
    public final Machine machine = new Machine();

    @Config.Comment("動物")
    public final Animal animal = new Animal();

    @Config.Comment("釣り")
    public final Fishing fishing = new Fishing();

    @Config.Comment("弓")
    public final Bow bow = new Bow();

    @Config.Comment("近接")
    public final Melee melee = new Melee();

    @Config.Comment("睡眠")
    public final Sleep sleep = new Sleep();

    @Config.Comment("飲食")
    public final Eat eat = new Eat();

    @Config.Comment("乗り物")
    public final Vehicle vehicle = new Vehicle();

    @Config.Comment("はしご")
    public final Ladder ladder = new Ladder();

    @Config.Comment("エリトラ")
    public final Elytra elytra = new Elytra();

    @Config.Comment("エンチャント")
    public final Enchant enchant = new Enchant();

    @Config.Comment("収穫")
    public final Harvest harvest = new Harvest();

    @Config.Name("throw")
    @Config.LangKey("weakspot.server.throw")
    @Config.Comment("投擲物")
    public final Throw throwing = new Throw();

    @Config.Comment("ダッシュ")
    public final Sprint sprint = new Sprint();

    @Config.Comment("ネザーゲート")
    public final Portal portal = new Portal();

    @Config.Comment("泳ぎ")
    public final Swim swim = new Swim();

    /** 共通（server.general） */
    public static final class General {

        @Config.Comment({"[サーバー] 初めてログインしたプレイヤーに、遊び方のガイドの本を渡すか（1人1回）",
                "統計画面（K キー）の「ガイド」ボタンでも読める。サーバーだけが使う（クライアントには送らない）"})
        public boolean giveGuideBook = true;

        @Config.Comment({"[サーバー] 他のプレイヤーの弱点マークを転送する範囲（ブロック）。マークからこの距離以内のプレイヤーにだけ見える",
                "0 で転送しない（他のプレイヤーのマークは見えなくなる）"})
        @Config.RangeDouble(min = 0.0, max = 256.0)
        public double markerShareRange = 16;

        @Config.Comment("[サーバー] 弱点マークの状態を、1プレイヤーあたり送る最小間隔（tick）。通信の多さを抑える")
        @Config.RangeInt(min = 0, max = 200)
        public int markerSendMinIntervalTicks = 2;

        General() {
        }
    }

    /** 採掘（server.mining） */
    public static final class Mining {

        @Config.Comment({"[サーバー] 採掘の弱点のオン・オフ",
                "オフにすると、全員の採掘の弱点が出ない（バニラの採掘に戻る）"})
        public boolean miningWeakSpotEnabled = true;

        @Config.Comment({"[サーバー] 採掘のコンボ倍率のオン・オフ",
                "オンなら、1 回のヒットで進む量にコンボの掛け数（25 で ×1.25 … 300 で ×4、その先も 100 ごとに +0.5）を掛ける"})
        public boolean miningComboBonus = true;

        @Config.Comment("[サーバー] ヒット時の破壊速度の倍率")
        @Config.RangeDouble(min = 1.0, max = 100.0)
        public double boostMultiplier = 4.0;

        @Config.Comment("[サーバー] 倍率を掛ける時間（tick）。1回のヒットで通常の (倍率-1)×この値 tick 分だけ進む")
        @Config.RangeInt(min = 1, max = 200)
        public int boostDurationTicks = 4;

        @Config.Comment("[サーバー] 弱点の半径（面の短い辺に対する比率）")
        @Config.RangeDouble(min = 0.02, max = 0.5)
        public double weakSpotRadiusRatio = 0.14;

        @Config.Comment({"[サーバー] 弱点の外周とブロック面の縁の間に空ける最小の距離（ブロック）",
                "大きくすると弱点が面の中央に寄る。大きくしすぎると minMoveDistance だけ離れた場所が取れず、移動距離が短くなる"})
        @Config.RangeDouble(min = 0.0, max = 0.5)
        public double edgeMargin = 0.1;

        @Config.Comment("[サーバー] ヒット後に弱点が移動する最小距離（ブロック）")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double minMoveDistance = 0.4;

        @Config.Comment({"[サーバー] 弱点の半径の下限（ブロック）。ボタンなどの小さい面でも当てやすくする",
                "上限（weakSpotMaxRadiusRatio）と食い違うときは上限を優先する"})
        @Config.RangeDouble(min = 0.0, max = 0.5)
        public double weakSpotMinRadius = 0.08;

        @Config.Comment("[サーバー] 弱点の半径の上限（面の短い辺に対する比率）。小さい面で弱点が面からはみ出さないようにする")
        @Config.RangeDouble(min = 0.05, max = 0.5)
        public double weakSpotMaxRadiusRatio = 0.35;

        @Config.Comment({"[サーバー] 弱点を出す面の、短い辺の最小の長さ（ブロック）。これより小さい面（カーペットの側面など）には弱点を出さない",
                "0 で、どんな小さい面にも出す"})
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double minFaceSize = 0.15;

        @Config.Comment("[サーバー] 長押しをやめた後に弱点が残る時間（tick）")
        @Config.RangeInt(min = 0, max = 1200)
        public int lingerTicks = 40;

        @Config.Comment("[サーバー] ヒット通知を受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
        @Config.RangeInt(min = 0, max = 200)
        public int minHitIntervalTicks = 6;

        Mining() {
        }
    }

    /** 道具の修理（server.repair） */
    public static final class Repair {

        @Config.Comment({"[サーバー] ブロックを壊したとき、そのブロックで当てた採掘ヒットを数え、この数ごとに手に持っているツールの耐久を回復する",
                "壊さずにやめたブロックのヒットは数えない。余りは次に壊すブロックへ持ち越す。0 で回復しない"})
        @Config.RangeInt(min = 0, max = 100000)
        public int hitsPerRepair = 5;

        @Config.Comment("[サーバー] hitsPerRepair ごとに回復する耐久")
        @Config.RangeInt(min = 0, max = 100000)
        public int repairPerStep = 1;

        @Config.Comment({"[サーバー] 1回の破壊で回復する耐久の上限。上限で回復できなかった分は持ち越さない",
                "0 で耐久回復（節目の報酬を除く）が無効になる"})
        @Config.RangeInt(min = 0, max = 100000)
        public int maxRepairPerBreak = 1;

        Repair() {
        }
    }

    /** 節目（server.milestones） */
    public static final class Milestones {

        @Config.Comment({"[サーバー] 節目にする採掘ヒットの累計。達した瞬間に1回だけ、祝いの演出と報酬が出る",
                "milestoneXp と milestoneRepair は、この順番に対応する。種類ごとの節目（kindMilestonesEnabled）も同じ数字を使う"})
        public int[] milestones = {50, 100, 250, 500, 777, 1000, 1500, 2000, 2500, 3000, 4000, 5000, 6000, 7000,
                7777, 8000, 9000, 10000, 15000, 20000, 25000, 30000, 40000, 50000, 60000, 70000, 77777, 80000, 90000,
                100000};

        @Config.Comment({"[サーバー] 節目ごとにもらえる経験値（milestones の順に対応）",
                "数が足りない分は 0 として扱う"})
        public int[] milestoneXp = {5, 10, 15, 20, 77, 30, 40, 40, 40, 40, 40, 50, 50, 50, 777, 50, 50, 100, 100,
                100, 150, 150, 150, 200, 200, 200, 7777, 200, 200, 1000};

        @Config.Comment({"[サーバー] 節目ごとに回復する、手に持っているツールの耐久（milestones の順に対応。採掘の節目だけ）",
                "数が足りない分は 0 として扱う"})
        public int[] milestoneRepair = {5, 10, 15, 20, 77, 30, 40, 40, 40, 40, 40, 50, 50, 50, 777, 50, 50, 100,
                100, 100, 150, 150, 150, 200, 200, 200, 7777, 200, 200, 1000};

        @Config.Comment({"[サーバー] milestones の一番大きい数より先は、この数ごとに節目にする。0 で繰り返さない",
                "ごほうびは milestoneXp / milestoneRepair の最後の値"})
        @Config.RangeInt(min = 0, max = 100000000)
        public int milestoneRepeatInterval = 50000;

        @Config.Comment({"[サーバー] 採掘以外の種類ごとのヒット数でも、milestones の数字で節目にするか",
                "ごほうびは経験値（milestoneXp）だけ"})
        public boolean kindMilestonesEnabled = true;

        @Config.Comment("[サーバー] すべての種類のヒット数の合計でも節目にするか。数字は totalMilestones")
        public boolean totalMilestonesEnabled = true;

        @Config.Comment({"[サーバー] 合計の節目にする、すべての種類のヒット数の合計",
                "totalMilestoneXp は、この順番に対応する"})
        public int[] totalMilestones = {1000, 5000, 7777, 10000, 25000, 50000, 77777, 100000, 250000, 500000,
                777777, 1000000};

        @Config.Comment({"[サーバー] 合計の節目ごとにもらえる経験値（totalMilestones の順に対応）",
                "数が足りない分は 0 として扱う"})
        public int[] totalMilestoneXp = {100, 200, 777, 300, 500, 700, 7777, 1000, 1500, 2000, 7777, 5000};

        @Config.Comment({"[サーバー] totalMilestones の一番大きい数より先は、この数ごとに合計の節目にする。0 で繰り返さない",
                "ごほうびは totalMilestoneXp の最後の値"})
        @Config.RangeInt(min = 0, max = 100000000)
        public int totalMilestoneRepeatInterval = 500000;

        Milestones() {
        }
    }

    /** 成長（server.growth） */
    public static final class Growth {

        @Config.Comment({"[サーバー] 作物・苗木の成長の弱点のオン・オフ",
                "オフにすると、全員の成長の弱点が出ず、素手の右クリックは通常の動作になる"})
        public boolean growthWeakSpotEnabled = true;

        @Config.Comment({"[サーバー] 作物・苗木の弱点に1回当てるごとに、そのブロックに余分に呼ぶ randomTick の回数",
                "骨粉と違い、明るさや農地の水分などの成長条件は守ったまま速くなる"})
        @Config.RangeInt(min = 0, max = 1000)
        public int growthTicksPerHit = 5;

        @Config.Comment({"[サーバー] 作物・苗木の弱点に当てても育たないときに、そのプレイヤーのチャットで知らせるか",
                "暗い（作物・茎・苗木で明るさ 9 未満）ときは最初のヒットで、原因が分からないときは growthStuckHits 回続けて変わらなかったときに知らせる"})
        public boolean growthWarnings = true;

        @Config.Comment("[サーバー] 何回続けて当てても状態が変わらなかったら「何らかの外部要因で育たない」と知らせるか")
        @Config.RangeInt(min = 5, max = 1000)
        public int growthStuckHits = 30;

        @Config.Comment("[サーバー] 作物・苗木のヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
        @Config.RangeInt(min = 0, max = 200)
        public int growthMinHitIntervalTicks = 6;

        @Config.Comment("[サーバー] 作物・苗木の弱点の最小の半径（ブロック）。小さい面でも当てやすくする")
        @Config.RangeDouble(min = 0.0, max = 0.5)
        public double growthMinRadius = 0.08;

        @Config.Comment({"[サーバー] 成長の弱点を出さないブロックの登録名（サトウキビ・サボテン・ネザーウォートにも効く）",
                "IGrowable を持つブロックのうち、作物・苗木ではないもの（草ブロック、草など）を初期値で外している"})
        public String[] growthExcludedBlocks = {
                "minecraft:grass", "minecraft:tallgrass", "minecraft:double_plant"};

        @Config.Comment({"[サーバー] 成長の弱点の対象に足すブロック（追加リスト）。当てると randomTick を余分に呼んで早める",
                "1行に「登録名」か「登録名[プロパティ=条件,...]」。条件は 0-6（数の範囲）、dry_*（* は任意の文字列）、完全一致",
                "例: somemod:crop[age=0-6]。条件を書かないと、いつでも出す（ネザーウォートは age=0-2、ic2:rubber_wood は state=dry_* が初期の条件。",
                "サトウキビ・サボテンは柱の高さをコードで判定する）。葉・草ブロック・耕地・氷など、成長以外に randomTick を使うブロックは書かない",
                "growthExcludedBlocks に入っているブロックは、ここにあっても対象外になる"})
        public String[] growthExtraBlocks = {"minecraft:reeds", "minecraft:cactus", "minecraft:nether_wart",
                "ic2:rubber_wood"};

        @Config.Comment({"[サーバー] キノコへの成長ヒット1回で、巨大キノコに育てようとする確率（0〜1。骨粉1回と同じ処理）",
                "育つ条件（下のブロック、上の空き）はバニラのまま。育たなければキノコが残る"})
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double mushroomGrowChance = 0.2;

        Growth() {
        }
    }

    /** 機械（server.machine） */
    public static final class Machine {

        @Config.Comment({"[サーバー] 機械の弱点のオン・オフ",
                "オフにすると、全員の機械の弱点が出ず、しゃがんで素手の右クリックは通常の動作（GUI が開く）になる"})
        public boolean machineWeakSpotEnabled = true;

        @Config.Comment({"[サーバー] 機械の弱点に当てたとき、update() を毎tick何倍呼ぶか（4.0 なら毎tick 3回余分に呼ぶ）",
                "複数のプレイヤーが同じ機械を加速しても、足さずに大きいほうだけを使う"})
        @Config.RangeDouble(min = 1.0, max = 100.0)
        public double machineBoostMultiplier = 4.0;

        @Config.Comment({"[サーバー] 機械の倍率の上限。クライアントにも送る（HUD の「機械 n倍速」）。コンボが続くと machineBoostMultiplier に掛け数を掛ける",
                "（25 で ×1.25、50 で ×1.5、100 で ×2、150 で ×2.5、200 で ×3、250 で ×3.5、300 で ×4、その先も 100 ごとに +0.5）。その結果をこの値で抑える",
                "機械 Mod の不具合やサーバーの負荷が気になるときに下げる。machineBoostMultiplier より小さいときも、こちらを優先する"})
        @Config.RangeDouble(min = 1.0, max = 100.0)
        public double machineBoostMaxMultiplier = 16.0;

        @Config.Comment({"[サーバー] 加速中の機械のまわりに、速さに合わせた色の粒子を出すか（近くのプレイヤーに見える。",
                "各自の machineParticlesVisible がオフの人には送らない）。サーバーだけが使う（クライアントには送らない）"})
        public boolean machineBoostParticles = true;

        @Config.Comment("[サーバー] 機械の加速が続く時間（tick）。machineMinHitIntervalTicks 以上なら、最短の間隔で当て続けると途切れない")
        @Config.RangeInt(min = 0, max = 200)
        public int machineBoostDurationTicks = 6;

        @Config.Comment("[サーバー] 機械のヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
        @Config.RangeInt(min = 0, max = 200)
        public int machineMinHitIntervalTicks = 6;

        @Config.Comment({"[サーバー] 機械の加速の対象外にするブロックの登録名。対象外のブロックは、しゃがんで素手で右クリックしても GUI が普通に開く",
                "加速で不具合が出た機械は、ここに登録名を足す"})
        public String[] excludedBlocks = {
                "minecraft:chest", "minecraft:trapped_chest", "minecraft:ender_chest",
                "minecraft:enchanting_table", "minecraft:beacon",
                "minecraft:white_shulker_box", "minecraft:orange_shulker_box", "minecraft:magenta_shulker_box",
                "minecraft:light_blue_shulker_box", "minecraft:yellow_shulker_box", "minecraft:lime_shulker_box",
                "minecraft:pink_shulker_box", "minecraft:gray_shulker_box", "minecraft:silver_shulker_box",
                "minecraft:cyan_shulker_box", "minecraft:purple_shulker_box", "minecraft:blue_shulker_box",
                "minecraft:brown_shulker_box", "minecraft:green_shulker_box", "minecraft:red_shulker_box",
                "minecraft:black_shulker_box"};

        Machine() {
        }
    }

    /** 動物（server.animal） */
    public static final class Animal {

        @Config.Comment({"[サーバー] 動物の弱点のオン・オフ",
                "オフにすると、全員の動物の弱点が出ず、素手の右クリックは通常の動作になる"})
        public boolean animalWeakSpotEnabled = true;

        @Config.Comment("[サーバー] 動物の弱点で、子どもの成長を早めるか")
        public boolean animalBabyEnabled = true;

        @Config.Comment("[サーバー] 動物の弱点で、繁殖の待ち時間を短くするか")
        public boolean animalBreedingEnabled = true;

        @Config.Comment("[サーバー] 羊の弱点で、羊毛の再生を早めるか（草は要らない）")
        public boolean sheepWoolEnabled = true;

        @Config.Comment("[サーバー] ニワトリの弱点で、次の卵までの時間を短くするか")
        public boolean chickenEggEnabled = true;

        @Config.Comment("[サーバー] 村人の弱点で、ロックされた取引の上限をリセットするか")
        public boolean villagerTradeResetEnabled = true;

        @Config.Comment({"[サーバー] 子どもが大人になるまでの目安のヒット数（1ヒット = 24000 tick ÷ この数）",
                "0 以下で、子どもの成長の加速を無効にする"})
        @Config.RangeInt(min = 0, max = 100000)
        public int animalBabyHits = 40;

        @Config.Comment({"[サーバー] 繁殖の待ち時間が終わるまでの目安のヒット数（1ヒット = 6000 tick ÷ この数）",
                "0 以下で、繁殖の待ち時間の短縮を無効にする"})
        @Config.RangeInt(min = 0, max = 100000)
        public int animalBreedingHits = 10;

        @Config.Comment({"[サーバー] 毛を刈られた羊の毛が生えるまでのヒット数", "0 以下で無効にする"})
        @Config.RangeInt(min = 0, max = 100000)
        public int sheepWoolHits = 10;

        @Config.Comment({"[サーバー] 次の卵までの目安のヒット数（1ヒット = 平均 9000 tick ÷ この数）", "0 以下で無効にする"})
        @Config.RangeInt(min = 0, max = 100000)
        public int chickenEggHits = 15;

        @Config.Comment({"[サーバー] 村人の、ロックされた取引の上限がリセットされるまでのヒット数", "0 以下で無効にする"})
        @Config.RangeInt(min = 0, max = 100000)
        public int villagerTradeResetHits = 10;

        @Config.Comment("[サーバー] 動物のヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
        @Config.RangeInt(min = 0, max = 200)
        public int animalMinHitIntervalTicks = 6;

        @Config.Comment({"[サーバー] 動物の弱点の対象外にするエンティティの ID（例: minecraft:cow）",
                "対象外の動物には弱点が出ず、右クリックも通常の動作になる"})
        public String[] animalExcludedEntities = {};

        @Config.Comment({"[サーバー] しゃがみ+素手のときだけ弱点を出す MOD の動物のエンティティの ID",
                "素手の右クリックに別の動作がある動物（乗る、持ち物の画面、座るなど）を足す。バニラの馬・飼いならしたオオカミなどは、コードで判定する"})
        public String[] animalSneakRequiredEntities = {};

        @Config.Comment({"[サーバー] 村人の取引上限のリセットで、新しい取引の段階も解放するか（バニラの補充と同じ）",
                "オフなら、ロックの解除だけ行う。サーバーだけが使う（クライアントには送らない）"})
        public boolean villagerResetUnlocksNewTier = false;

        Animal() {
        }
    }

    /** 釣り（server.fishing） */
    public static final class Fishing {

        @Config.Comment("[サーバー] 釣りの弱点のオン・オフ")
        public boolean fishingWeakSpotEnabled = true;

        @Config.Comment({"[サーバー] 釣りの、最長の待ち時間（600 tick）を 0 にするまでのヒット数（1ヒット = 600 tick ÷ この数）",
                "0 以下で、釣りの弱点を無効にする"})
        @Config.RangeInt(min = 0, max = 600)
        public int fishingHits = 6;

        @Config.Comment("[サーバー] 釣りのヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
        @Config.RangeInt(min = 0, max = 200)
        public int fishingMinHitIntervalTicks = 6;

        Fishing() {
        }
    }

    /** 弓（server.bow） */
    public static final class Bow {

        @Config.Comment("[サーバー] 弓の弱点のオン・オフ")
        public boolean bowWeakSpotEnabled = true;

        @Config.Comment({"[サーバー] 弓の弱点に1回当てるごとに進める、弓の引きの tick 数（バニラの弓は 20 tick で引き切る）",
                "引き切りは超えない。0 で、弓の弱点を無効にする"})
        @Config.RangeInt(min = 0, max = 20)
        public int bowHitTicks = 5;

        @Config.Comment("[サーバー] 弓のヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
        @Config.RangeInt(min = 0, max = 200)
        public int bowMinHitIntervalTicks = 4;

        Bow() {
        }
    }

    /** 近接（server.melee） */
    public static final class Melee {

        @Config.Comment({"[サーバー] 近接の弱点のオン・オフ",
                "剣か斧を持って敵の近くにいる間、照準の左右に弱点が出る。当てるたびに溜まり、次に殴った攻撃が強くなる"})
        public boolean meleeWeakSpotEnabled = true;

        @Config.Comment("[サーバー] 近接のヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
        @Config.RangeInt(min = 0, max = 200)
        public int meleeMinHitIntervalTicks = 4;

        @Config.Comment({"[サーバー] 近接の弱点に1回当てるごとに溜まる量（次の攻撃の倍率に足す）。コンボの掛け数を上乗せする",
                "溜めた攻撃はクリティカル（1.5 倍）になり、さらに (1 + 溜め) 倍になる"})
        @Config.RangeDouble(min = 0.05, max = 10.0)
        public double meleeChargePerHit = 0.25;

        @Config.Comment({"[サーバー] 近接の溜めの上限。0 なら上限なし（初期値）", "強すぎて困るときに、2.0 などを書く"})
        @Config.RangeDouble(min = 0.0, max = 1000.0)
        public double meleeChargeMax = 0.0;

        @Config.Comment({"[サーバー] 近接の溜めた攻撃が何回当たるごとに、手に持っている物の耐久を回復するか。0 で回復しない",
                "余りはログアウトまで持ち越す。サーバーだけが使う（クライアントには送らない）"})
        @Config.RangeInt(min = 0, max = 100000)
        public int critsPerRepair = 5;

        @Config.Comment("[サーバー] critsPerRepair ごとに回復する耐久。サーバーだけが使う（クライアントには送らない）")
        @Config.RangeInt(min = 0, max = 100000)
        public int critRepairPerStep = 1;

        Melee() {
        }
    }

    /** 睡眠（server.sleep） */
    public static final class Sleep {

        @Config.Comment({"[サーバー] 寝ている間の弱点のオン・オフ",
                "夜にベッドで寝ている間、寝ている画面にマーカーが出る。クリックで当てると、ワールドの時刻が進む（全員に共通）"})
        public boolean sleepWeakSpotEnabled = true;

        @Config.Comment({"[サーバー] 寝ている間の弱点に1回当てるごとに進めるワールドの時刻（tick）。次の朝は越えない",
                "サーバーだけが使う（クライアントには送らない）"})
        @Config.RangeInt(min = 0, max = 12000)
        public int sleepHitTicks = 200;

        @Config.Comment("[サーバー] 寝ている間のヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
        @Config.RangeInt(min = 0, max = 200)
        public int sleepMinHitIntervalTicks = 6;

        Sleep() {
        }
    }

    /** 飲食（server.eat） */
    public static final class Eat {

        @Config.Comment({"[サーバー] 飲食の弱点のオン・オフ",
                "食べている・飲んでいる間、照準の近くに弱点が出る。当てると、食べ終わるまでの時間が縮む"})
        public boolean eatWeakSpotEnabled = true;

        @Config.Comment("[サーバー] 飲食の弱点に1回当てるごとに縮める時間（tick）。バニラの食事は 32 tick なので、16 なら 2 ヒットで食べ終わる")
        @Config.RangeInt(min = 0, max = 64)
        public int eatHitTicks = 16;

        @Config.Comment("[サーバー] 飲食のヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
        @Config.RangeInt(min = 0, max = 200)
        public int eatMinHitIntervalTicks = 4;

        Eat() {
        }
    }

    /** 乗り物（server.vehicle） */
    public static final class Vehicle {

        @Config.Comment({"[サーバー] 乗り物の弱点のオン・オフ",
                "馬・豚・トロッコ・ボートに乗って動いている間、照準の近くに弱点が出る。当てると、その乗り物が少しの間速くなる"})
        public boolean vehicleWeakSpotEnabled = true;

        @Config.Comment("[サーバー] 乗り物の弱点に当てたときの速さの倍率。コンボの掛け数（25 で ×1.25 … 300 で ×4、その先も 100 ごとに +0.5）を上乗せする")
        @Config.RangeDouble(min = 1.0, max = 100.0)
        public double vehicleBoostMultiplier = 1.5;

        @Config.Comment({"[サーバー] 乗り物の速さの倍率の上限。0 なら上限なし（初期値。コンボ 300 で 6 倍、その先も上がり続ける）",
                "速すぎて困るときに、3.0 などを書く"})
        @Config.RangeDouble(min = 0.0, max = 100.0)
        public double vehicleBoostMaxMultiplier = 0.0;

        @Config.Comment("[サーバー] 乗り物の加速が続く時間（tick）。ヒットのたびに、この長さに戻す")
        @Config.RangeInt(min = 1, max = 1200)
        public int vehicleBoostDurationTicks = 40;

        @Config.Comment("[サーバー] 乗り物のヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
        @Config.RangeInt(min = 0, max = 200)
        public int vehicleMinHitIntervalTicks = 6;

        Vehicle() {
        }
    }

    /** はしご（server.ladder） */
    public static final class Ladder {

        @Config.Comment({"[サーバー] はしごの弱点のオン・オフ",
                "はしご・ツタを登り降りしている間、照準の真上か真下に弱点が出る。当てると、登り降りが少しの間速くなる"})
        public boolean ladderWeakSpotEnabled = true;

        @Config.Comment("[サーバー] はしごの弱点に当てたときの速さの倍率。コンボの掛け数（25 で ×1.25 … 300 で ×4、その先も 100 ごとに +0.5）を上乗せする")
        @Config.RangeDouble(min = 1.0, max = 100.0)
        public double ladderBoostMultiplier = 1.5;

        @Config.Comment({"[サーバー] はしごの速さの倍率の上限。0 なら上限なし（初期値）", "速すぎて困るときに、3.0 などを書く"})
        @Config.RangeDouble(min = 0.0, max = 100.0)
        public double ladderBoostMaxMultiplier = 0.0;

        @Config.Comment("[サーバー] はしごの加速が続く時間（tick）。ヒットのたびに、この長さに戻す")
        @Config.RangeInt(min = 1, max = 1200)
        public int ladderBoostDurationTicks = 40;

        @Config.Comment("[サーバー] はしごのヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
        @Config.RangeInt(min = 0, max = 200)
        public int ladderMinHitIntervalTicks = 6;

        Ladder() {
        }
    }

    /** エリトラ（server.elytra） */
    public static final class Elytra {

        @Config.Comment({"[サーバー] エリトラの弱点のオン・オフ",
                "エリトラで飛んでいる間、照準の近くに弱点が出る。当てると、見ている向きへ一気に飛び出す"})
        public boolean elytraWeakSpotEnabled = true;

        @Config.Comment({"[サーバー] エリトラの弱点に1回当てるごとに足す速さ（ブロック/tick）。コンボの掛け数を上乗せする",
                "速さの上限はない"})
        @Config.RangeDouble(min = 0.1, max = 10.0)
        public double elytraBoostPower = 1.5;

        @Config.Comment("[サーバー] エリトラのヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
        @Config.RangeInt(min = 0, max = 200)
        public int elytraMinHitIntervalTicks = 6;

        Elytra() {
        }
    }

    /** エンチャント（server.enchant） */
    public static final class Enchant {

        @Config.Comment({"[サーバー] エンチャントの弱点のオン・オフ",
                "エンチャント台に物を置くと、画面にマーカーが出る。クリックで当てると、3 つの候補が引き直される（何も減らない）"})
        public boolean enchantWeakSpotEnabled = true;

        @Config.Comment("[サーバー] エンチャントのヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
        @Config.RangeInt(min = 0, max = 200)
        public int enchantMinHitIntervalTicks = 6;

        Enchant() {
        }
    }

    /** 収穫（server.harvest） */
    public static final class Harvest {

        @Config.Comment({"[サーバー] 収穫の弱点のオン・オフ",
                "実った作物を素手で右クリックしたままにすると弱点が出る。当てると、収穫して植え直す。",
                "Quark など、右クリックで収穫する Mod と重なるときはオフにする"})
        public boolean harvestWeakSpotEnabled = true;

        @Config.Comment("[サーバー] 収穫のヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
        @Config.RangeInt(min = 0, max = 200)
        public int harvestMinHitIntervalTicks = 4;

        @Config.Comment({"[サーバー] 収穫で、コンボの掛け数（25 で ×1.25 … 300 で ×4、その先も 100 ごとに +0.5）だけ収穫物を増やすか（種は増やさない）",
                "サーバーだけが使う（クライアントには送らない）"})
        public boolean harvestComboBonus = true;

        Harvest() {
        }
    }

    /** 投擲物（server.throw） */
    public static final class Throw {

        @Config.Comment({"[サーバー] 投擲物の弱点のオン・オフ",
                "エンダーパール・雪玉・卵・ポーション・エンチャントの瓶を持っている間、照準の近くに弱点が出る。",
                "当てるたびに溜まり、次に投げた物が速く遠くへ飛ぶ（持ち替えるまで残り、投げたら使い切る）"})
        public boolean throwWeakSpotEnabled = true;

        @Config.Comment({"[サーバー] 投擲物の弱点に1回当てるごとに溜まる量（投げる速さの倍率に足す）。コンボの掛け数を上乗せする",
                "溜めの上限はない"})
        @Config.RangeDouble(min = 0.1, max = 10.0)
        public double throwChargePerHit = 0.5;

        @Config.Comment("[サーバー] 投擲物のヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
        @Config.RangeInt(min = 0, max = 200)
        public int throwMinHitIntervalTicks = 4;

        Throw() {
        }
    }

    /** ダッシュ（server.sprint） */
    public static final class Sprint {

        @Config.Comment({"[サーバー] ダッシュの弱点のオン・オフ",
                "地面を走っている間、照準の真上か真下に弱点が出る。当てると、少しの間速く走れる"})
        public boolean sprintWeakSpotEnabled = true;

        @Config.Comment("[サーバー] ダッシュの弱点に当てたときの速さの倍率。コンボの掛け数（25 で ×1.25 … 300 で ×4、その先も 100 ごとに +0.5）を上乗せする")
        @Config.RangeDouble(min = 1.0, max = 100.0)
        public double sprintBoostMultiplier = 1.5;

        @Config.Comment({"[サーバー] ダッシュの速さの倍率の上限。0 なら上限なし（初期値）", "速すぎて困るときに、3.0 などを書く"})
        @Config.RangeDouble(min = 0.0, max = 100.0)
        public double sprintBoostMaxMultiplier = 0.0;

        @Config.Comment("[サーバー] ダッシュの加速が続く時間（tick）。ヒットのたびに、この長さに戻す")
        @Config.RangeInt(min = 1, max = 1200)
        public int sprintBoostDurationTicks = 40;

        @Config.Comment("[サーバー] ダッシュのヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
        @Config.RangeInt(min = 0, max = 200)
        public int sprintMinHitIntervalTicks = 6;

        Sprint() {
        }
    }

    /** ネザーゲート（server.portal） */
    public static final class Portal {

        @Config.Comment({"[サーバー] ネザーゲートの弱点のオン・オフ",
                "ゲートの中に立っている間、照準の近くに弱点が出る。当てると、移動までの待ち時間が縮む"})
        public boolean portalWeakSpotEnabled = true;

        @Config.Comment("[サーバー] ネザーゲートの弱点に1回当てるごとに縮める待ち時間（tick）。コンボの掛け数を上乗せする。バニラの待ち時間は 80 tick")
        @Config.RangeInt(min = 1, max = 200)
        public int portalHitTicks = 20;

        @Config.Comment("[サーバー] ネザーゲートのヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
        @Config.RangeInt(min = 0, max = 200)
        public int portalMinHitIntervalTicks = 4;

        Portal() {
        }
    }

    /** 泳ぎ（server.swim） */
    public static final class Swim {

        @Config.Comment({"[サーバー] 泳ぎの弱点のオン・オフ",
                "オフにすると、全員の泳ぎの弱点が出ない"})
        public boolean swimWeakSpotEnabled = true;

        @Config.Comment({"[サーバー] 泳ぎの弱点に当てたときの、泳ぐ速さの倍率",
                "コンボの掛け数を上乗せする。ボート（乗り物）と同じ値にすると、同じだけ当てればボートのほうが常に速い"})
        @Config.RangeDouble(min = 1.0, max = 100.0)
        public double swimBoostMultiplier = 1.5;

        @Config.Comment("[サーバー] 泳ぐ速さの倍率の上限。0 で上限なし")
        @Config.RangeDouble(min = 0.0, max = 1000.0)
        public double swimBoostMaxMultiplier = 0;

        @Config.Comment("[サーバー] 泳ぎの加速の持続時間（tick）。ヒットのたびにこの長さに戻る")
        @Config.RangeInt(min = 1, max = 1200)
        public int swimBoostDurationTicks = 40;

        @Config.Comment("[サーバー] 泳ぎの弱点に当てた瞬間の突進で進む距離（ブロック、おおよそ）。コンボでは大きくならない")
        @Config.RangeDouble(min = 0.0, max = 10.0)
        public double swimDashDistance = 0.3;

        @Config.Comment("[サーバー] 泳ぎのヒットを受け付ける最小間隔（tick）。クライアントも同じ間隔でヒットを制限する")
        @Config.RangeInt(min = 0, max = 200)
        public int swimMinHitIntervalTicks = 6;

        Swim() {
        }
    }

    ServerConfig() {
    }
}
