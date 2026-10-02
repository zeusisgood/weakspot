package io.github.zeusisgood.weakspot.client;

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
 * ヒット数はサーバーの数を出す。的は始めたときの向きを中心にした枠の中だけに出し、画面にないときは矢印で方向を示す。
 * 自分ではやめられない（持ち替え・画面を開いても続く）。ラウンドの間は、ほかの種類の弱点を出さない（KindSwitches）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
public final class TargetPlay {

    static final int SPOT_RGB = 0xFF8A2A;
    static final int DECOY_RGB = 0xE53935;
    private static final int GOLD = 0xFFD700;
    /** 弱点の点を置く、目からの距離（ブロック。向きだけが意味を持つ）。 */
    private static final double SPOT_DISTANCE = 16.0;
    /** 枠の外を向いたときの矢印の大きさと、画面の中心からの距離（画面の短い辺に対する比率）。 */
    private static final double ARROW_SIZE = 7;
    private static final double ARROW_RADIUS = 0.36;
    private static final int RESULT_TICKS = 80;
    private static final int POPUP_TICKS = 20;

    private static final ScreenProjection SCREEN = new ScreenProjection();
    private static final Random RANDOM = new Random();

    /** 方向（yaw / pitch）で持つ的。 */
    private static final class Spot {
        /** 基準の向きからのずれ（度）。 */
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
    /** 始めたときの自己ベストと、このラウンドでそれを超えた tick（超えていなければ負）。 */
    private static long bestAtStart;
    private static long passedTick = Long.MIN_VALUE / 2;
    private static final int PASS_GLOW_TICKS = 12;
    /** 始めたときの向き（的はこのまわりの枠の中だけに出す）。 */
    private static double anchorYaw;
    private static double anchorPitch;
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
                bestAtStart = TargetRecords.best;
                passedTick = Long.MIN_VALUE / 2;
                if (mc.player != null) {
                    anchorYaw = mc.player.rotationYaw;
                    anchorPitch = TargetRules.anchorPitch(mc.player.rotationPitch);
                }
                break;
            case TargetMessage.SCORE:
                hits = newHits;
                if (bestAtStart > 0 && hits > bestAtStart && passedTick < startTick) {
                    // 自己ベストを超えた瞬間
                    passedTick = now;
                    playNote(mc, 2.0F, 0.8F);
                }
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
            place(spot, phase, null);
        }
        while (DECOYS.size() < TargetRules.decoys(phase)) {
            Spot decoy = new Spot(true);
            place(decoy, phase, spot);
            DECOYS.add(decoy);
        }
        move(spot, phase);
        for (Spot decoy : DECOYS) {
            if (now - decoy.placedTick >= TargetRules.DECOY_MOVE_TICKS) {
                place(decoy, phase, spot);
            } else {
                move(decoy, phase);
            }
        }
    }

    /** 始めたときの向きを中心にした、段階の枠の中に置く。avoid があれば、そこから MIN_SEPARATION 以上離す。 */
    private static void place(Spot s, int phase, Spot avoid) {
        double w = TargetRules.windowYaw(phase);
        double h = TargetRules.windowPitch(phase);
        for (int i = 0; i < 20; i++) {
            s.yaw = (RANDOM.nextDouble() * 2 - 1) * w;
            s.pitch = (RANDOM.nextDouble() * 2 - 1) * h;
            if (avoid == null || Math.hypot(s.yaw - avoid.yaw, s.pitch - avoid.pitch) > TargetRules.MIN_SEPARATION) {
                break;
            }
        }
        double speed = TargetRules.speed(phase);
        double heading = RANDOM.nextDouble() * Math.PI * 2;
        s.vyaw = speed * Math.cos(heading);
        s.vpitch = speed * Math.sin(heading);
        s.placedTick = ClientWeakSpotHandler.clientTick;
    }

    /** 段階の速さで、枠の中を動かす（端で跳ね返る）。止まっている段階から動く段階になったら、向きを決め直す。 */
    private static void move(Spot s, int phase) {
        double speed = TargetRules.speed(phase);
        if (speed <= 0) {
            return;
        }
        if (Math.hypot(s.vyaw, s.vpitch) < speed * 0.5) {
            double heading = RANDOM.nextDouble() * Math.PI * 2;
            s.vyaw = speed * Math.cos(heading);
            s.vpitch = speed * Math.sin(heading);
        }
        double[] y = TargetRules.bounce(s.yaw, s.vyaw, TargetRules.windowYaw(phase));
        double[] p = TargetRules.bounce(s.pitch, s.vpitch, TargetRules.windowPitch(phase));
        s.yaw = y[0];
        s.vyaw = y[1];
        s.pitch = p[0];
        s.vpitch = p[1];
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
                place(decoy, phase, spot);
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
            place(spot, phase, previous);
        }
    }

    private static double[] project(Spot s) {
        double[] d = BowMath.vector(anchorYaw + s.yaw, anchorPitch + s.pitch);
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
        } else {
            drawArrow(mc, res, spot);
        }
        for (Spot decoy : DECOYS) {
            double[] d = project(decoy);
            if (d != null) {
                drawCross(d[0] / scale, d[1] / scale, radius);
            }
        }
        HudSpot.endOverlay();
    }

    /** 的が画面にないとき、画面の端寄りに的の方向を示す三角（オレンジ）。 */
    private static void drawArrow(Minecraft mc, ScaledResolution res, Spot s) {
        EntityPlayerSP player = mc.player;
        double dx = BowMath.wrapDegrees(anchorYaw + s.yaw - player.rotationYaw);
        double dy = anchorPitch + s.pitch - player.rotationPitch;
        double len = Math.hypot(dx, dy);
        if (len < 1e-6) {
            return;
        }
        dx /= len;
        dy /= len;
        double r = Math.min(res.getScaledWidth(), res.getScaledHeight()) * ARROW_RADIUS;
        double cx = res.getScaledWidth() / 2.0 + dx * r;
        double cy = res.getScaledHeight() / 2.0 + dy * r;
        float red = (SPOT_RGB >> 16 & 0xFF) / 255F;
        float green = (SPOT_RGB >> 8 & 0xFF) / 255F;
        float blue = (SPOT_RGB & 0xFF) / 255F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(cx + dx * ARROW_SIZE, cy + dy * ARROW_SIZE, 0).color(red, green, blue, 0.95F).endVertex();
        double backX = cx - dx * ARROW_SIZE * 0.6;
        double backY = cy - dy * ARROW_SIZE * 0.6;
        double sideX = -dy * ARROW_SIZE * 0.7;
        double sideY = dx * ARROW_SIZE * 0.7;
        buffer.pos(backX + sideX, backY + sideY, 0).color(red, green, blue, 0.95F).endVertex();
        buffer.pos(backX - sideX, backY - sideY, 0).color(red, green, blue, 0.95F).endVertex();
        tessellator.draw();
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
        String hitText = I18n.format("weakspot.combo.hit", hits);
        drawScaled(font, hitText, cx, 18, 2, SPOT_RGB);
        if (bestAtStart > 0) {
            // 自己ベスト。超えたら金で、超えた瞬間は大きく光る
            boolean passed = passedTick >= startTick;
            double glow = passed
                    ? Math.max(0, 1 - (ClientWeakSpotHandler.clientTick - passedTick) / (double) PASS_GLOW_TICKS) : 0;
            String best = I18n.format("weakspot.target.best", bestAtStart);
            float bx = cx + font.getStringWidth(hitText) + 8 + font.getStringWidth(best) / 2F;
            drawScaled(font, best, bx, 19, (float) (1 + 0.6 * glow), passed ? GOLD : 0xAAAAAA);
        }
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
