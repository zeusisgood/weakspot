package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.VehicleTargets;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.TimedBoostMath;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import io.github.zeusisgood.weakspot.network.HitMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityBoat;

/**
 * 加速はサーバーがかける（馬・豚・トロッコ）。ボートは動きをクライアントが決めるので、ここで速さを足す。
 * 弓・食事の弱点のヒットでも、乗っていれば加速を続ける（onRiderHit）。
 */
final class VehicleSpot extends AimSpotKind {

    /** 1 tick の水平の移動（ブロック）。 */
    private static final double MOVING_SPEED = 0.05;
    /** サーバーの「動きが速すぎる」の判定に引っかからないように。 */
    private static final double MAX_BOAT_STEP = 8.0;

    /** 弱点の色を変えても変わらない。照準の下はコンボの表示と重なるので、上に出す（弓の引きゲージ（下）と上下対称）。 */
    private static final int BAR_COLOR = 0x55CCFF;

    /** ボートの速さと、ゲージのため。 */
    private static final TimedBoost BOOST = new TimedBoost();

    /** 曲がった分だけ弱点を回す。 */
    private Entity lastBoat;
    private float lastBoatYaw;

    VehicleSpot() {
        super(HitKind.VEHICLE, new HudSpot(HitKind.VEHICLE));
    }

    @Override
    void clear() {
        super.clear();
        BOOST.clear();
    }

    @Override
    boolean wanted(EntityPlayerSP player, SyncedSettings settings) {
        if (VehicleTargets.kind(player) == null) {
            return false;
        }
        Entity vehicle = player.getRidingEntity();
        return Math.hypot(vehicle.posX - vehicle.prevPosX, vehicle.posZ - vehicle.prevPosZ) >= MOVING_SPEED;
    }

    @Override
    int keepTicks() {
        return MOVING_KEEP_TICKS;
    }

    @Override
    int placement(EntityPlayerSP player) {
        return VehicleTargets.isSteeredByLook(player) ? HudSpot.VERTICAL : HudSpot.FREE;
    }

    @Override
    HitMessage message(EntityPlayerSP player, int streak) {
        return HitMessage.entity(HitKind.VEHICLE, player.getRidingEntity().getEntityId(), streak);
    }

    @Override
    void onHit(Minecraft mc, EntityPlayerSP player, SyncedSettings settings, int streak) {
        onRiderHit(streak);
    }

    /** ボートは乗っている人の視線も回すため。 */
    @Override
    void beforeTick(Minecraft mc) {
        EntityPlayerSP player = mc.player;
        Entity vehicle = player.getRidingEntity();
        if (!(vehicle instanceof EntityBoat) || vehicle.getControllingPassenger() != player) {
            lastBoat = null;
            return;
        }
        if (vehicle == lastBoat) {
            spot.rotateYaw(vehicle.rotationYaw - lastBoatYaw);
        }
        lastBoat = vehicle;
        lastBoatYaw = vehicle.rotationYaw;
    }

    @Override
    void tickEnd(Minecraft mc) {
        EntityPlayerSP player = mc.player;
        Entity vehicle = player.getRidingEntity();
        if (!(vehicle instanceof EntityBoat) || vehicle.getControllingPassenger() != player
                || !BOOST.isActive(ClientWeakSpotHandler.clientTick)) {
            return;
        }
        double extra = TimedBoostMath.extra(BOOST.multiplier());
        double dx = vehicle.motionX * extra;
        double dz = vehicle.motionZ * extra;
        double step = Math.hypot(dx, dz);
        if (step > MAX_BOAT_STEP) {
            dx *= MAX_BOAT_STEP / step;
            dz *= MAX_BOAT_STEP / step;
        }
        if (step > 1e-4) {
            vehicle.move(MoverType.SELF, dx, 0, dz);
        }
    }

    /** combo はヒット後の数。 */
    static void onRiderHit(int combo) {
        Minecraft mc = Minecraft.getMinecraft();
        SyncedSettings settings = ClientSettings.get();
        if (mc.player == null || !settings.enabled(HitKind.VEHICLE) || VehicleTargets.kind(mc.player) == null) {
            return;
        }
        BOOST.start(TimedBoostMath.multiplier(settings.vehicleBoostMultiplier, settings.vehicleBoostMaxMultiplier,
                combo), settings.vehicleBoostDurationTicks, ClientWeakSpotHandler.clientTick);
    }

    private static double remaining(float partialTicks) {
        return BOOST.remainingFraction(ClientWeakSpotHandler.clientTick, partialTicks);
    }

    @Override
    boolean hasGauge(Minecraft mc, float partialTicks) {
        return WeakSpotConfig.client.hud.vehicleBoostBarEnabled && remaining(partialTicks) > 0 && mc.player.isRiding();
    }

    @Override
    void drawGauge(Minecraft mc, float partialTicks) {
        HudSpot.gauge(mc, remaining(partialTicks), BAR_COLOR, true);
    }
}
