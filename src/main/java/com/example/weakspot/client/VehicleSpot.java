package com.example.weakspot.client;

import com.example.weakspot.VehicleTargets;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.VehicleBoostMath;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.network.HitMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityBoat;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 乗り物の弱点（自分だけ。1.6.0）と、加速の残り時間のゲージ。馬・豚・トロッコ・ボートに乗って動いている間、
 * 照準の近く（馬・豚は真上か真下だけ）に水色の弱点を出し、照準を合わせるだけでヒットにする。弓を引いている・
 * 何かを使っている（食べているなど）間は出さない（そちらの弱点を出す）。
 *
 * 加速はサーバーがかける（馬・豚・トロッコ）。ボートは動きをクライアントが決めるので、ここで速さを足す。
 * 弓・食事の弱点のヒットでも、乗っていれば加速を続ける（onRiderHit）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
final class VehicleSpot {

    /** 乗り物が動いているとみなす、1 tick の水平の移動（ブロック）。 */
    private static final double MOVING_SPEED = 0.05;
    /** ボートに足す 1 tick の移動の上限（ブロック。サーバーの「動きが速すぎる」の判定に引っかからないように）。 */
    private static final double MAX_BOAT_STEP = 8.0;

    private static final int BAR_WIDTH = 40;
    private static final int BAR_HEIGHT = 3;
    /** 照準の中心から、ゲージの中心までの下向きの距離（GUI ピクセル。弓の引きゲージの下）。 */
    private static final int BAR_OFFSET = 22;
    /** 水色 #55CCFF と、背景 #1E1E1E 半透明。 */
    private static final int RGB = 0x55CCFF;
    private static final float[] BAR_FILL = {0x55 / 255F, 0xCC / 255F, 0xFF / 255F, 1.0F};
    private static final float[] BAR_BACK = {0x1E / 255F, 0x1E / 255F, 0x1E / 255F, 0.5F};

    private static final HudSpot SPOT = new HudSpot(RGB);

    /** 自分の側で覚えている加速（ボートの速さと、ゲージのため）。 */
    private static double multiplier = 1;
    private static long boostUntil = Long.MIN_VALUE / 2;
    private static int boostDuration = 1;

    private VehicleSpot() {
    }

    static void clear() {
        SPOT.clear();
        boostUntil = Long.MIN_VALUE / 2;
    }

    /** 乗り物の弱点を出すか。 */
    private static boolean eligible(Minecraft mc) {
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null || !WeakSpotConfig.weakSpotsEnabled
                || player.capabilities.isCreativeMode || player.isSpectator() || player.isHandActive()) {
            return false;
        }
        if (!ClientSettings.get().vehicleWeakSpotEnabled || VehicleTargets.kind(player) == null) {
            return false;
        }
        Entity vehicle = player.getRidingEntity();
        return Math.hypot(vehicle.posX - vehicle.prevPosX, vehicle.posZ - vehicle.prevPosZ) >= MOVING_SPEED;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.isGamePaused() || mc.player == null) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            if (!eligible(mc)) {
                SPOT.clear();
            } else {
                SPOT.ensure(mc.player, VehicleTargets.isSteeredByLook(mc.player));
            }
        } else {
            pushBoat(mc.player);
        }
    }

    /** ボートの動きはクライアントが決めるので、加速中は、その tick の移動に (倍率 − 1) 倍を足す。 */
    private static void pushBoat(EntityPlayerSP player) {
        Entity vehicle = player.getRidingEntity();
        if (!(vehicle instanceof EntityBoat) || vehicle.getControllingPassenger() != player
                || ClientWeakSpotHandler.clientTick >= boostUntil) {
            return;
        }
        double extra = VehicleBoostMath.extra(multiplier);
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

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!SPOT.aimed(mc, event.getPartialTicks()) || !eligible(mc)) {
            return;
        }
        SyncedSettings settings = ClientSettings.get();
        if (!ClientWeakSpotHandler.canHitNow(HitKind.VEHICLE, settings.vehicleMinHitIntervalTicks)) {
            return;
        }
        int streak = ClientWeakSpotHandler.registerHit(HitKind.VEHICLE);
        WeakSpotMod.network.sendToServer(HitMessage.entity(HitKind.VEHICLE, mc.player.getRidingEntity().getEntityId(),
                streak));
        onRiderHit(streak);
        SPOT.relocate(mc.player);
    }

    /** 乗り物・弓・食事の弱点に当てた（乗っていれば、加速を続ける）。combo はヒット後の連続ヒット数。 */
    static void onRiderHit(int combo) {
        Minecraft mc = Minecraft.getMinecraft();
        SyncedSettings settings = ClientSettings.get();
        if (mc.player == null || !settings.vehicleWeakSpotEnabled || VehicleTargets.kind(mc.player) == null) {
            return;
        }
        multiplier = VehicleBoostMath.multiplier(settings.vehicleBoostMultiplier, settings.vehicleBoostMaxMultiplier,
                combo);
        boostDuration = Math.max(1, settings.vehicleBoostDurationTicks);
        boostUntil = ClientWeakSpotHandler.clientTick + boostDuration;
    }

    @SubscribeEvent
    public static void onOverlayPost(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.gameSettings.hideGUI) {
            return;
        }
        double remaining = (boostUntil - ClientWeakSpotHandler.clientTick - event.getPartialTicks()) / boostDuration;
        boolean bar = WeakSpotConfig.vehicleBoostBarEnabled && remaining > 0 && mc.player.isRiding();
        if (!SPOT.has() && !bar) {
            return;
        }
        HudSpot.beginOverlay();
        SPOT.draw(mc);
        if (bar) {
            ScaledResolution res = new ScaledResolution(mc);
            double x0 = res.getScaledWidth() / 2.0 - BAR_WIDTH / 2.0;
            double y0 = res.getScaledHeight() / 2.0 + BAR_OFFSET - BAR_HEIGHT / 2.0;
            ScreenProjection.rect(x0, y0, x0 + BAR_WIDTH, y0 + BAR_HEIGHT, BAR_BACK);
            ScreenProjection.rect(x0, y0, x0 + BAR_WIDTH * Math.min(1, remaining), y0 + BAR_HEIGHT, BAR_FILL);
        }
        HudSpot.endOverlay();
    }
}
