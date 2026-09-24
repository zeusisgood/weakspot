package com.example.weakspot.config;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.server.SettingsSync;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
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
            "IGrowable を持つブロックのうち、作物・苗木ではないもの（草ブロック、草、キノコなど）を初期値で外している"})
    public static String[] growthExcludedBlocks = {
            "minecraft:grass", "minecraft:tallgrass", "minecraft:double_plant",
            "minecraft:brown_mushroom", "minecraft:red_mushroom"};

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

    private WeakSpotConfig() {
    }

    /** 統計画面などから [クライアント] の値を書き換えた後に呼び、weakspot.cfg に保存する（Forge の設定画面と同じ値になる）。 */
    public static void save() {
        ConfigManager.sync(WeakSpotMod.MODID, Config.Type.INSTANCE);
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
