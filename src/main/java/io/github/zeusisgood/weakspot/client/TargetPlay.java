package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.BowMath;
import io.github.zeusisgood.weakspot.common.FishingMath;
import io.github.zeusisgood.weakspot.common.MarkerMotion;
import io.github.zeusisgood.weakspot.common.TargetRules;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
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
 * 的当て（1.11.0）のクライアント側。サーバーの知らせ（TargetMessage）で始まり・終わり、カウントダウン（見本「的 当てる　✕ 当てない」）、
 * 30 秒の間の的（赤白のアーチェリーの的）とハズレ（黒い丸に黄色い ✕。最後の 10 秒）を照準のまわりに出して、照準を合わせたら
 * サーバーへ知らせる。ヒット数はサーバーの数を出す。的は始めたときの向きを中心にした枠の中だけに出し、画面にないときは矢印で
 * 方向を示す。終わったら、結果の板（今回・自己ベスト・サーバーの 1 位・当てた数と ✕ の数・次のご褒美）を出す。
 * 自分ではやめられない（持ち替え・画面を開いても続く）。ラウンドの間は、ほかの種類の弱点を出さない（KindSwitches）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
public final class TargetPlay {

    /** 的の赤・白・中心の黄と、外側の縁（半透明の黒）。 */
    private static final float[] TARGET_RED = rgb(0xD32F2F);
    private static final float[] TARGET_WHITE = rgb(0xFFFFFF);
    private static final float[] TARGET_YELLOW = rgb(0xFFD54F);
    private static final float[] BLACK = rgb(0x000000);
    /** ハズレの丸（半透明の黒）と ✕（黄）。 */
    private static final float[] DECOY_DISK = rgb(0x1A1A1A);
    private static final float[] DECOY_CROSS = rgb(0xFFEB3B);
    /** ハズレに当てたときの「−5」。 */
    private static final int PENALTY_RGB = 0xE53935;
    /** ほかの人の頭の上の「的当て中」。 */
    static final int OTHERS_RGB = 0xFF8A2A;
    /** ハズレに当てたときの低い音のピッチ（近くの人にも同じ音。HitSounds.playOtherMiss）。 */
    static final float MISS_PITCH = 0.5F;
    private static final int GOLD = 0xFFD700;
    private static final int GRAY = 0xAAAAAA;
    /** 弱点の点を置く、目からの距離（ブロック。向きだけが意味を持つ）。 */
    private static final double SPOT_DISTANCE = 16.0;
    private static final int RESULT_TICKS = 100;
    /** 結果の板の幅と行の高さ。 */
    private static final int BOARD_WIDTH = 210;
    private static final int LINE = 12;
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
        /** 表示の位置（当てて移るときに滑らせ、残像を残す。ほかの照準の弱点と同じ。1.11.0）。 */
        final MarkerMotion motion = new MarkerMotion(0, 0);
        boolean placedOnce;

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
    private static long resultBest;
    private static int resultGood;
    private static int resultDecoys;
    private static String resultTopName = "";
    private static long resultTopScore;

    private TargetPlay() {
    }

    /** ラウンド（カウントダウンを含む）の最中か。ほかの種類の弱点を止めるのに使う。 */
    static boolean active() {
        return active;
    }

