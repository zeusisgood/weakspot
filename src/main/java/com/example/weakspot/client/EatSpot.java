package com.example.weakspot.client;

import com.example.weakspot.EatDraw;
import com.example.weakspot.VehicleTargets;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.network.HitMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 食事・飲み物の弱点（自分だけ。1.6.0）。食べている・飲んでいる間、照準から離れた所（馬・豚に乗っているときは真上か
 * 真下だけ）に緑の弱点を出し、照準を合わせるだけでヒットにする。当てると食べ終わるまでの時間が縮む（EatDraw。
 * クライアントもサーバーの返事を待たずに縮める）。乗っていれば、乗り物の加速も続ける。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
final class EatSpot {

    /** 緑 #7CFC00。 */
    private static final HudSpot SPOT = new HudSpot(HitKind.EAT, 0x7CFC00);

    private EatSpot() {
    }

    static void clear() {
        SPOT.clear();
    }

    private static boolean eligible(Minecraft mc) {
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null || !KindSwitches.isEnabled(HitKind.EAT)
                || player.capabilities.isCreativeMode || player.isSpectator()) {
            return false;
        }
        SyncedSettings settings = ClientSettings.get();
        return settings.eatWeakSpotEnabled && settings.eatHitTicks > 0 && EatDraw.isEating(player);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.START || mc.isGamePaused()) {
            return;
        }
        if (!eligible(mc)) {
            SPOT.clear();
        } else {
            SPOT.ensure(mc.player, VehicleTargets.isSteeredByLook(mc.player));
        }
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!SPOT.aimed(mc, event.getPartialTicks()) || !eligible(mc)) {
            return;
        }
        SyncedSettings settings = ClientSettings.get();
        if (!ClientWeakSpotHandler.canHitNow(HitKind.EAT, settings.eatMinHitIntervalTicks)) {
            return;
        }
        int streak = ClientWeakSpotHandler.registerHit(HitKind.EAT);
        WeakSpotMod.network.sendToServer(HitMessage.withoutTarget(HitKind.EAT, streak));
        EatDraw.add(mc.player, settings.eatHitTicks);
        VehicleSpot.onRiderHit(streak);
        SPOT.relocate(mc.player);
    }

    @SubscribeEvent
    public static void onOverlayPost(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.gameSettings.hideGUI || !SPOT.has()) {
            return;
        }
        HudSpot.beginOverlay();
        SPOT.draw(mc);
        HudSpot.endOverlay();
    }
}
