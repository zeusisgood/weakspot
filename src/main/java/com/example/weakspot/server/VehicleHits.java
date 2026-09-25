package com.example.weakspot.server;

import com.example.weakspot.common.HitKind;
import com.example.weakspot.VehicleTargets;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.VehicleBoostMath;
import com.example.weakspot.config.WeakSpotConfig;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 乗り物の弱点のヒット通知の検証と効果（論理サーバー。1.6.0）。倍率は VehicleBoostMath（コンボの上乗せあり、上限は
 * 初期値で無し）、続く時間は vehicleBoostDurationTicks（ヒットのたびに戻す）。
 * 馬系・豚は移動速度に一時的な修正（保存しない）をかけ、乗っている人のクライアントにも届いて効く。
 * トロッコは、ワールドの tick の最後に onUpdate を余分に呼ぶ（レールに沿って進むので、カーブでも脱線しない）。
 * ボートは乗っている人のクライアントが動きを決めるので、クライアント（ClientVehicleBoost）が速さを足す。
 * 弓・食事の弱点のヒットでも、乗っていれば加速を続ける（boostFromRider）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class VehicleHits {

    /** 通知の間隔はネットワークの揺らぎで縮むので、この tick 数だけ甘く見る。 */
    private static final int INTERVAL_JITTER_TICKS = 2;
    private static final UUID SPEED_MODIFIER = UUID.fromString("7a1c9c55-0f3b-4f5e-9d52-5a1f6c1e8b01");
    /** 移動速度の修正の種類: 合計に (1 + 値) を掛ける。 */
    private static final int MULTIPLY_TOTAL = 2;

    private static final Map<UUID, Long> LAST_HIT = new HashMap<>();
    private static final Map<Entity, Boost> BOOSTS = new WeakHashMap<>();

    private static final class Boost {
        double multiplier;
        int remaining;
        double carry;
    }

    private VehicleHits() {
    }

    public static void onHit(EntityPlayerMP player, int entityId, int streak) {
        if (!canBoost(player)) {
            return;
        }
        Entity vehicle = player.getRidingEntity();
        if (vehicle == null || vehicle.getEntityId() != entityId) {
            return;
        }
        long now = player.world.getTotalWorldTime();
        Long last = LAST_HIT.get(player.getUniqueID());
        int minInterval = Math.max(0, WeakSpotConfig.vehicleMinHitIntervalTicks - INTERVAL_JITTER_TICKS);
        if (last != null && now - last < minInterval) {
            return;
        }
        LAST_HIT.put(player.getUniqueID(), now);
        int combo = ServerStats.countStreak(player);
        ServerStats.recordKindHit(player, HitKind.VEHICLE);
        boost(vehicle, combo);
        ServerBoostTracker.notifyNearbyPlayers(player, new BlockPos(vehicle), streak);
    }

    /** 弓・食事の弱点のヒットを受け付けたときに呼ぶ。対象の乗り物に乗っていれば、加速を続ける（騎射）。 */
    public static void boostFromRider(EntityPlayerMP player, int combo) {
        if (canBoost(player)) {
            boost(player.getRidingEntity(), combo);
        }
    }

    private static boolean canBoost(EntityPlayerMP player) {
        return ServerSwitches.isEnabled(player, HitKind.VEHICLE) && !player.capabilities.isCreativeMode && !player.isSpectator()
                && WeakSpotConfig.vehicleWeakSpotEnabled && VehicleTargets.kind(player) != null;
    }

    private static void boost(Entity vehicle, int combo) {
        double multiplier = VehicleBoostMath.multiplier(WeakSpotConfig.vehicleBoostMultiplier,
                WeakSpotConfig.vehicleBoostMaxMultiplier, combo);
        Boost boost = BOOSTS.computeIfAbsent(vehicle, v -> new Boost());
        boost.multiplier = multiplier;
        boost.remaining = WeakSpotConfig.vehicleBoostDurationTicks;
        if (vehicle instanceof EntityLivingBase) {
            setSpeedModifier((EntityLivingBase) vehicle, multiplier);
        }
    }

    private static void setSpeedModifier(EntityLivingBase living, double multiplier) {
        IAttributeInstance speed = living.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        speed.removeModifier(SPEED_MODIFIER);
        double extra = VehicleBoostMath.extra(multiplier);
        if (extra > 0) {
            // 保存しない（加速中にワールドを保存しても、速いまま残らないように）
            speed.applyModifier(new AttributeModifier(SPEED_MODIFIER, "weakspot vehicle boost", extra, MULTIPLY_TOTAL)
                    .setSaved(false));
        }
    }

    /** ワールドの tick の最後に、残り時間を減らし、トロッコを余分に進め、切れたら修正を外す。 */
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote || BOOSTS.isEmpty()) {
            return;
        }
        for (Iterator<Map.Entry<Entity, Boost>> it = BOOSTS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Entity, Boost> entry = it.next();
            Entity vehicle = entry.getKey();
            if (vehicle.world != event.world) {
                continue;
            }
            Boost boost = entry.getValue();
            if (vehicle.isDead || --boost.remaining < 0) {
                if (vehicle instanceof EntityLivingBase) {
                    setSpeedModifier((EntityLivingBase) vehicle, 1);
                }
                it.remove();
                continue;
            }
            if (vehicle instanceof EntityMinecart && vehicle.isBeingRidden()) {
                double extra = VehicleBoostMath.extra(boost.multiplier) + boost.carry;
                int calls = (int) Math.floor(extra);
                boost.carry = extra - calls;
                for (int i = 0; i < calls && !vehicle.isDead; i++) {
                    vehicle.onUpdate();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_HIT.remove(event.player.getUniqueID());
    }

    /** サーバーが止まったら捨てる。 */
    public static void clear() {
        BOOSTS.clear();
    }
}
