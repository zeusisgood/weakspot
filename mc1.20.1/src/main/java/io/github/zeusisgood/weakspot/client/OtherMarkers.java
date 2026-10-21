package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.MarkerMotion;
import io.github.zeusisgood.weakspot.common.MarkerSendPolicy;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import io.github.zeusisgood.weakspot.network.MarkerMessage.MarkerData;
import io.github.zeusisgood.weakspot.network.MarkerMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 他のプレイヤーの弱点マーク（見えるだけで、当たり判定はない）と、自分のマークの送信。 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Dist.CLIENT)
final class OtherMarkers {

    /** 受け手の時間切れを防ぐ。 */
    private static final int KEEP_ALIVE_TICKS = 20;
    /** 送り直しの間隔より十分長く。 */
    private static final int TIMEOUT_TICKS = 60;

    private static final MarkerSendPolicy SEND_POLICY = new MarkerSendPolicy(KEEP_ALIVE_TICKS);
    /** null なら「出ていない」を送った（または何も送っていない）。 */
    private static MarkerData lastSent;

    /** エンティティ ID ごと。 */
    private static final Map<Integer, Received> MARKERS = new HashMap<>();
    private static long tick;

    private OtherMarkers() {
    }

    private static final class Received {
        final MarkerData data;
        final long receivedTick;
        /** 受け取ったときの自分のワールドのブロックの形から作る。 */
        final WeakSpot spot;

        Received(MarkerData data, long receivedTick, WeakSpot spot) {
            this.data = data;
            this.receivedTick = receivedTick;
            this.spot = spot;
        }
    }

    /** クライアントのスレッドで呼ぶ。data が null なら消えた。 */
    static void receive(int entityId, MarkerData data) {
        Minecraft mc = Minecraft.getInstance();
        if (data == null || mc.level == null) {
            MARKERS.remove(entityId);
            return;
        }
        WeakSpot spot;
        if (data.isAnimal()) {
            Entity animal = mc.level.getEntity(data.entityId);
            spot = animal == null || animal.isRemoved() ? null
                    : WeakSpot.atEntity(animal, data.face, data.u, data.v, ClientSettings.get());
        } else {
            BlockState state = mc.level.getBlockState(data.pos);
            spot = state.isAir() ? null
                    : WeakSpot.at(HitKind.MINING, mc.level, data.pos, state, data.face, data.u, data.v,
                            ClientSettings.get());
        }
        if (spot == null) {
            MARKERS.remove(entityId);
            return;
        }
        Received previous = MARKERS.get(entityId);
        boolean sameSurface = previous != null && previous.spot.sameSurface(spot);
        if (MarkerMotion.animates(WeakSpotConfig.client.markers.weakSpotTrailEnabled, previous != null, sameSurface)) {
            // 間引かれて途中の移動がまとめて届いても、最後に知っている位置から動かす。同じ位置の送り直しなら動かさない
            spot = previous.spot;
            spot.moveTo(data.u, data.v, true, Util.getMillis());
        }
        MARKERS.put(entityId, new Received(data, tick, spot));
    }

    /** 範囲の外のものは除く。 */
    static List<WeakSpot> visible(Entity camera, float partialTicks) {
        List<WeakSpot> result = new ArrayList<>();
        double range = ClientSettings.get().markerShareRange;
        for (Received marker : MARKERS.values()) {
            if (camera.distanceToSqr(Vec3.atCenterOf(marker.data.pos)) <= range * range) {
                if (marker.spot.entity != null) {
                    marker.spot.follow(WeakSpot.renderBox(marker.spot.entity, partialTicks));
                }
                result.add(marker.spot);
            }
        }
        return result;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            MARKERS.clear();
            lastSent = null;
            SEND_POLICY.reset();
            return;
        }
        tick++;
        for (Iterator<Received> it = MARKERS.values().iterator(); it.hasNext(); ) {
            Received marker = it.next();
            if (tick - marker.receivedTick > TIMEOUT_TICKS || isGone(mc, marker.data)) {
                it.remove();
            }
        }
        sendOwn();
    }

    private static boolean isGone(Minecraft mc, MarkerData data) {
        if (data.isAnimal()) {
            Entity animal = mc.level.getEntity(data.entityId);
            return animal == null || animal.isRemoved();
        }
        return mc.level.isEmptyBlock(data.pos);
    }

    /** 機械・成長は採掘と同じブロックのマークとして送る（受け取る側は、ブロックの箱から形を決める）。 */
    private static void sendOwn() {
        SyncedSettings settings = ClientSettings.get();
        WeakSpot spot = ClientWeakSpotHandler.spot;
        MarkerData current = null;
        // 掘る前の採掘の弱点は送らない（よそ見のたびに、ほかの人の画面に出ないように）
        if (spot != null && !spot.preview && (spot.kind == HitKind.MINING || spot.kind == HitKind.MACHINE
                || spot.kind == HitKind.GROWTH || spot.kind == HitKind.HARVEST)) {
            current = new MarkerData(spot.pos, spot.face, spot.u, spot.v);
        } else if (spot != null && spot.kind == HitKind.ANIMAL) {
            current = new MarkerData(spot.entity.blockPosition(), spot.face, spot.u, spot.v, spot.entity.getId());
        }
        if (settings.markerShareRange <= 0) {
            // サーバーは転送しないので送らない。範囲が 0 に変わる前に出ていたマークは、サーバー側で時間切れになる
            return;
        }
        boolean changed = current == null ? lastSent != null : !current.sameAs(lastSent);
        if (SEND_POLICY.shouldSend(changed, current != null, tick, settings.markerSendMinIntervalTicks)) {
            WeakSpotMod.network.sendToServer(new MarkerMessage(current));
            lastSent = current;
        }
    }
}