    /** サーバーの知らせ（クライアントのスレッド）。 */
    static void receive(TargetMessage message) {
        Minecraft mc = Minecraft.getMinecraft();
        long now = ClientWeakSpotHandler.clientTick;
        int newHits = message.hits();
        long best = message.best();
        boolean newBest = message.newBest();
        int newTier = message.newTier();
        switch (message.type()) {
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
                resultBest = best;
                resultGood = message.good();
                resultDecoys = message.decoys();
                resultTopName = message.topName();
                resultTopScore = message.topScore();
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
        if (s.placedOnce && WeakSpotConfig.client.markers.weakSpotTrailEnabled) {
            s.motion.moveTo(s.yaw, s.pitch, Minecraft.getSystemTime());
        } else {
            s.motion.jumpTo(s.yaw, s.pitch);
        }
        s.placedOnce = true;
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
        // ふだんの動きは、表示も同じだけずらす（滑りと残像は、移るときだけ）
        s.motion.shift(y[0] - s.yaw, p[0] - s.pitch);
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
                playNote(mc, MISS_PITCH, 1.0F);
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
        return projectAt(s.yaw, s.pitch);
    }

    /** 基準の向きからのずれ (u, v) の向きを、画面に写す。 */
    private static double[] projectAt(double u, double v) {
        double[] d = BowMath.vector(anchorYaw + u, anchorPitch + v);
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

    /** 「3」「2」「1」と、見本「(的) 当てる　(✕) 当てない」（的とハズレは本物と同じ絵）。 */
    private static void drawCountdown(Minecraft mc, ScaledResolution res, int count) {
        FontRenderer font = mc.fontRenderer;
        float cx = res.getScaledWidth() / 2F;
        float cy = res.getScaledHeight() / 2F;
        drawScaled(font, String.valueOf(count), cx, cy - 34, 4, 0xFFFFFF);
        String hit = I18n.format("weakspot.target.sample.hit");
        String avoid = I18n.format("weakspot.target.sample.avoid");
        double icon = 6;
        int gap = 18;
        float width = (float) (icon * 2 + 4 + font.getStringWidth(hit) + gap + icon * 2 + 4 + font.getStringWidth(avoid));
        float x = cx - width / 2;
        float y = cy + 26;
        HudSpot.beginOverlay();
        drawTarget(x + icon, y, icon, 1F);
        double decoyX = x + icon * 2 + 4 + font.getStringWidth(hit) + gap + icon;
        drawDecoy(decoyX, y, icon, 1F);
        HudSpot.endOverlay();
        font.drawStringWithShadow(hit, (float) (x + icon * 2 + 4), y - font.FONT_HEIGHT / 2F + 1, 0xFFFFFF);
        font.drawStringWithShadow(avoid, (float) (decoyX + icon + 4), y - font.FONT_HEIGHT / 2F + 1, 0xFFFFFF);
    }

    /** 的とハズレを描く。 */
    private static void drawSpots(Minecraft mc, ScaledResolution res) {
        if (!SCREEN.isValid() || spot == null) {
            return;
        }
        double scale = res.getScaleFactor();
        double radius = TargetRules.radius(TargetRules.phase(into()));
        HudSpot.beginOverlay();
        long nowMs = Minecraft.getSystemTime();
        for (Spot decoy : DECOYS) {
            drawWithTrail(decoy, scale, radius, nowMs);
        }
        if (!drawWithTrail(spot, scale, radius, nowMs)) {
            drawArrow(mc, res, spot);
        }
        HudSpot.endOverlay();
    }

    /**
     * 的かハズレを、残像（移るときに通った跡。薄い絵）と、滑っている途中の表示の位置で描く（weakSpotTrailEnabled が
     * オフなら本当の位置に）。画面に写らなければ false。
     */
    private static boolean drawWithTrail(Spot s, double scale, double radius, long nowMs) {
        boolean trail = WeakSpotConfig.client.markers.weakSpotTrailEnabled;
        if (trail) {
            for (MarkerMotion.Afterimage image : s.motion.afterimages(nowMs)) {
                double[] a = projectAt(image.u, image.v);
                if (a != null) {
                    float alpha = (float) (TRAIL_ALPHA * image.alpha(nowMs));
                    if (s.decoy) {
                        drawDecoy(a[0] / scale, a[1] / scale, radius, alpha);
                    } else {
                        drawTarget(a[0] / scale, a[1] / scale, radius, alpha);
                    }
                }
            }
        }
        double[] m = trail ? s.motion.position(nowMs) : new double[] {s.yaw, s.pitch};
        double[] p = projectAt(m[0], m[1]);
        if (p == null) {
            return false;
        }
        if (s.decoy) {
            drawDecoy(p[0] / scale, p[1] / scale, radius, 1F);
        } else {
            drawTarget(p[0] / scale, p[1] / scale, radius, 1F);
        }
        return true;
    }

    /** 残像の濃さ（ほかの弱点の残像と同じくらい）。 */
    private static final double TRAIL_ALPHA = 0.4;

    /** アーチェリーの的（外から半透明の黒の縁・赤・白・赤・中心の黄）。 */
    static void drawTarget(double x, double y, double radius, float a) {
        ScreenProjection.fill(x, y, radius * 1.15, BLACK, 0.45F * a);
        ScreenProjection.fill(x, y, radius, TARGET_RED, 0.95F * a);
        ScreenProjection.fill(x, y, radius * 0.75, TARGET_WHITE, 0.95F * a);
        ScreenProjection.fill(x, y, radius * 0.5, TARGET_RED, 0.95F * a);
        ScreenProjection.fill(x, y, radius * 0.25, TARGET_YELLOW, a);
    }

    /** ハズレ（半透明の黒い丸に、黄色い太い ✕ と白い縁）。 */
    private static void drawDecoy(double x, double y, double radius, float a) {
        ScreenProjection.fill(x, y, radius, DECOY_DISK, 0.75F * a);
        ScreenProjection.outline(x, y, radius, TARGET_WHITE, 0.9F * a);
        double k = radius * 0.55;
        double w = Math.max(1.2, radius * 0.16);
        bar(x - k, y - k, x + k, y + k, w, DECOY_CROSS, a);
        bar(x - k, y + k, x + k, y - k, w, DECOY_CROSS, a);
    }

    /** 太さ w の線（四角形で描く。線の太さの上限がある環境でも太く見えるように）。 */
    private static void bar(double x0, double y0, double x1, double y1, double w, float[] c, float a) {
        double len = Math.hypot(x1 - x0, y1 - y0);
        double nx = -(y1 - y0) / len * w / 2;
        double ny = (x1 - x0) / len * w / 2;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x0 + nx, y0 + ny, 0).color(c[0], c[1], c[2], a).endVertex();
        buffer.pos(x1 + nx, y1 + ny, 0).color(c[0], c[1], c[2], a).endVertex();
        buffer.pos(x1 - nx, y1 - ny, 0).color(c[0], c[1], c[2], a).endVertex();
        buffer.pos(x0 - nx, y0 - ny, 0).color(c[0], c[1], c[2], a).endVertex();
        tessellator.draw();
    }

    /** 的が画面にないとき、画面の端寄りに的の方向を示す三角（的と同じ赤に白い縁）。 */
    private static void drawArrow(Minecraft mc, ScaledResolution res, Spot s) {
        EntityPlayerSP player = mc.player;
        double dx = BowMath.wrapDegrees(anchorYaw + s.yaw - player.rotationYaw);
        double dy = anchorPitch + s.pitch - player.rotationPitch;
        ScreenProjection.edgeArrow(res, dx, dy, TARGET_RED, TARGET_WHITE);
    }

    /** 画面の上の中央に、ヒット数と残り時間。 */
    private static void drawRoundInfo(Minecraft mc, ScaledResolution res, long into) {
        FontRenderer font = mc.fontRenderer;
        float cx = res.getScaledWidth() / 2F;
        String hitText = I18n.format("weakspot.combo.hit", hits);
        drawScaled(font, hitText, cx, 18, 2, 0xFFFFFF);
        if (bestAtStart > 0) {
            // 自己ベスト。超えたら金で、超えた瞬間は大きく光る
            boolean passed = passedTick >= startTick;
            double glow = passed
                    ? Math.max(0, 1 - (ClientWeakSpotHandler.clientTick - passedTick) / (double) PASS_GLOW_TICKS) : 0;
            String best = I18n.format("weakspot.target.best", bestAtStart);
            float bx = cx + font.getStringWidth(hitText) + 8 + font.getStringWidth(best) / 2F;
            drawScaled(font, best, bx, 19, (float) (1 + 0.6 * glow), passed ? GOLD : GRAY);
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
                    (float) (popup.y - 8 - t), alpha << 24 | PENALTY_RGB);
        }
    }

