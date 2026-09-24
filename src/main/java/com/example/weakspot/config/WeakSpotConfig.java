package com.example.weakspot.config;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.server.SettingsSync;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

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

    @Config.Comment({"[クライアント] 他のプレイヤーのヒット音の音量（0 で聞こえなくなる）",
            "バニラの「プレイヤー」音量も掛かる"})
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double othersHitVolume = 0.4;

    @Config.Comment({"[クライアント] 他のプレイヤーのヒット音に使うノートブロックの楽器",
            "XYLOPHONE, CHIME, BELL, FLUTE, GUITAR, HARP, BASS, HAT, SNARE, BASEDRUM, PLING（PLING は自分のヒット音と同じ）"})
    public static OtherHitSound othersHitSound = OtherHitSound.XYLOPHONE;

    private WeakSpotConfig() {
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
            }
        }
    }
}
