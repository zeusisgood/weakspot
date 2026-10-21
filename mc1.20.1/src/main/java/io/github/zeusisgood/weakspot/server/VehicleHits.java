package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.VehicleTargets;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.TimedBoostMath;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 馬系・豚は移動速度に一時的な修正（保存しない）をかけ、乗っている人のクライアントにも届いて効く。
 * トロッコは、ワールドの tick の最後に tick を余分に呼ぶ（レールに沿って進むので、カーブでも脱線しない）。
 * ボートは乗っている人のクライアントが動きを決めるので、クライアント（VehicleSpot）が速さを足す。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class VehicleHits {

    private static final SpeedModifier SPEED = new SpeedModifier(
            UUID.fromString("7a1c9c55-0f3b-4f5e-9d52-5a1f6c1e8b01"), "weakspot vehicle boost");

    private static final Map<Entity, Boost> BOOSTS = new WeakHashMap<>();

    private static final class Boost {
        double multiplier;
        int remaining;
        double carry;
    }

    private VehicleHits() {
    }

    public static void onHit(ServerPlayer player, int entityId, int streak) {
        if (!canBoost(player)) {
            return;
        }
        Entity vehicle = player.getVehicle();
        if (vehicle == null || vehicle.getId() != entityId || !HitGate.ready(player, HitKind.VEHICLE)) {
            return;
        }
        int combo = HitGate.accept(player, HitKind.VEHICLE, vehicle.blockPosition(), streak);
        boost(vehicle, combo);
    }

    /** 対象の乗り物に乗っていれば、加速を続ける（騎射）。 */
    public static void boostFromRider(ServerPlayer player, int combo) {
        if (canBoost(player)) {
            boost(player.getVehicle(), combo);
        }
    }

    private static boolean canBoost(ServerPlayer player) {
        return HitGate.allowed(player, HitKind.VEHICLE) && VehicleTargets.kind(player) != null;
    }

    private static void boost(Entity vehicle, int combo) {
        double multiplier = TimedBoostMath.multiplier(WeakSpotConfig.server.vehicle.vehicleBoostMultiplier,
                WeakSpotConfig.server.vehicle.vehicleBoostMaxMultiplier, combo);
        Boost boost = BOOSTS.computeIfAbsent(vehicle, v -> new Boost());
        boost.multiplier = multiplier;
        boost.remaining = WeakSpotConfig.server.vehicle.vehicleBoostDurationTicks;
        if (vehicle instanceof LivingEntity living) {
            SPEED.set(living, multiplier);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (event.level.isClientSide || BOOSTS.isEmpty()) {
            return;
        }
        for (Iterator<Map.Entry<Entity, Boost>> it = BOOSTS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Entity, Boost> entry = it.next();
            Entity vehicle = entry.getKey();
            if (vehicle.level() != event.level) {
                continue;
            }
            Boost boost = entry.getValue();
            if (vehicle.isRemoved() || --boost.remaining < 0) {
                if (vehicle instanceof LivingEntity living) {
                    SPEED.clear(living);
                }
                it.remove();
                continue;
            }
            if (vehicle instanceof AbstractMinecart && vehicle.isVehicle()) {
                double extra = TimedBoostMath.extra(boost.multiplier) + boost.carry;
                int calls = (int) Math.floor(extra);
                boost.carry = extra - calls;
                for (int i = 0; i < calls && !vehicle.isRemoved(); i++) {
                    vehicle.tick();
                }
            }
        }
    }

    public static void clear() {
        BOOSTS.clear();
    }
}
