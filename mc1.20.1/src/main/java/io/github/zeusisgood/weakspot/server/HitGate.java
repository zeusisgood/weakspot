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
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** ヒット通知の共通の前置き（論理サーバー）。種類ごとの条件（弓を引いているか、など）は、各 *Hits に残す。 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class HitGate {

    /** ready / mark を使う種類だけ。 */
    private static final Map<HitKind, Map<UUID, HitBudget>> BUDGETS = new EnumMap<>(HitKind.class);

    private HitGate() {
    }

    /** プレイヤーの一時オフ・種類ごとのオフも見る。観戦モードは受け付けない（クリエイティブは受け付ける）。 */
    static boolean allowed(ServerPlayer player, HitKind kind) {
        return SyncedSettings.server().enabled(kind) && ServerSwitches.isEnabled(player, kind)
                && PlayerRules.canUse(player);
    }

    /** 記録はしない（受け付けたら mark）。 */
    static boolean ready(ServerPlayer player, HitKind kind) {
        Map<UUID, HitBudget> budgets = BUDGETS.get(kind);
        HitBudget budget = budgets == null ? null : budgets.get(player.getUUID());
        return budget == null || budget.ok(player.level().getGameTime(), interval(kind));
    }

    static void mark(ServerPlayer player, HitKind kind) {
        BUDGETS.computeIfAbsent(kind, k -> new HashMap<>())
                .computeIfAbsent(player.getUUID(), id -> new HitBudget())
                .use(player.level().getGameTime(), interval(kind));
    }

    /**
    * このヒットを数えたあとのコンボ数を返す。採掘は、短縮した時間と一緒に ServerStats.record で数え、節目も
    * MiningRewards.onMiningHit で見るので、種類ごとの数は数えない。
    */
    static int accept(ServerPlayer player, HitKind kind, BlockPos soundPos, int streak) {
        mark(player, kind);
        if (kind != HitKind.MINING) {
            ServerStats.recordKindHit(player, kind);
        }
        int combo = ServerStats.countStreak(player);
        ItemRewards.onHit(player, combo);
        ServerBoostTracker.notifyNearbyPlayers(player, soundPos, streak);
        return combo;
    }

    /** 自分で HitBudget を持つ種類（ブロックや浮きごとなど、持ち主が違う）向け。 */
    static boolean take(HitBudget budget, long now, HitKind kind) {
        return budget.tryUse(now, interval(kind));
    }

    private static int interval(HitKind kind) {
        return SyncedSettings.server().minHitInterval(kind);
    }

    /** 消すものは理由ごとに違うので、各 *Hits の forget が分ける。 */
    enum Leave {
        LOGOUT,
        RESPAWN,
        DIMENSION
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        forgetAll(event.getEntity(), Leave.LOGOUT);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        forgetAll(event.getEntity(), Leave.RESPAWN);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        forgetAll(event.getEntity(), Leave.DIMENSION);
    }

    /** プレイヤーごとの記憶を持つクラスを足したら、ここに足す（付け忘れを防ぐため）。 */
    private static void forgetAll(Player player, Leave leave) {
        if (leave == Leave.LOGOUT) {
            for (Map<UUID, HitBudget> budgets : BUDGETS.values()) {
                budgets.remove(player.getUUID());
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
            PlayerText.forgetOnLogout(player);
            if (player instanceof ServerPlayer serverPlayer) {
                VillagerBreedHints.onLogout(serverPlayer);
            }
        }
    }
}
