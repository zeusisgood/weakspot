package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.config.SyncedSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 照準のまわりの弱点（AimSpotKind）を、まとめて回す（1.8.6。それまでは種類ごとのクラスが、それぞれにイベントを受けていた）。
 * 毎 tick 出す・消す、毎フレーム当たりを見る（tick 単位だと素早い照準の動きを取りこぼす）、HUD に描く。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
final class AimSpots {

    /** 種類を足すときは、ここに足す。 */
    private static final AimSpotKind[] KINDS = {
            new VehicleSpot(),
            new EatSpot(),
            new BowSpot(),
            new LadderSpot(),
            new SprintSpot(),
            new ElytraSpot(),
            new ThrowSpot(),
            new MeleeSpot(),
            new PortalSpot(),
    };

    private AimSpots() {
    }

    /** その種類の照準のまわりの弱点が、今出ているか（コンボの種類の表示。1.8.7）。 */
    static boolean isShown(HitKind kind) {
        for (AimSpotKind k : KINDS) {
            if (k.kind == kind) {
                return k.spot.has();
            }
        }
        return false;
    }

    /** 弱点の一時オフ、ワールドを出たとき。 */
    static void clearAll() {
        for (AimSpotKind k : KINDS) {
            k.clear();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.isGamePaused() || mc.player == null) {
            return;
        }
        for (AimSpotKind k : KINDS) {
            if (event.phase == TickEvent.Phase.START) {
                k.beforeTick(mc);
                if (!k.eligible(mc)) {
                    k.spot.clear();
                } else {
                    k.spot.ensure(mc.player, k.placement(mc.player));
                }
                k.afterTick(mc);
            } else {
                k.tickEnd(mc);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        for (AimSpotKind k : KINDS) {
            if (!k.spot.aimed(mc, event.getPartialTicks()) || !k.eligible(mc) || !k.canAim(mc)) {
                continue;
            }
            SyncedSettings settings = ClientSettings.get();
            if (!ClientWeakSpotHandler.canHitNow(k.kind, k.minHitInterval(settings))) {
                continue;
            }
            EntityPlayerSP player = mc.player;
            int streak = ClientWeakSpotHandler.registerHit(k.kind);
            WeakSpotMod.network.sendToServer(k.message(player, streak));
            k.onHit(mc, player, settings, streak);
            if (k.keepAfterHit(player)) {
                k.spot.relocate(player);
            } else {
                k.spot.clear();
            }
        }
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
        float partialTicks = event.getPartialTicks();
        for (AimSpotKind k : KINDS) {
            boolean gauge = k.hasGauge(mc, partialTicks);
            if (!k.spot.has() && !gauge) {
                continue;
            }
            HudSpot.beginOverlay();
            k.spot.draw(mc);
            if (gauge) {
                k.drawGauge(mc, partialTicks);
            }
            HudSpot.endOverlay();
            if (gauge) {
                k.drawAfterOverlay(mc, partialTicks);
            }
        }
    }

    @SubscribeEvent
    public static void onFovUpdate(FOVUpdateEvent event) {
        for (AimSpotKind k : KINDS) {
            k.onFovUpdate(event);
        }
    }
}
