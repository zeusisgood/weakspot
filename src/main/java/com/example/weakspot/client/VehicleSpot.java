package com.example.weakspot.client;

import com.example.weakspot.VehicleTargets;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.TimedBoostMath;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.network.HitMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityBoat;

/**
 * 乗り物の弱点（自分だけ。1.6.0）と、加速の残り時間のゲージ（照準の上）。馬・豚・トロッコ・ボートに乗って動いている間、
 * 照準の近く（馬・豚は真上か真下だけ）に水色の弱点を出し、照準を合わせるだけでヒットにする。弓を引いている・
 * 何かを使っている（食べているなど）間は出さない（そちらの弱点を出す）。
 *
 * 加速はサーバーがかける（馬・豚・トロッコ）。ボートは動きをクライアントが決めるので、ここで速さを足す。
 * 弓・食事の弱点のヒットでも、乗っていれば加速を続ける（onRiderHit）。1.8.6 から AimSpotKind（AimSpots が回す）。
 */
final class VehicleSpot extends AimSpotKind {

    /** 乗り物が動いているとみなす、1 tick の水平の移動（ブロック）。 */
    private static final double MOVING_SPEED = 0.05;
    /** ボートに足す 1 tick の移動の上限（ブロック。サーバーの「動きが速すぎる」の判定に引っかからないように）。 */
    private static final double MAX_BOAT_STEP = 8.0;

    private static final int BAR_WIDTH = 40;
    private static final int BAR_HEIGHT = 3;
    /**
     * 照準の中心から、ゲージの中心までの上向きの距離（GUI ピクセル）。照準の下はコンボの表示と重なるので、上に出す
     * （1.6.4。弓の引きゲージ（下に 12）と上下対称）。
     */
    private static final int BAR_OFFSET = 12;
    private static final float[] BAR_FILL = {0x55 / 255F, 0xCC / 255F, 0xFF / 255F, 1.0F};
    private static final float[] BAR_BACK = {0x1E / 255F, 0x1E / 255F, 0x1E / 255F, 0.5F};

    /** 自分の側で覚えている加速（ボートの速さと、ゲージのため。弓・食事・投げる物からも onRiderHit で続ける）。 */
    private static double multiplier = 1;
    private static long boostUntil = Long.MIN_VALUE / 2;
    private static int boostDuration = 1;

    /** 前の tick のボートと、その yaw（1.8.2。曲がった分だけ弱点を回す）。 */
    private Entity lastBoat;
    private float lastBoatYaw;

    VehicleSpot() {
        super(HitKind.VEHICLE, new HudSpot(HitKind.VEHICLE));
    }

    @Override
    void clear() {
        super.clear();
        boostUntil = Long.MIN_VALUE / 2;
    }

    /** 乗り物の弱点を出すか（乗り物が動いているとき）。 */
    @Override
    boolean wanted(EntityPlayerSP player, SyncedSettings settings) {
        if (VehicleTargets.kind(player) == null) {
            return false;
        }
        Entity vehicle = player.getRidingEntity();
        return Math.hypot(vehicle.posX - vehicle.prevPosX, vehicle.posZ - vehicle.prevPosZ) >= MOVING_SPEED;
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

    /** 自分が操っているボートが曲がった分だけ、弱点を回す（ボートは乗っている人の視線も回すため）。 */
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

    /** ボートの動きはクライアントが決めるので、加速中は、その tick の移動に (倍率 − 1) 倍を足す。 */
    @Override
    void tickEnd(Minecraft mc) {
        EntityPlayerSP player = mc.player;
        Entity vehicle = player.getRidingEntity();
        if (!(vehicle instanceof EntityBoat) || vehicle.getControllingPassenger() != player
                || ClientWeakSpotHandler.clientTick >= boostUntil) {
            return;
        }
        double extra = TimedBoostMath.extra(multiplier);
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

    /** 乗り物・弓・食事・投げる物の弱点に当てた（乗っていれば、加速を続ける）。combo はヒット後の連続ヒット数。 */
    static void onRiderHit(int combo) {
        Minecraft mc = Minecraft.getMinecraft();
        SyncedSettings settings = ClientSettings.get();
        if (mc.player == null || !settings.enabled(HitKind.VEHICLE) || VehicleTargets.kind(mc.player) == null) {
            return;
        }
        multiplier = TimedBoostMath.multiplier(settings.vehicleBoostMultiplier, settings.vehicleBoostMaxMultiplier,
                combo);
        boostDuration = Math.max(1, settings.vehicleBoostDurationTicks);
        boostUntil = ClientWeakSpotHandler.clientTick + boostDuration;
    }

    private static double remaining(float partialTicks) {
        return (boostUntil - ClientWeakSpotHandler.clientTick - partialTicks) / boostDuration;
    }

    @Override
    boolean hasGauge(Minecraft mc, float partialTicks) {
        return WeakSpotConfig.vehicleBoostBarEnabled && remaining(partialTicks) > 0 && mc.player.isRiding();
    }

    @Override
    void drawGauge(Minecraft mc, float partialTicks) {
        ScaledResolution res = new ScaledResolution(mc);
        double x0 = res.getScaledWidth() / 2.0 - BAR_WIDTH / 2.0;
        double y0 = res.getScaledHeight() / 2.0 - BAR_OFFSET - BAR_HEIGHT / 2.0;
        ScreenProjection.rect(x0, y0, x0 + BAR_WIDTH, y0 + BAR_HEIGHT, BAR_BACK);
        ScreenProjection.rect(x0, y0, x0 + BAR_WIDTH * Math.min(1, remaining(partialTicks)), y0 + BAR_HEIGHT,
                BAR_FILL);
    }
}
