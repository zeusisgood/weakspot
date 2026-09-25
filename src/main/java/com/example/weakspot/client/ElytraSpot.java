package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.MachineComboBoost;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.network.HitMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * エリトラの弱点（自分だけ。1.7.0）。エリトラで飛んでいる間、照準から 10〜20 度の所に空の青の弱点を出し、照準を
 * 合わせるだけでヒットにする。当てた瞬間に、見ている向きへ一気に飛び出す（トライデントの激流のような急加速）。
 * 速さはクライアントで足す（エリトラの動きは本人のクライアントが決める）。上限は付けない（ユーザーの方針）。
 * 急加速の演出（視野が一瞬広がる、雲の尾、打ち上げの音）は自分の画面だけ。サーバーは検証だけ（MoveHits）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
final class ElytraSpot {

    /** 空の青 #7FB2FF。 */
    private static final int RGB = 0x7FB2FF;
    /** 演出の長さ（tick）。視野はこの間に元に戻り、雲の尾もこの間だけ出す。 */
    private static final int DASH_TICKS = 10;
    /** 当てた瞬間の視野の倍率。 */
    private static final float DASH_FOV = 1.25F;
    private static final int SPARKS = 8;
    private static final int CLOUDS_PER_TICK = 2;

    private static final HudSpot SPOT = new HudSpot(HitKind.ELYTRA, RGB);
    private static long dashTick = Long.MIN_VALUE / 2;

    private ElytraSpot() {
    }

    static void clear() {
        SPOT.clear();
        dashTick = Long.MIN_VALUE / 2;
    }

    private static boolean eligible(Minecraft mc) {
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null || !KindSwitches.isEnabled(HitKind.ELYTRA)
                || player.capabilities.isCreativeMode || player.isSpectator() || player.isHandActive()) {
            return false;
        }
        return ClientSettings.get().elytraWeakSpotEnabled && player.isElytraFlying();
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
            SPOT.ensure(mc.player, false);
        }
        long since = ClientWeakSpotHandler.clientTick - dashTick;
        if (since >= 0 && since < DASH_TICKS && mc.player.isElytraFlying()) {
            trail(mc.player);
        }
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!SPOT.aimed(mc, event.getPartialTicks()) || !eligible(mc)) {
            return;
        }
        SyncedSettings settings = ClientSettings.get();
        if (!ClientWeakSpotHandler.canHitNow(HitKind.ELYTRA, settings.elytraMinHitIntervalTicks)) {
            return;
        }
        int streak = ClientWeakSpotHandler.registerHit(HitKind.ELYTRA);
        WeakSpotMod.network.sendToServer(HitMessage.withoutTarget(HitKind.ELYTRA, streak));
        dash(mc, settings.elytraBoostPower * MachineComboBoost.factor(streak));
        SPOT.relocate(mc.player);
    }

    /** 見ている向きへ power（ブロック/tick）だけ速さを足す（激流と同じく、今の速さに足す）。 */
    private static void dash(Minecraft mc, double power) {
        EntityPlayerSP player = mc.player;
        Vec3d look = player.getLookVec();
        player.motionX += look.x * power;
        player.motionY += look.y * power;
        player.motionZ += look.z * power;
        dashTick = ClientWeakSpotHandler.clientTick;
        for (int i = 0; i < SPARKS; i++) {
            mc.world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, player.posX, player.posY, player.posZ,
                    player.getRNG().nextGaussian() * 0.15, player.getRNG().nextGaussian() * 0.15,
                    player.getRNG().nextGaussian() * 0.15);
        }
        // 1.12 にはトライデントの音がないので、花火の打ち上げの音を少し低めに鳴らす
        mc.world.playSound(player.posX, player.posY, player.posZ, SoundEvents.ENTITY_FIREWORK_LAUNCH,
                SoundCategory.PLAYERS, 1.0F, 0.8F, false);
    }

    /** 急加速のあと、後ろに雲の尾を出す。 */
    private static void trail(EntityPlayerSP player) {
        Vec3d look = player.getLookVec();
        for (int i = 0; i < CLOUDS_PER_TICK; i++) {
            player.world.spawnParticle(EnumParticleTypes.CLOUD, player.posX - look.x, player.posY + 0.5 - look.y,
                    player.posZ - look.z, 0, 0, 0);
        }
    }

    /** 当てた瞬間に視野を DASH_FOV 倍に広げ、DASH_TICKS かけて元に戻す（ease-out）。 */
    @SubscribeEvent
    public static void onFovUpdate(FOVUpdateEvent event) {
        long since = ClientWeakSpotHandler.clientTick - dashTick;
        if (since < 0 || since >= DASH_TICKS) {
            return;
        }
        double left = 1 - since / (double) DASH_TICKS;
        event.setNewfov((float) (event.getNewfov() * (1 + (DASH_FOV - 1) * left * left)));
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
