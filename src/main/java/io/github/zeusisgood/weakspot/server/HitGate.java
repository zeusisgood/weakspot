package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.PlayerRules;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.HitBudget;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
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
 * ヒット通知の共通の前置き（1.8.6。論理サーバー）: 受け付けるプレイヤーか、ヒットの間隔（HitBudget）、ログアウトの後片付け。
 * 種類ごとの条件（弓を引いているか、など）は、各 *Hits に残す。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class HitGate {

    /** 種類ごと・プレイヤーごとのヒットの間隔（ready / mark を使う種類だけ。1.11.6 から HitBudget）。 */
    private static final Map<HitKind, Map<UUID, HitBudget>> BUDGETS = new EnumMap<>(HitKind.class);

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

    /** 間隔が空いているか（記録はしない。受け付けたら mark）。まとめて届いても HitBudget.BURST 回分までは受け付ける（1.11.6）。 */
    static boolean ready(EntityPlayerMP player, HitKind kind) {
        Map<UUID, HitBudget> budgets = BUDGETS.get(kind);
        HitBudget budget = budgets == null ? null : budgets.get(player.getUniqueID());
        return budget == null || budget.ok(player.world.getTotalWorldTime(), interval(kind));
    }

    /** このヒットを受け付けた（間隔を 1 回分使う）。 */
    static void mark(EntityPlayerMP player, HitKind kind) {
        BUDGETS.computeIfAbsent(kind, k -> new HashMap<>())
                .computeIfAbsent(player.getUniqueID(), id -> new HitBudget())
                .use(player.world.getTotalWorldTime(), interval(kind));
    }

    /**
     * ヒットを受け付けると決めたあとの共通の処理（1.8.7）: 間隔を 1 回分使う（mark）→ 種類ごとの数と節目 → 連続ヒット →
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

    /**
     * 自分で HitBudget を持つ種類（採掘・右クリック・釣り・移動。ブロックや浮きごとなど持ち主が違う）向け: 間隔が空いていれば
     * 1 回分使って true（1.11.6）。間隔は設定の表から（1.8.9）。
     */
    static boolean take(HitBudget budget, long now, HitKind kind) {
        return budget.tryUse(now, interval(kind));
    }

    private static int interval(HitKind kind) {
        return SyncedSettings.server().minHitInterval(kind);
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
            for (Map<UUID, HitBudget> budgets : BUDGETS.values()) {
                budgets.remove(player.getUniqueID());
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
        TargetRounds.forget(player, leave);
        ComboRelay.forget(player, leave);
        if (leave == Leave.LOGOUT) {
            ServerSwitches.forgetOnLogout(player);
            MiningRewards.forgetOnLogout(player);
            VersionCheck.forgetOnLogout(player);
            if (player instanceof EntityPlayerMP) {
                VillagerBreedHints.onLogout((EntityPlayerMP) player);
            }
        }
    }
}