    /**
     * 結果の板（画面の中央の半透明の黒）: 題・大きな「42 HIT」・NEW BEST!・今回の記録・自己ベスト（あと N）・サーバーの 1 位
     * （自分なら金で「あなた！」）・当てた数と ✕ の数・次のご褒美まで。最後の 1 秒で薄くなる。
     */
    private static void drawResult(Minecraft mc, ScaledResolution res, double t) {
        FontRenderer font = mc.fontRenderer;
        float fade = (float) Math.max(0.03, Math.min(1, (RESULT_TICKS - t) / 20.0));
        int alpha = (int) (255 * fade) << 24;
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {I18n.format("weakspot.target.result.this"), I18n.format("weakspot.combo.hit", resultHits)});
        String best = I18n.format("weakspot.combo.hit", resultBest);
        if (resultBest > resultHits) {
            best += I18n.format("weakspot.target.result.behind", resultBest - resultHits);
        }
        rows.add(new String[] {I18n.format("weakspot.target.result.best"), best});
        boolean youTop = resultTopScore > 0 && resultBest >= resultTopScore;
        String top = resultTopScore <= 0 ? "—"
                : I18n.format("weakspot.combo.hit", resultTopScore) + I18n.format("weakspot.target.result.topName",
                        youTop ? I18n.format("weakspot.target.result.you") : resultTopName);
        rows.add(new String[] {I18n.format("weakspot.target.result.top"), top});
        String counts = I18n.format("weakspot.target.result.counts", resultGood, resultDecoys,
                resultDecoys * TargetRules.MISS_PENALTY);
        TargetRules.Tier next = TargetRules.Tier.of(resultBest).next();
        String reward = next == null ? I18n.format("weakspot.target.result.allDone")
                : I18n.format("weakspot.target.result.next",
                        I18n.format("weakspot.target.tier." + tierKey(next)), next.from - resultBest);

