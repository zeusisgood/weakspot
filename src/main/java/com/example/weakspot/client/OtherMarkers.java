package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.MarkerMotion;
import com.example.weakspot.common.MarkerSendPolicy;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.network.MarkerMessage;
import com.example.weakspot.network.MarkerMessage.MarkerData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 他のプレイヤーの弱点マーク（見えるだけで、当たり判定はない）と、自分のマークの状態の送信。
 * 対象は採掘の弱点と動物の弱点（成長・機械・釣りは送らない）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
final class OtherMarkers {

    /** 自分のマークが出ている間、変化がなくてもこの間隔で送り直す（受け手の時間切れを防ぐ）。 */
    private static final int KEEP_ALIVE_TICKS = 20;
    /** この間なにも届かなければ消す（送り直しの間隔より十分長く）。 */
    private static final int TIMEOUT_TICKS = 60;

    private static final MarkerSendPolicy SEND_POLICY = new MarkerSendPolicy(KEEP_ALIVE_TICKS);
    /** 最後にサーバーへ送った自分のマーク。null なら「出ていない」を送った（または何も送っていない）。 */
    private static MarkerData lastSent;

    /** 他のプレイヤー（エンティティ ID）ごとのマーク。 */
    private static final Map<Integer, Received> MARKERS = new HashMap<>();
    private static long tick;

    private OtherMarkers() {
    }

    private static final class Received {
        final MarkerData data;
        final long receivedTick;
        /** 描画用。受け取ったときの自分のワールドのブロックの形から作る。 */
        final WeakSpot spot;

        Received(MarkerData data, long receivedTick, WeakSpot spot) {
            this.data = data;
            this.receivedTick = receivedTick;
            this.spot = spot;
        }
    }

    /** サーバーから届いた（クライアントのスレッドで呼ぶ）。data が null なら消えた。 */
    static void receive(int entityId, MarkerData data) {
        Minecraft mc = Minecraft.getMinecraft();
        if (data == null || mc.world == null) {
            MARKERS.remove(entityId);
            return;
        }
        WeakSpot spot;
        if (data.isAnimal()) {
            Entity animal = mc.world.getEntityByID(data.entityId);
            spot = animal == null || animal.isDead ? null
                    : WeakSpot.atEntity(animal, data.face, data.u, data.v, ClientSettings.get());
        } else {
            IBlockState state = mc.world.getBlockState(data.pos);
            spot = state.getBlock().isAir(state, mc.world, data.pos) ? null
                    : WeakSpot.at(HitKind.MINING, mc.world, data.pos, state, data.face, data.u, data.v,
                            ClientSettings.get());
        }
        if (spot == null) {
            MARKERS.remove(entityId);
            return;
        }
        Received previous = MARKERS.get(entityId);
        boolean sameSurface = previous != null && previous.spot.sameSurface(spot);
        if (MarkerMotion.animates(WeakSpotConfig.weakSpotTrailEnabled, previous != null, sameSurface)) {
            // 最後に知っている位置から、届いた位置へ動かす（間引かれて途中の移動がまとめて届いても同じ）。
            // 同じ位置の送り直しなら動かさない
            spot = previous.spot;
            spot.moveTo(data.u, data.v, true, Minecraft.getSystemTime());
        }
        MARKERS.put(entityId, new Received(data, tick, spot));
    }

    /** 描画する他のプレイヤーのマーク（範囲の外のものは除く）。 */
    static List<WeakSpot> visible(Entity camera, float partialTicks) {
        List<WeakSpot> result = new ArrayList<>();
        double range = ClientSettings.get().markerShareRange;
        for (Received marker : MARKERS.values()) {
            if (camera.getDistanceSqToCenter(marker.data.pos) <= range * range) {
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
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null) {
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

    /** マークの対象（ブロックまたは動物）がなくなった。 */
    private static boolean isGone(Minecraft mc, MarkerData data) {
        if (data.isAnimal()) {
            Entity animal = mc.world.getEntityByID(data.entityId);
            return animal == null || animal.isDead;
        }
        return mc.world.isAirBlock(data.pos);
    }

    /** 自分の採掘の弱点が出た・動いた・消えたときに、送信頻度の上限を守って送る。 */
    private static void sendOwn() {
        SyncedSettings settings = ClientSettings.get();
        WeakSpot spot = ClientWeakSpotHandler.spot;
        MarkerData current = null;
        if (spot != null && spot.kind == HitKind.MINING) {
            current = new MarkerData(spot.pos, spot.face, spot.u, spot.v);
        } else if (spot != null && spot.kind == HitKind.ANIMAL) {
            current = new MarkerData(spot.entity.getPosition(), spot.face, spot.u, spot.v, spot.entity.getEntityId());
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
