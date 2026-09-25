package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.VehicleBoostMath;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.network.HitMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.MoverType;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * はしごの弱点（自分だけ。1.7.0）。はしご・ツタ（バニラの isOnLadder に当たるもの）を登り降りしている間、照準の
 * 真上か真下に木の茶色の弱点を出し、照準を合わせるだけでヒットにする（左右に向きを変えると、はしごから外れるため）。
 * 速さはクライアントで足す（プレイヤーの動きはクライアントが決める。ボートと同じ方式）。サーバーは検証だけ（MoveHits）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
final class LadderSpot {

    /** 登り降りしているとみなす、1 tick の縦の移動（ブロック）。 */
    private static final double MOVING_SPEED = 0.05;
    /** 木の茶色 #C8A060。 */
    private static final int RGB = 0xC8A060;

    private static final HudSpot SPOT = new HudSpot(HitKind.LADDER, RGB);
    private static final TimedBoost BOOST = new TimedBoost();

    private LadderSpot() {
    }

    static void clear() {
        SPOT.clear();
        BOOST.clear();
    }

    /** はしごを登り降りしているか（弱点を出す条件と、速さを足す条件）。 */
    private static boolean climbing(EntityPlayerSP player) {
        return player.isOnLadder() && !player.isRiding() && !player.isElytraFlying()
                && Math.abs(player.posY - player.prevPosY) >= MOVING_SPEED;
    }

    private static boolean eligible(Minecraft mc) {
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null || !KindSwitches.isEnabled(HitKind.LADDER)
                || player.capabilities.isCreativeMode || player.isSpectator() || player.isHandActive()) {
            return false;
        }
        return ClientSettings.get().ladderWeakSpotEnabled && climbing(player);
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
                SPOT.ensure(mc.player, true);
            }
        } else {
            push(mc.player);
        }
    }

    /**
     * 加速中は、その tick に登り降りした分の (倍率 − 1) 倍を、縦に足す。1 tick に足す量に上限は付けない
     * （ユーザーの方針）。はしごの上では落下の距離を数えない（バニラと同じ）。
     */
    private static void push(EntityPlayerSP player) {
        if (!BOOST.isActive(ClientWeakSpotHandler.clientTick) || !KindSwitches.isEnabled(HitKind.LADDER)
                || !climbing(player)) {
            return;
        }
        double dy = (player.posY - player.prevPosY) * VehicleBoostMath.extra(BOOST.multiplier());
        if (Math.abs(dy) > 1e-4) {
            player.move(MoverType.SELF, 0, dy, 0);
            if (player.isOnLadder()) {
                player.fallDistance = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!SPOT.aimed(mc, event.getPartialTicks()) || !eligible(mc)) {
            return;
        }
        SyncedSettings settings = ClientSettings.get();
        if (!ClientWeakSpotHandler.canHitNow(HitKind.LADDER, settings.ladderMinHitIntervalTicks)) {
            return;
        }
        int streak = ClientWeakSpotHandler.registerHit(HitKind.LADDER);
        WeakSpotMod.network.sendToServer(HitMessage.withoutTarget(HitKind.LADDER, streak));
        BOOST.start(VehicleBoostMath.multiplier(settings.ladderBoostMultiplier, settings.ladderBoostMaxMultiplier,
                streak), settings.ladderBoostDurationTicks, ClientWeakSpotHandler.clientTick);
        SPOT.relocate(mc.player);
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
        boolean bar = WeakSpotConfig.ladderBoostBarEnabled && BOOST.isActive(now) && mc.player.isOnLadder();
        if (!SPOT.has() && !bar) {
            return;
        }
        HudSpot.beginOverlay();
        SPOT.draw(mc);
        if (bar) {
            HudSpot.gauge(mc, BOOST.remaining(now, event.getPartialTicks()), MarkerLook.color(HitKind.LADDER, RGB),
                    true);
        }
        HudSpot.endOverlay();
    }
}
