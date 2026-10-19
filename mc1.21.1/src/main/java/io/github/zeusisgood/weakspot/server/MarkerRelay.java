package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import io.github.zeusisgood.weakspot.network.MarkerMessage.MarkerData;
import io.github.zeusisgood.weakspot.network.OtherMarkerMessage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 毎 tick、マークから markerShareRange 以内にいる他のプレイヤー（見る人）の集合を計算し直す。
 * プレイヤーの追跡範囲（StartTracking）は描画距離に合わせたもので markerShareRange と合わないため、使わない。
 */
@EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class MarkerRelay {

    /** 送り手が送り直す間隔（20 tick）より十分長く。 */
    private static final int TIMEOUT_TICKS = 60;
    /** 通知の間隔はネットワークの揺らぎで縮むので、この tick 数だけ甘く見る。 */
    private static final int INTERVAL_JITTER_TICKS = 1;

    private static final Map<UUID, Marker> MARKERS = new HashMap<>();

    private MarkerRelay() {
    }

    private static final class Marker {
        final ServerPlayer owner;
        MarkerData data;
        long receivedTick;
        boolean dirty;
        long lastForwardTick = Long.MIN_VALUE / 2;
        final Set<ServerPlayer> viewers = new HashSet<>();

        Marker(ServerPlayer owner) {
            this.owner = owner;
        }
    }

    /** data が null なら消えた。 */
    public static void onMarker(ServerPlayer player, MarkerData data) {
        if (data == null) {
            remove(player);
            return;
        }
        Marker marker = MARKERS.computeIfAbsent(player.getUUID(), id -> new Marker(player));
        marker.data = data;
        marker.receivedTick = player.level().getGameTime();
        marker.dirty = true;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (MARKERS.isEmpty()) {
            return;
        }
        double range = WeakSpotConfig.server.general.markerShareRange;
        int minInterval = Math.max(0, WeakSpotConfig.server.general.markerSendMinIntervalTicks - INTERVAL_JITTER_TICKS);
        for (Iterator<Marker> it = MARKERS.values().iterator(); it.hasNext(); ) {
            Marker marker = it.next();
            long now = marker.owner.level().getGameTime();
            if (marker.owner.hasDisconnected() || now - marker.receivedTick > TIMEOUT_TICKS) {
                hideFromAll(marker);
                it.remove();
                continue;
            }
            Set<ServerPlayer> inRange = viewersInRange(marker, range);
            OtherMarkerMessage show = new OtherMarkerMessage(marker.owner.getId(), marker.data);
            OtherMarkerMessage hide = new OtherMarkerMessage(marker.owner.getId(), null);
            for (ServerPlayer viewer : marker.viewers) {
                if (!inRange.contains(viewer)) {
                    WeakSpotMod.network.sendTo(hide, viewer);
                }
            }
            boolean forward = marker.dirty && now - marker.lastForwardTick >= minInterval;
            for (ServerPlayer viewer : inRange) {
                if (forward || !marker.viewers.contains(viewer)) {
                    WeakSpotMod.network.sendTo(show, viewer);
                }
            }
            if (forward) {
                marker.dirty = false;
                marker.lastForwardTick = now;
            }
            marker.viewers.clear();
            marker.viewers.addAll(inRange);
        }
    }

    /** ブロックの中心から測る。range が 0 なら誰もいない。 */
    private static Set<ServerPlayer> viewersInRange(Marker marker, double range) {
        Set<ServerPlayer> result = new HashSet<>();
        if (range <= 0) {
            return result;
        }
        BlockPos pos = marker.data.pos;
        double rangeSq = range * range;
        for (Player other : marker.owner.level().players()) {
            if (other != marker.owner && other instanceof ServerPlayer && !((ServerPlayer) other).hasDisconnected()
                    && other.distanceToSqr(Vec3.atCenterOf(pos)) <= rangeSq) {
                result.add((ServerPlayer) other);
            }
        }
        return result;
    }

    private static void remove(Player player) {
        Marker marker = MARKERS.remove(player.getUUID());
        if (marker != null) {
            hideFromAll(marker);
        }
    }

    private static void hideFromAll(Marker marker) {
        OtherMarkerMessage hide = new OtherMarkerMessage(marker.owner.getId(), null);
        for (ServerPlayer viewer : marker.viewers) {
            if (!viewer.hasDisconnected()) {
                WeakSpotMod.network.sendTo(hide, viewer);
            }
        }
        marker.viewers.clear();
    }

    static void forget(Player player, HitGate.Leave leave) {
        remove(player);
    }
}
