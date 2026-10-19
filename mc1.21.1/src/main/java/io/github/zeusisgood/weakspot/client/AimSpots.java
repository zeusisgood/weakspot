package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** 毎フレーム当たりを見る（tick 単位だと素早い照準の動きを取りこぼす）。 */
@EventBusSubscriber(modid = WeakSpotMod.MODID, value = Dist.CLIENT)
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
            new SwimSpot(),
            new FallSpot(),
    };

    private AimSpots() {
    }

    static boolean isShown(HitKind kind) {
        for (AimSpotKind k : KINDS) {
            if (k.kind == kind) {
                return k.spot.has();
            }
        }
        return false;
    }

    static void clearAll() {
        for (AimSpotKind k : KINDS) {
            k.clear();
        }
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused() || mc.player == null) {
            return;
        }
        for (AimSpotKind k : KINDS) {
            k.beforeTick(mc);
            if (!k.allowed(mc)) {
                k.spot.clear();
                k.missedTicks = 0;
            } else if (k.wanted(mc.player, ClientSettings.get())) {
                k.missedTicks = 0;
                k.spot.ensure(mc.player, k.placement(mc.player));
            } else if (!k.spot.has() || ++k.missedTicks > k.keepTicks()) {
                // keepTicks の間は残す（当たりは eligible で止まる）
                k.spot.clear();
                k.missedTicks = 0;
            }
            k.afterTick(mc);
        }
    }

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused() || mc.player == null) {
            return;
        }
        for (AimSpotKind k : KINDS) {
            k.tickEnd(mc);
        }
    }

    /** そのフレームの行列は ClientWeakSpotHandler.onRenderLevel が同じ段で覚えるので、そのあとに回す。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        float partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        for (AimSpotKind k : KINDS) {
            if (!k.spot.aimed(mc, partialTicks) || !k.eligible(mc) || !k.canAim(mc)) {
                continue;
            }
            SyncedSettings settings = ClientSettings.get();
            if (!OwnHits.canHit(k.kind, k.minHitInterval(settings))) {
                continue;
            }
            LocalPlayer player = mc.player;
            int streak = OwnHits.register(k.kind);
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
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        float partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        for (AimSpotKind k : KINDS) {
            boolean gauge = k.hasGauge(mc, partialTicks);
            if (!k.spot.has() && !gauge) {
                continue;
            }
            // バニラの HUD の溜まった描画を先に出して、弱点を上に重ねる
            graphics.flush();
            Draw.setPose(graphics.pose().last().pose());
            HudSpot.beginOverlay();
            k.spot.draw(mc);
            if (gauge) {
                k.drawGauge(mc, partialTicks);
            }
            HudSpot.endOverlay();
            Draw.setPose(null);
            if (gauge) {
                k.drawAfterOverlay(graphics, mc, partialTicks);
            }
        }
    }

    @SubscribeEvent
    public static void onFovUpdate(ComputeFovModifierEvent event) {
        for (AimSpotKind k : KINDS) {
            k.onFovUpdate(event);
        }
    }
}
