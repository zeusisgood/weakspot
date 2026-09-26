package com.example.weakspot.server;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.network.MarkerMessage.MarkerData;
import com.example.weakspot.network.OtherMarkerMessage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 他のプレイヤーの弱点マークの転送（論理サーバー）。各プレイヤーの今のマークを覚えておき、毎tick、
 * マークから markerShareRange 以内にいる他のプレイヤー（見る人）の集合を計算し直す。
 * 新しく範囲に入った人には今の状態を、マークが変わったら範囲内の全員に、範囲から出た人には「消えた」を送る。
 * プレイヤーの追跡範囲（StartTracking）は描画距離に合わせたもので markerShareRange と合わないため、使わない。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class MarkerRelay {

    /** 送り手が送り直す間隔（20tick）より十分長い間なにも届かなければ、消えたものとして扱う。 */
    private static final int TIMEOUT_TICKS = 60;
    /** 通知の間隔はネットワークの揺らぎで縮むので、この tick 数だけ甘く見る。 */
    private static final int INTERVAL_JITTER_TICKS = 1;

    private static final Map<UUID, Marker> MARKERS = new HashMap<>();

    private MarkerRelay() {
    }

    private static final class Marker {
        final EntityPlayerMP owner;
        MarkerData data;
        long receivedTick;
        /** まだ見る人へ送っていない変更がある。 */
        boolean dirty;
        long lastForwardTick = Long.MIN_VALUE / 2;
        /** 今このマークを送っている人。 */
        final Set<EntityPlayerMP> viewers = new HashSet<>();

        Marker(EntityPlayerMP owner) {
            this.owner = owner;
        }
    }

    /** クライアントから自分のマークの状態が届いた。data が null なら消えた。 */
    public static void onMarker(EntityPlayerMP player, MarkerData data) {
        if (data == null) {
            remove(player);
            return;
        }
        Marker marker = MARKERS.computeIfAbsent(player.getUniqueID(), id -> new Marker(player));
        marker.data = data;
        marker.receivedTick = player.world.getTotalWorldTime();
        marker.dirty = true;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || MARKERS.isEmpty()) {
            return;
        }
        double range = WeakSpotConfig.markerShareRange;
        int minInterval = Math.max(0, WeakSpotConfig.markerSendMinIntervalTicks - INTERVAL_JITTER_TICKS);
        for (Iterator<Marker> it = MARKERS.values().iterator(); it.hasNext(); ) {
            Marker marker = it.next();
            long now = marker.owner.world.getTotalWorldTime();
            if (marker.owner.hasDisconnected() || now - marker.receivedTick > TIMEOUT_TICKS) {
                hideFromAll(marker);
                it.remove();
                continue;
            }
            Set<EntityPlayerMP> inRange = viewersInRange(marker, range);
            OtherMarkerMessage show = new OtherMarkerMessage(marker.owner.getEntityId(), marker.data);
            OtherMarkerMessage hide = new OtherMarkerMessage(marker.owner.getEntityId(), null);
            for (EntityPlayerMP viewer : marker.viewers) {
                if (!inRange.contains(viewer)) {
                    WeakSpotMod.network.sendTo(hide, viewer);
                }
            }
            boolean forward = marker.dirty && now - marker.lastForwardTick >= minInterval;
            for (EntityPlayerMP viewer : inRange) {
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

    /** 同じワールドで、マーク（ブロックの中心）から range 以内にいる、持ち主以外のプレイヤー。range が 0 なら誰もいない。 */
    private static Set<EntityPlayerMP> viewersInRange(Marker marker, double range) {
        Set<EntityPlayerMP> result = new HashSet<>();
        if (range <= 0) {
            return result;
        }
        BlockPos pos = marker.data.pos;
        double rangeSq = range * range;
        for (EntityPlayer other : marker.owner.world.playerEntities) {
            if (other != marker.owner && other instanceof EntityPlayerMP && !((EntityPlayerMP) other).hasDisconnected()
                    && other.getDistanceSqToCenter(pos) <= rangeSq) {
                result.add((EntityPlayerMP) other);
            }
        }
        return result;
    }

    private static void remove(EntityPlayer player) {
        Marker marker = MARKERS.remove(player.getUniqueID());
        if (marker != null) {
            hideFromAll(marker);
        }
    }

    private static void hideFromAll(Marker marker) {
        OtherMarkerMessage hide = new OtherMarkerMessage(marker.owner.getEntityId(), null);
        for (EntityPlayerMP viewer : marker.viewers) {
            if (!viewer.hasDisconnected()) {
                WeakSpotMod.network.sendTo(hide, viewer);
            }
        }
        marker.viewers.clear();
    }

    /** ログアウト・ディメンション移動・死亡で、マークを消す（HitGate から呼ぶ）。 */
    static void forget(EntityPlayer player, HitGate.Leave leave) {
        remove(player);
    }
}
