package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.ItemTarget;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.BowMath;
import io.github.zeusisgood.weakspot.common.FishingMath;
import io.github.zeusisgood.weakspot.common.MarkerShape;
import io.github.zeusisgood.weakspot.common.TargetRules;
import io.github.zeusisgood.weakspot.network.TargetActionMessage;
import io.github.zeusisgood.weakspot.network.TargetMessage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;

/**
 * 的当て（1.11.0）のクライアント側。サーバーの知らせ（TargetMessage）で始まり・終わり、カウントダウン（見本「● 当てる　✕ 当てない」）、
 * 30 秒の間の弱点（オレンジの ●）とハズレ（赤い ✕。最後の 10 秒）を照準のまわりに出して、照準を合わせたらサーバーへ知らせる。
 * ヒット数はサーバーの数を出す。画面を開く・持ち替えたらやめる。ラウンドの間は、ほかの種類の弱点を出さない（KindSwitches）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
public final class TargetPlay {

    static final int SPOT_RGB = 0xFF8A2A;
    static final int DECOY_RGB = 0xE53935;
    private static final int GOLD = 0xFFD700;
    /** 弱点の点を置く、目からの距離（ブロック。向きだけが意味を持つ）。 */
    private static final double SPOT_DISTANCE = 16.0;
    /** 照準からこれより離れた弱点は、照準の近くに出し直す（度）。 */
    private static final double RELOCATE_DEGREES = 35.0;
    private static final int RESULT_TICKS = 80;
    private static final int POPUP_TICKS = 20;

    private static final ScreenProjection SCREEN = new ScreenProjection();
    private static final Random RANDOM = new Random();

    /** 方向（yaw / pitch）で持つ的。 */
    private static final class Spot {
        double yaw;
        double pitch;
        double vyaw;
        double vpitch;
        final boolean decoy;
        long placedTick;

        Spot(boolean decoy) {
            this.decoy = decoy;
        }
    }

    /** ハズレに当てたときの「−5」。 */
    private static final class Popup {
        final double x;
        final double y;
        final long tick;

        Popup(double x, double y, long tick) {
            this.x = x;
            this.y = y;
            this.tick = tick;
        }
    }

    private static boolean active;
    private static long startTick;
    private static int hits;
    private static int streak;
    private static long lastHitTick = Long.MIN_VALUE / 2;
    private static Spot spot;
    private static final List<Spot> DECOYS = new ArrayList<>();
    private static final List<Popup> POPUPS = new ArrayList<>();
    private static double eyeX;
    private static double eyeY;
    private static double eyeZ;
    private static int lastCountdown;
    /** 結果の表示。 */
    private static long resultTick = Long.MIN_VALUE / 2;
    private static int resultHits;
    private static boolean resultNewBest;

    private TargetPlay() {
    }

    /** ラウンド（カウントダウンを含む）の最中か。ほかの種類の弱点を止めるのに使う。 */
    static boolean active() {
        return active;
    }

    /** サーバーの知らせ（クライアントのスレッド）。 */
    static void receive(byte type, int newHits, long best, boolean newBest, int newTier) {
        Minecraft mc = Minecraft.getMinecraft();
        long now = ClientWeakSpotHandler.clientTick;
        switch (type) {
            case TargetMessage.START:
                clear();
                active = true;
                startTick = now;
                lastCountdown = 0;
                break;
            case TargetMessage.SCORE:
                hits = newHits;
                break;
            case TargetMessage.END:
                clear();
                resultTick = now;
                resultHits = newHits;
                resultNewBest = newBest;
                TargetRecords.best = best;
                TargetRecords.rounds++;
                if (newBest && newHits > 0) {
                    MilestoneEffects.comboFireworks(3, GOLD);
                }
                playNote(mc, 1.0F, 1.0F);
                if (newTier > 0 && mc.player != null) {
                    TargetRules.Tier tier = TargetRules.Tier.values()[newTier];
                    TextComponentTranslation text = new TextComponentTranslation("weakspot.target.tierReached",
                            new TextComponentTranslation("weakspot.target.tier." + tierKey(tier)), tier.from);
                    text.getStyle().setColor(TextFormatting.GOLD);
                    mc.player.sendMessage(text);
                }
                break;
            case TargetMessage.CANCEL:
                if (active && mc.ingameGUI != null) {
                    mc.ingameGUI.setOverlayMessage(I18n.format("weakspot.target.cancelled"), false);
                }
                clear();
                break;
            case TargetMessage.RECORD:
                TargetRecords.best = best;
                break;
            default:
                break;
        }
    }

    static String tierKey(TargetRules.Tier tier) {
        return tier.name().toLowerCase(java.util.Locale.ROOT);
    }

    private static void clear() {
        active = false;
        hits = 0;
        streak = 0;
        spot = null;
        DECOYS.clear();
        POPUPS.clear();
        SCREEN.invalidate();
    }

    /** 自分からやめる（画面を開いた・持ち替えた）。 */
    private static void cancelByClient(Minecraft mc) {
        WeakSpotMod.network.sendToServer(new TargetActionMessage(TargetActionMessage.CANCEL));
        mc.ingameGUI.setOverlayMessage(I18n.format("weakspot.target.cancelled"), false);
        clear();
    }

    private static long into() {
        return ClientWeakSpotHandler.clientTick - startTick - TargetRules.COUNTDOWN_TICKS;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !active) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null) {
            clear();
            return;
        }
        if (mc.currentScreen != null || !holdsTarget(player)) {
            cancelByClient(mc);
            return;
        }
        long into = into();
        if (into < 0) {
            // カウントダウンの音（3・2・1）
            int count = (int) Math.ceil(-into / 20.0);
            if (count != lastCountdown) {
                lastCountdown = count;
                playNote(mc, 0.8F, 0.7F);
            }
            return;
        }
        if (into == 0) {
            playNote(mc, 1.6F, 0.9F);
        }
        if (into >= TargetRules.ROUND_TICKS) {
            // 終わりの知らせ（サーバー）を待つ間は、的を消す
            spot = null;
            DECOYS.clear();
            return;
        }
        int phase = TargetRules.phase(into);
        long now = ClientWeakSpotHandler.clientTick;
        if (spot == null) {
            spot = new Spot(false);
            place(spot, player, phase, null);
        }
        while (DECOYS.size() < TargetRules.decoys(phase)) {
            Spot decoy = new Spot(true);
            place(decoy, player, phase, spot);
            DECOYS.add(decoy);
        }
        move(spot, player, phase);
        for (Spot decoy : DECOYS) {
            if (now - decoy.placedTick >= TargetRules.DECOY_MOVE_TICKS) {
                place(decoy, player, phase, spot);
            } else {
                move(decoy, player, phase);
            }
        }
    }

    private static boolean holdsTarget(EntityPlayerSP player) {
        return player.getHeldItem(EnumHand.MAIN_HAND).getItem() == ItemTarget.INSTANCE
                || player.getHeldItem(EnumHand.OFF_HAND).getItem() == ItemTarget.INSTANCE;
    }

    /** 照準のまわり（段階の距離）に置く。avoid があれば、そこから離す。 */
    private static void place(Spot s, EntityPlayerSP player, int phase, Spot avoid) {
        double min = TargetRules.minOffset(phase);
        double max = TargetRules.maxOffset(phase);
        for (int i = 0; i < 20; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double r = min + RANDOM.nextDouble() * (max - min);
            s.yaw = player.rotationYaw + r * Math.cos(angle);
            s.pitch = Math.max(-80, Math.min(80, player.rotationPitch + r * Math.sin(angle)));
            boolean farFromPrevious = avoid == null
                    || BowMath.angleBetween(s.yaw, s.pitch, avoid.yaw, avoid.pitch) > TargetRules.radius(phase) / 2;
            if (farFromPrevious) {
                break;
            }
        }
        double speed = TargetRules.speed(phase);
        double heading = RANDOM.nextDouble() * Math.PI * 2;
        s.vyaw = speed * Math.cos(heading);
        s.vpitch = speed * Math.sin(heading);
        s.placedTick = ClientWeakSpotHandler.clientTick;
    }

    /** 段階の速さで動かす。照準から離れすぎたら、照準の方へ向きを変える。とても離れたら出し直す。 */
    private static void move(Spot s, EntityPlayerSP player, int phase) {
        double speed = TargetRules.speed(phase);
        double offset = BowMath.angleBetween(s.yaw, s.pitch, player.rotationYaw, player.rotationPitch);
        if (offset > RELOCATE_DEGREES) {
            place(s, player, phase, null);
            return;
        }
        if (speed <= 0) {
            return;
        }
        if (offset > TargetRules.maxOffset(phase)) {
            double dy = BowMath.wrapDegrees(player.rotationYaw - s.yaw);
            double dp = player.rotationPitch - s.pitch;
            double len = Math.max(1e-6, Math.hypot(dy, dp));
            s.vyaw = speed * dy / len;
            s.vpitch = speed * dp / len;
        } else if (Math.hypot(s.vyaw, s.vpitch) < speed * 0.5) {
            double heading = RANDOM.nextDouble() * Math.PI * 2;
            s.vyaw = speed * Math.cos(heading);
            s.vpitch = speed * Math.sin(heading);
        }
        s.yaw += s.vyaw;
        s.pitch = Math.max(-80, Math.min(80, s.pitch + s.vpitch));
    }

    /** 毎フレーム、照準が的に合ったかを見る（RenderWorldLastEvent。ほかの照準の弱点と同じ）。 */
    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        SCREEN.invalidate();
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (!active || player == null || spot == null || into() < 0 || into() >= TargetRules.ROUND_TICKS) {
            return;
        }
        float partial = event.getPartialTicks();
        eyeX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partial;
        eyeY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partial + player.getEyeHeight();
        eyeZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partial;
        if (!SCREEN.capture(mc, partial) || mc.currentScreen != null) {
            return;
        }
        long now = ClientWeakSpotHandler.clientTick;
        if (now - lastHitTick < TargetRules.MIN_HIT_INTERVAL_TICKS) {
            return;
        }
        int phase = TargetRules.phase(into());
        double scale = new ScaledResolution(mc).getScaleFactor();
        double allowed = FishingMath.allowedAngle(TargetRules.radius(phase) * scale, SCREEN.fovDegrees(),
                SCREEN.viewportHeight());
        for (Spot decoy : DECOYS) {
            double[] p = project(decoy);
            if (p != null && p[2] <= allowed) {
                lastHitTick = now;
                streak = 0;
                POPUPS.add(new Popup(p[0] / scale, p[1] / scale, now));
                WeakSpotMod.network.sendToServer(new TargetActionMessage(TargetActionMessage.DECOY));
                playNote(mc, 0.5F, 1.0F);
                place(decoy, player, phase, spot);
                return;
            }
        }
        double[] p = project(spot);
        if (p != null && p[2] <= allowed) {
            lastHitTick = now;
            streak++;
            HitSounds.playHit(streak);
            WeakSpotMod.network.sendToServer(new TargetActionMessage(TargetActionMessage.HIT));
            Spot previous = new Spot(false);
            previous.yaw = spot.yaw;
            previous.pitch = spot.pitch;
            place(spot, player, phase, previous);
        }
    }

    private static double[] project(Spot s) {
        double[] d = BowMath.vector(s.yaw, s.pitch);
        return SCREEN.project(eyeX + d[0] * SPOT_DISTANCE, eyeY + d[1] * SPOT_DISTANCE, eyeZ + d[2] * SPOT_DISTANCE);
    }

    private static void playNote(Minecraft mc, float pitch, float volume) {
        if (mc.player != null && mc.world != null) {
            mc.world.playSound(mc.player.posX, mc.player.posY, mc.player.posZ, SoundEvents.BLOCK_NOTE_PLING,
                    SoundCategory.PLAYERS, volume, pitch, false);
        }
    }

    @SubscribeEvent
    public static void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.gameSettings.hideGUI) {
            return;
        }
        ScaledResolution res = event.getResolution();
        double now = ClientWeakSpotHandler.clientTick + event.getPartialTicks();
        if (active) {
            long into = into();
            if (into < 0) {
                drawCountdown(mc, res, (int) Math.ceil(-into / 20.0));
            } else {
                drawSpots(mc, res);
                drawRoundInfo(mc, res, into);
            }
            drawPopups(mc, now);
        } else if (now - resultTick < RESULT_TICKS) {
            drawResult(mc, res, now - resultTick);
        }
    }

    /** 「3」「2」「1」と、見本「● 当てる　✕ 当てない」。 */
    private static void drawCountdown(Minecraft mc, ScaledResolution res, int count) {
        FontRenderer font = mc.fontRenderer;
        float cx = res.getScaledWidth() / 2F;
        float cy = res.getScaledHeight() / 2F;
        drawScaled(font, String.valueOf(count), cx, cy - 34, 4, 0xFFFFFF);
        String hit = "● " + I18n.format("weakspot.target.sample.hit");
        String avoid = "✕ " + I18n.format("weakspot.target.sample.avoid");
        String gap = "    ";
        float width = font.getStringWidth(hit + gap + avoid);
        float x = cx - width / 2;
        float y = cy + 22;
        font.drawStringWithShadow(hit, x, y, SPOT_RGB);
        font.drawStringWithShadow(avoid, x + font.getStringWidth(hit + gap), y, DECOY_RGB);
    }

    /** 的とハズレを描く。 */
    private static void drawSpots(Minecraft mc, ScaledResolution res) {
        if (!SCREEN.isValid() || spot == null) {
            return;
        }
        double scale = res.getScaleFactor();
        double radius = TargetRules.radius(TargetRules.phase(into()));
        HudSpot.beginOverlay();
        double[] p = project(spot);
        if (p != null) {
            ScreenProjection.drawMarker(p[0] / scale, p[1] / scale, radius, MarkerShape.CIRCLE,
                    ScreenProjection.lookOf(SPOT_RGB), 0.55F, 0.95F);
        }
        for (Spot decoy : DECOYS) {
            double[] d = project(decoy);
            if (d != null) {
                drawCross(d[0] / scale, d[1] / scale, radius);
            }
        }
        HudSpot.endOverlay();
    }

    /** 赤い ✕（輪と斜めの 2 本）。 */
    private static void drawCross(double x, double y, double radius) {
        float r = (DECOY_RGB >> 16 & 0xFF) / 255F;
        float g = (DECOY_RGB >> 8 & 0xFF) / 255F;
        float b = (DECOY_RGB & 0xFF) / 255F;
        ScreenProjection.outline(x, y, radius, new float[] {r, g, b}, 0.9F);
        double k = radius * 0.6;
        GlStateManager.glLineWidth(3.0F);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x - k, y - k, 0).color(r, g, b, 1F).endVertex();
        buffer.pos(x + k, y + k, 0).color(r, g, b, 1F).endVertex();
        buffer.pos(x - k, y + k, 0).color(r, g, b, 1F).endVertex();
        buffer.pos(x + k, y - k, 0).color(r, g, b, 1F).endVertex();
        tessellator.draw();
        GlStateManager.glLineWidth(2.0F);
    }

    /** 画面の上の中央に、ヒット数と残り時間。 */
    private static void drawRoundInfo(Minecraft mc, ScaledResolution res, long into) {
        FontRenderer font = mc.fontRenderer;
        float cx = res.getScaledWidth() / 2F;
        drawScaled(font, I18n.format("weakspot.combo.hit", hits), cx, 18, 2, SPOT_RGB);
        double left = Math.max(0, (TargetRules.ROUND_TICKS - into) / 20.0);
        String time = I18n.format("weakspot.target.timeLeft", String.format("%.1f", left));
        font.drawStringWithShadow(time, cx - font.getStringWidth(time) / 2F, 32, 0xFFFFFF);
    }

    private static void drawPopups(Minecraft mc, double now) {
        FontRenderer font = mc.fontRenderer;
        Iterator<Popup> it = POPUPS.iterator();
        while (it.hasNext()) {
            Popup popup = it.next();
            double t = now - popup.tick;
            if (t >= POPUP_TICKS) {
                it.remove();
                continue;
            }
            int alpha = (int) Math.max(8, 255 * (1 - t / POPUP_TICKS));
            String text = "-" + TargetRules.MISS_PENALTY;
            font.drawStringWithShadow(text, (float) popup.x - font.getStringWidth(text) / 2F,
                    (float) (popup.y - 8 - t), alpha << 24 | DECOY_RGB);
        }
    }

    /** 終わりの「42 HIT」と「NEW BEST!」。 */
    private static void drawResult(Minecraft mc, ScaledResolution res, double t) {
        FontRenderer font = mc.fontRenderer;
        float cx = res.getScaledWidth() / 2F;
        float cy = res.getScaledHeight() / 2F;
        int alpha = (int) Math.max(8, Math.min(255, 255 * (RESULT_TICKS - t) / 20.0));
        drawScaled(font, TextFormatting.BOLD + I18n.format("weakspot.combo.hit", resultHits), cx, cy - 40, 3,
                alpha << 24 | SPOT_RGB);
        if (resultNewBest) {
            drawScaled(font, TextFormatting.BOLD + I18n.format("weakspot.target.newBest"), cx, cy - 16, 2,
                    alpha << 24 | GOLD);
        }
    }

    private static void drawScaled(FontRenderer font, String text, float cx, float cy, float scale, int argb) {
        int color = (argb >>> 24) == 0 ? 0xFF000000 | argb : argb;
        GlStateManager.enableBlend();
        GlStateManager.pushMatrix();
        GlStateManager.translate(cx, cy, 0);
        GlStateManager.scale(scale, scale, 1);
        font.drawStringWithShadow(text, -font.getStringWidth(text) / 2F, -font.FONT_HEIGHT / 2F, color);
        GlStateManager.popMatrix();
        GlStateManager.color(1, 1, 1, 1);
    }
}
