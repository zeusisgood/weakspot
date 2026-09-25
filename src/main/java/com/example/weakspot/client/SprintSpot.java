package com.example.weakspot.client;

import com.example.weakspot.PlayerRules;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.VehicleBoostMath;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.network.HitMessage;
import com.example.weakspot.server.MoveHits;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 走りの弱点（自分だけ。1.7.0）。地面を走っている間、照準の真上か真下に赤の弱点を出し、照準を合わせるだけで
 * ヒットにする（左右に向きを変えると進む向きがぶれるため。馬と同じ）。速さはサーバーが移動速度の修正でかける
 * （MoveHits。自分のクライアントにも届いて効く）。ここでは残り時間のゲージと、視野の広がりの抑えだけを受け持つ。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
final class SprintSpot {

    /** 走っているとみなす、1 tick の水平の移動（ブロック）。 */
    private static final double MOVING_SPEED = 0.05;
    /** 赤 #FF5A5F。 */
    private static final int RGB = 0xFF5A5F;
    /** 走りの加速中の、視野の倍率の上限（見た目だけ。速さには上限を付けない）。 */
    private static final float MAX_FOV = 1.3F;

    private static final HudSpot SPOT = new HudSpot(HitKind.SPRINT, RGB);
    private static final TimedBoost BOOST = new TimedBoost();

    private SprintSpot() {
    }

    static void clear() {
        SPOT.clear();
        BOOST.clear();
    }

    private static boolean eligible(Minecraft mc) {
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null || !KindSwitches.isEnabled(HitKind.SPRINT)
                || !PlayerRules.canUse(player) || player.isHandActive()) {
            return false;
        }
        return ClientSettings.get().sprintWeakSpotEnabled && player.isSprinting() && !player.isRiding()
                && !player.isElytraFlying() && !player.isInWater() && !player.isOnLadder()
                && Math.hypot(player.posX - player.prevPosX, player.posZ - player.prevPosZ) >= MOVING_SPEED;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.START || mc.isGamePaused() || mc.player == null) {
            return;
        }
        if (!eligible(mc)) {
            SPOT.clear();
        } else {
            SPOT.ensure(mc.player, true);
        }
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!SPOT.aimed(mc, event.getPartialTicks()) || !eligible(mc)) {
            return;
        }
        SyncedSettings settings = ClientSettings.get();
        if (!ClientWeakSpotHandler.canHitNow(HitKind.SPRINT, settings.sprintMinHitIntervalTicks)) {
            return;
        }
        int streak = ClientWeakSpotHandler.registerHit(HitKind.SPRINT);
        WeakSpotMod.network.sendToServer(HitMessage.withoutTarget(HitKind.SPRINT, streak));
        BOOST.start(VehicleBoostMath.multiplier(settings.sprintBoostMultiplier, settings.sprintBoostMaxMultiplier,
                streak), settings.sprintBoostDurationTicks, ClientWeakSpotHandler.clientTick);
        SPOT.relocate(mc.player);
    }

    /**
     * 走りの加速の修正がかかっている間、バニラの視野の広がり（移動速度から決まる）を MAX_FOV までに抑える。
     * 修正を除いた速さでの視野より狭くはしない。
     */
    @SubscribeEvent
    public static void onFovUpdate(FOVUpdateEvent event) {
        IAttributeInstance speed = event.getEntity().getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        AttributeModifier boost = speed == null ? null : speed.getModifier(MoveHits.SPRINT_MODIFIER);
        if (boost == null) {
            return;
        }
        float walk = event.getEntity().capabilities.getWalkSpeed();
        if (walk <= 0) {
            return;
        }
        double value = speed.getAttributeValue();
        double without = value / (1 + boost.getAmount());
        // バニラ: 視野 *= (速さ / 歩く速さ + 1) / 2。修正を除いた速さの視野に直す
        float plain = (float) (event.getNewfov() * (without / walk + 1) / (value / walk + 1));
        event.setNewfov(Math.min(event.getNewfov(), Math.max(plain, MAX_FOV)));
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
        long now = ClientWeakSpotHandler.clientTick;
        boolean bar = WeakSpotConfig.sprintBoostBarEnabled && BOOST.isActive(now) && !mc.player.isRiding();
        if (!SPOT.has() && !bar) {
            return;
        }
        HudSpot.beginOverlay();
        SPOT.draw(mc);
        if (bar) {
            HudSpot.gauge(mc, BOOST.remaining(now, event.getPartialTicks()), MarkerLook.color(HitKind.SPRINT, RGB),
                    true);
        }
        HudSpot.endOverlay();
    }
}
