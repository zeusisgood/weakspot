package com.example.weakspot.server;

import com.example.weakspot.PlayerRules;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.config.SyncedSettings;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * ヒット通知の共通の前置き（1.8.6。論理サーバー）: 受け付けるプレイヤーか、前のヒットからの間隔、ログアウトの後片付け。
 * 種類ごとの条件（弓を引いているか、など）は、各 *Hits に残す。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class HitGate {

    /** 通知の間隔はネットワークの揺らぎで縮むので、この tick 数だけ甘く見る。 */
    static final int INTERVAL_JITTER_TICKS = 2;

    /** 種類ごと・プレイヤーごとの最後に受け付けたヒットの tick（ready / mark を使う種類だけ）。 */
    private static final Map<HitKind, Map<UUID, Long>> LAST_HIT = new EnumMap<>(HitKind.class);

    private HitGate() {
    }

    /**
     * サーバーの設定でその種類がオンで（1.8.9 から。SyncedSettings.enabled）、プレイヤーもオンにしていて（一時オフ・
     * 種類ごとのオフ）、観戦モードでない。1.8.6 からクリエイティブも受け付ける。
     */
    static boolean allowed(EntityPlayerMP player, HitKind kind) {
        return SyncedSettings.server().enabled(kind) && ServerSwitches.isEnabled(player, kind)
                && PlayerRules.canUse(player);
    }

    /** 前に受け付けたヒットから、設定の間隔（− 2 tick）がたっているか（記録はしない。受け付けたら mark）。 */
    static boolean ready(EntityPlayerMP player, HitKind kind) {
        Map<UUID, Long> last = LAST_HIT.get(kind);
        Long previous = last == null ? null : last.get(player.getUniqueID());
        return previous == null || intervalOk(player.world.getTotalWorldTime(), previous, kind);
    }

    /** このヒットを受け付けた（次の ready の起点）。 */
    static void mark(EntityPlayerMP player, HitKind kind) {
        LAST_HIT.computeIfAbsent(kind, k -> new HashMap<>()).put(player.getUniqueID(), player.world.getTotalWorldTime());
    }

    /**
     * ヒットを受け付けると決めたあとの共通の処理（1.8.7）: 間隔の起点（mark）→ 種類ごとの数と節目 → 連続ヒット →
     * 近くの他のプレイヤーのヒット音（soundPos から）。このヒットを数えたあとのコンボ数を返す（コンボの掛け数は
     * ComboFactor.factor(accept(...))）。採掘は、短縮した時間と一緒に ServerStats.record で数え、節目も
     * MiningRewards.onMiningHit で見るので、種類ごとの数は数えない。
     */
    static int accept(EntityPlayerMP player, HitKind kind, BlockPos soundPos, int streak) {
        mark(player, kind);
        if (kind != HitKind.MINING) {
            ServerStats.recordKindHit(player, kind);
        }
        int combo = ServerStats.countStreak(player);
        ServerBoostTracker.notifyNearbyPlayers(player, soundPos, streak);
        return combo;
    }

    /** 自分で最後の tick を持つ種類（採掘・右クリック・釣り・移動）向けの判定。間隔は設定の表から（1.8.9）。 */
    static boolean intervalOk(long now, long last, HitKind kind) {
        return now - last >= Math.max(0, SyncedSettings.server().minHitInterval(kind) - INTERVAL_JITTER_TICKS);
    }

    /** 後片付けの理由（1.8.9）。消すものは理由ごとに違うので、各 *Hits の forget が分ける。 */
    enum Leave {
        /** ログアウト（プレイヤーごとの記憶をすべて消す）。 */
        LOGOUT,
        /** 死亡から戻った。 */
        RESPAWN,
        /** ディメンションを移動した。 */
        DIMENSION
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        forgetAll(event.player, Leave.LOGOUT);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        forgetAll(event.player, Leave.RESPAWN);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        forgetAll(event.player, Leave.DIMENSION);
    }

    /**
     * プレイヤーごとの記憶の後片付けは、ここの 1 か所（1.8.6 でログアウト、1.8.9 から死亡・ディメンション移動も）。
     * プレイヤーごとの記憶を持つクラスを足したら、ここに足す（付け忘れを防ぐため）。
     */
    private static void forgetAll(EntityPlayer player, Leave leave) {
        if (leave == Leave.LOGOUT) {
            for (Map<UUID, Long> last : LAST_HIT.values()) {
                last.remove(player.getUniqueID());
            }
        }
        ServerStats.forget(player, leave);
        ServerBoostTracker.forget(player, leave);
        RightClickHits.forget(player, leave);
        GrowthWarnings.forget(player, leave);
        FishingHits.forget(player, leave);
        MoveHits.forget(player, leave);
        MeleeHits.forget(player, leave);
        ThrowHits.forget(player, leave);
        MarkerRelay.forget(player, leave);
        if (leave == Leave.LOGOUT) {
            ServerSwitches.forgetOnLogout(player);
            MiningRewards.forgetOnLogout(player);
            ComboRelay.forgetOnLogout(player);
            VersionCheck.forgetOnLogout(player);
            if (player instanceof EntityPlayerMP) {
                VillagerBreedHints.onLogout((EntityPlayerMP) player);
            }
        }
    }
}