        int height = 14 + 30 + (resultNewBest ? 14 : 0) + LINE * rows.size() + 6 + LINE * 2 + 8;
        float cx = res.getScaledWidth() / 2F;
        float top0 = res.getScaledHeight() / 2F - height / 2F - 10;
        float left = cx - BOARD_WIDTH / 2F;
        HudSpot.beginOverlay();
        ScreenProjection.rect(left, top0, left + BOARD_WIDTH, top0 + height, new float[] {0, 0, 0, 0.6F * fade});
        HudSpot.endOverlay();
        GlStateManager.enableBlend();
        float y = top0 + 6;
        String title = I18n.format("weakspot.target.result.title");
        font.drawStringWithShadow(title, cx - font.getStringWidth(title) / 2F, y, alpha | GRAY);
        y += 14;
        drawScaled(font, TextFormatting.BOLD + I18n.format("weakspot.combo.hit", resultHits), cx, y + 12, 3,
                alpha | 0xFFFFFF);
        y += 30;
        if (resultNewBest) {
            String text = TextFormatting.BOLD + I18n.format("weakspot.target.newBest");
            font.drawStringWithShadow(text, cx - font.getStringWidth(text) / 2F, y, alpha | GOLD);
            y += 14;
        }
        float labelX = left + 14;
        float valueRight = left + BOARD_WIDTH - 14;
        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            font.drawStringWithShadow(row[0], labelX, y, alpha | GRAY);
            int color = i == 2 && youTop ? GOLD : 0xFFFFFF;
            font.drawStringWithShadow(row[1], valueRight - font.getStringWidth(row[1]), y, alpha | color);
            y += LINE;
        }
        y += 6;
        font.drawStringWithShadow(counts, cx - font.getStringWidth(counts) / 2F, y, alpha | 0xFFFFFF);
        y += LINE;
        font.drawStringWithShadow(reward, cx - font.getStringWidth(reward) / 2F, y, alpha | (next == null ? GOLD : 0xFFFFFF));
        GlStateManager.color(1, 1, 1, 1);
    }

    private static float[] rgb(int rgb) {
        return new float[] {(rgb >> 16 & 0xFF) / 255F, (rgb >> 8 & 0xFF) / 255F, (rgb & 0xFF) / 255F};
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
