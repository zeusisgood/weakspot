package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.FishingWater;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.FishingMath;
import io.github.zeusisgood.weakspot.common.HitBudget;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.network.StateMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 浮きの待ち時間のタイマーはサーバーだけが持ち、非公開のフィールドを読み書きする（accesstransformer.cfg）。
 * バニラの catchingFish は、待ち時間（timeUntilLured）→ 魚が寄ってくる段階（timeUntilHooked）→ 食いついた段階
 * （nibble）の順に進む。待ち時間が始まったときの長さは、進み具合のバーのために、浮きごとに覚える（メモリだけ）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class FishingHits {

    private static final Map<UUID, Track> TRACKS = new HashMap<>();

    private FishingHits() {
    }

    private static final class Track {
        FishingHook hook;
        int initialWait;
        int lastWait;
        final HitBudget budget = new HitBudget();
    }

    /** 弱点が出る段階。 */
    static boolean isWaiting(FishingHook hook) {
        return waitingPhase(hook) && FishingWater.isFloating(hook);
    }

    /** 浮きの位置が水かは見ない。 */
    private static boolean waitingPhase(FishingHook hook) {
        return !hook.isRemoved() && hook.currentState == FishingHook.FishHookState.BOBBING
                && hook.timeUntilLured > 0 && hook.timeUntilHooked == 0 && hook.nibble == 0;
    }

    /** なければ null。 */
    private static FishingHook hookOf(Player player) {
        FishingHook hook = player.fishing;
        if (hook == null || hook.isRemoved() || hook.getPlayerOwner() != player) {
            return null;
        }
        boolean holdsRod = player.getMainHandItem().getItem() instanceof FishingRodItem
                || player.getOffhandItem().getItem() instanceof FishingRodItem;
        return holdsRod ? hook : null;
    }

    /** 新しい待ち時間が始まると、長さが増える。 */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide) {
            return;
        }
        FishingHook hook = player.fishing;
        if (hook == null || hook.isRemoved()) {
            TRACKS.remove(player.getUUID());
            return;
        }
        Track track = TRACKS.computeIfAbsent(player.getUUID(), id -> new Track());
        if (track.hook != hook) {
            track.hook = hook;
            track.initialWait = 0;
            track.lastWait = 0;
        }
        if (isWaiting(hook)) {
            int wait = hook.timeUntilLured;
            if (track.initialWait == 0 || wait > track.lastWait) {
                track.initialWait = wait;
            }
            track.lastWait = wait;
        } else {
            track.initialWait = 0;
            track.lastWait = 0;
        }
    }

    public static void onQuery(ServerPlayer player) {
        FishingHook hook = hookOf(player);
        boolean waiting = hook != null && isWaiting(hook) && enabled(SyncedSettings.server());
        float progress = 0;
        if (waiting) {
            Track track = TRACKS.get(player.getUUID());
            if (track != null && track.hook == hook) {
                progress = (float) FishingMath.progress(track.initialWait, hook.timeUntilLured);
            }
        }
        WeakSpotMod.network.sendTo(StateMessage.fishing(waiting, progress), player);
    }

    private static boolean enabled(SyncedSettings settings) {
        return settings.enabled(HitKind.FISHING) && settings.fishingHits > 0;
    }

    /** 照準の角度は確かめない（クライアントを信用する）。 */
    public static void onHit(ServerPlayer player, int streak) {
        if (!HitGate.allowed(player, HitKind.FISHING)) {
            return;
        }
        SyncedSettings settings = SyncedSettings.server();
        FishingHook hook = hookOf(player);
        if (hook == null || !enabled(settings) || !isWaiting(hook)) {
            return;
        }
        Track track = TRACKS.computeIfAbsent(player.getUUID(), id -> new Track());
        long now = player.level().getGameTime();
        if (!HitGate.take(track.budget, now, HitKind.FISHING)) {
            return;
        }
        hook.timeUntilLured = FishingMath.waitAfterHit(hook.timeUntilLured, FishingMath.ticksPerHit(settings.fishingHits));
        HitGate.accept(player, HitKind.FISHING, hook.blockPosition(), streak);
    }

    static void forget(Player player, HitGate.Leave leave) {
        if (leave == HitGate.Leave.LOGOUT) {
            TRACKS.remove(player.getUUID());
        }
    }
}
