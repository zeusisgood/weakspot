package io.github.zeusisgood.weakspot.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
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
import java.util.Locale;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 的当てのクライアント側。ヒット数はサーバーの数を出す。自分ではやめられない（持ち替え・画面を開いても続く）。
 * ラウンドの間は、ほかの種類の弱点を出さない（KindSwitches）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Dist.CLIENT)
public final class TargetPlay {

    private static final float[] TARGET_RED = rgb(0xD32F2F);
    private static final float[] TARGET_WHITE = rgb(0xFFFFFF);
    private static final float[] TARGET_YELLOW = rgb(0xFFD54F);
    private static final float[] BLACK = rgb(0x000000);
    private static final float[] DECOY_DISK = rgb(0x1A1A1A);
    private static final float[] DECOY_CROSS = rgb(0xFFEB3B);
    private static final int PENALTY_RGB = 0xE53935;
    /** ほかの人の頭の上の「的当て中」。 */
    static final int OTHERS_RGB = 0xFF8A2A;
    /** 近くの人にも同じ音（HitSounds.playOtherMiss）。 */
    static final float MISS_PITCH = 0.5F;
    private static final int GOLD = 0xFFD700;
    private static final int GRAY = 0xAAAAAA;
    /** ブロック。向きだけが意味を持つ。 */
    private static final double SPOT_DISTANCE = 16.0;
    private static final int RESULT_TICKS = 100;
    private static final int BOARD_WIDTH = 210;
    private static final int LINE = 12;
    private static final int POPUP_TICKS = 20;

    private static final ScreenProjection SCREEN = new ScreenProjection();
    private static final Random RANDOM = new Random();

    private static final class Spot {
        /** 基準の向きからのずれ（度）。 */
        double yaw;
        double pitch;
        double vyaw;
        double vpitch;
        final boolean decoy;
        long placedTick;
        /** 当てて移るときに滑らせ、残像を残す（ほかの照準の弱点と同じ）。 */
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
    /** 超えていなければ負。 */
    private static long bestAtStart;
    private static long passedTick = Long.MIN_VALUE / 2;
    private static final int PASS_GLOW_TICKS = 12;
    private static double anchorYaw;
    private static double anchorPitch;
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

    /** カウントダウンを含む。 */
    static boolean active() {
        return active;
    }

    /** クライアントのスレッド。 */
    static void receive(TargetMessage message) {
        Minecraft mc = Minecraft.getInstance();
        long now = ClientWeakSpotHandler.clientTick;
        int newHits = message.hits();
        long best = message.best();
        boolean newBest = message.newBest();
        int newTier = message.newTier();
        switch (message.kind()) {
            case TargetMessage.START:
                clear();
                active = true;
                startTick = now;
                lastCountdown = 0;
                bestAtStart = TargetRecords.best;
                passedTick = Long.MIN_VALUE / 2;
                if (mc.player != null) {
                    anchorYaw = mc.player.getYRot();
                    anchorPitch = TargetRules.anchorPitch(mc.player.getXRot());
                }
                break;
            case TargetMessage.SCORE:
                hits = newHits;
                if (bestAtStart > 0 && hits > bestAtStart && passedTick < startTick) {
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
                    mc.player.displayClientMessage(Component.translatable("weakspot.target.tierReached",
                            Component.translatable("weakspot.target.tier." + tierKey(tier)), tier.from)
                            .withStyle(ChatFormatting.GOLD), false);
                }
                break;
            case TargetMessage.CANCEL:
                if (active && mc.gui != null) {
                    mc.gui.setOverlayMessage(Component.translatable("weakspot.target.cancelled"), false);
                }
                clear();
                break;
            case TargetMessage.RECORD:
                TargetRecords.best = best;
                if (newTier >= 0 && newTier < TargetRules.Tier.values().length) {
                    TargetRecords.earned = TargetRules.Tier.values()[newTier];
                }
                break;
            default:
                break;
        }
    }

    static String tierKey(TargetRules.Tier tier) {
        return tier.name().toLowerCase(Locale.ROOT);
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
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!active) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            clear();
            return;
        }
        long into = into();
        if (into < 0) {
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

    /** avoid があれば、そこから MIN_SEPARATION 以上離す。 */
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
            s.motion.moveTo(s.yaw, s.pitch, Util.getMillis());
        } else {
            s.motion.jumpTo(s.yaw, s.pitch);
        }
        s.placedOnce = true;
    }

    /** 止まっている段階から動く段階になったら、向きを決め直す。 */
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

    /**
     * 毎フレーム見る（ほかの照準の弱点と同じ）。行列は ClientWeakSpotHandler が AFTER_WEATHER で渡すので、
     * それより後の段で見る。
     */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        SCREEN.invalidate();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!active || player == null || spot == null || into() < 0 || into() >= TargetRules.ROUND_TICKS) {
            return;
        }
        float partial = event.getPartialTick();
        Vec3 eye = player.getEyePosition(partial);
        eyeX = eye.x;
        eyeY = eye.y;
        eyeZ = eye.z;
        if (!SCREEN.capture(mc, partial) || mc.screen != null) {
            return;
        }
        long now = ClientWeakSpotHandler.clientTick;
        if (now - lastHitTick < TargetRules.MIN_HIT_INTERVAL_TICKS) {
            return;
        }
        int phase = TargetRules.phase(into());
        double scale = mc.getWindow().getGuiScale();
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

    private static double[] projectAt(double u, double v) {
        double[] d = BowMath.vector(anchorYaw + u, anchorPitch + v);
        return SCREEN.project(eyeX + d[0] * SPOT_DISTANCE, eyeY + d[1] * SPOT_DISTANCE, eyeZ + d[2] * SPOT_DISTANCE);
    }

    private static void playNote(Minecraft mc, float pitch, float volume) {
        if (mc.player != null && mc.level != null) {
            mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, volume, pitch, false);
        }
    }

    @SubscribeEvent
    public static void onOverlay(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        double now = ClientWeakSpotHandler.clientTick + event.getPartialTick();
        if (active) {
            long into = into();
            if (into < 0) {
                drawCountdown(g, mc, (int) Math.ceil(-into / 20.0));
            } else {
                drawSpots(g, mc);
                drawRoundInfo(g, mc, into);
            }
            drawPopups(g, mc, now);
        } else if (now - resultTick < RESULT_TICKS) {
            drawResult(g, mc, now - resultTick);
        }
    }

    /** 図形を GUI の座標で描く間だけ、beginOverlay と GuiGraphics の行列を使う。 */
    private static void beginShapes(GuiGraphics g) {
        HudSpot.beginOverlay();
        Draw.setPose(g.pose().last().pose());
    }

    private static void endShapes() {
        Draw.setPose(null);
        HudSpot.endOverlay();
    }

    private static void drawCountdown(GuiGraphics g, Minecraft mc, int count) {
        Font font = mc.font;
        float cx = mc.getWindow().getGuiScaledWidth() / 2F;
        float cy = mc.getWindow().getGuiScaledHeight() / 2F;
        drawScaled(g, font, String.valueOf(count), cx, cy - 34, 4, 0xFFFFFF);
        String hit = I18n.get("weakspot.target.sample.hit");
        String avoid = I18n.get("weakspot.target.sample.avoid");
        double icon = 6;
        int gap = 18;
        float width = (float) (icon * 2 + 4 + font.width(hit) + gap + icon * 2 + 4 + font.width(avoid));
        float x = cx - width / 2;
        float y = cy + 26;
        beginShapes(g);
        drawTarget(x + icon, y, icon, 1F);
        double decoyX = x + icon * 2 + 4 + font.width(hit) + gap + icon;
        drawDecoy(decoyX, y, icon, 1F);
        endShapes();
        g.drawString(font, hit, (float) (x + icon * 2 + 4), y - font.lineHeight / 2F + 1, 0xFFFFFFFF, true);
        g.drawString(font, avoid, (float) (decoyX + icon + 4), y - font.lineHeight / 2F + 1, 0xFFFFFFFF, true);
    }

    private static void drawSpots(GuiGraphics g, Minecraft mc) {
        if (!SCREEN.isValid() || spot == null) {
            return;
        }
        double scale = mc.getWindow().getGuiScale();
        double radius = TargetRules.radius(TargetRules.phase(into()));
        beginShapes(g);
        long nowMs = Util.getMillis();
        for (Spot decoy : DECOYS) {
            drawWithTrail(decoy, scale, radius, nowMs);
        }
        if (!drawWithTrail(spot, scale, radius, nowMs)) {
            drawArrow(mc, spot);
        }
        endShapes();
    }

    /** 画面に写らなければ false。 */
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

    private static final double TRAIL_ALPHA = 0.4;

    static void drawTarget(double x, double y, double radius, float a) {
        ScreenProjection.fill(x, y, radius * 1.15, BLACK, 0.45F * a);
        ScreenProjection.fill(x, y, radius, TARGET_RED, 0.95F * a);
        ScreenProjection.fill(x, y, radius * 0.75, TARGET_WHITE, 0.95F * a);
        ScreenProjection.fill(x, y, radius * 0.5, TARGET_RED, 0.95F * a);
        ScreenProjection.fill(x, y, radius * 0.25, TARGET_YELLOW, a);
    }

    private static void drawDecoy(double x, double y, double radius, float a) {
        ScreenProjection.fill(x, y, radius, DECOY_DISK, 0.75F * a);
        ScreenProjection.outline(x, y, radius, TARGET_WHITE, 0.9F * a);
        double k = radius * 0.55;
        double w = Math.max(1.2, radius * 0.16);
        bar(x - k, y - k, x + k, y + k, w, DECOY_CROSS, a);
        bar(x - k, y + k, x + k, y - k, w, DECOY_CROSS, a);
    }

    /** 四角形で描く（線の太さの上限がある環境でも太く見えるように）。 */
    private static void bar(double x0, double y0, double x1, double y1, double w, float[] c, float a) {
        double len = Math.hypot(x1 - x0, y1 - y0);
        double nx = -(y1 - y0) / len * w / 2;
        double ny = (x1 - x0) / len * w / 2;
        BufferBuilder buffer = Draw.begin(Draw.QUADS);
        Draw.vertex(buffer, x0 + nx, y0 + ny, 0, c[0], c[1], c[2], a);
        Draw.vertex(buffer, x1 + nx, y1 + ny, 0, c[0], c[1], c[2], a);
        Draw.vertex(buffer, x1 - nx, y1 - ny, 0, c[0], c[1], c[2], a);
        Draw.vertex(buffer, x0 - nx, y0 - ny, 0, c[0], c[1], c[2], a);
        Draw.end(buffer);
    }

    private static void drawArrow(Minecraft mc, Spot s) {
        LocalPlayer player = mc.player;
        double dx = BowMath.wrapDegrees(anchorYaw + s.yaw - player.getYRot());
        double dy = anchorPitch + s.pitch - player.getXRot();
        ScreenProjection.edgeArrow(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), dx, dy,
                TARGET_RED, TARGET_WHITE);
    }

    private static void drawRoundInfo(GuiGraphics g, Minecraft mc, long into) {
        Font font = mc.font;
        float cx = mc.getWindow().getGuiScaledWidth() / 2F;
        String hitText = I18n.get("weakspot.combo.hit", hits);
        drawScaled(g, font, hitText, cx, 18, 2, 0xFFFFFF);
        if (bestAtStart > 0) {
            boolean passed = passedTick >= startTick;
            double glow = passed
                    ? Math.max(0, 1 - (ClientWeakSpotHandler.clientTick - passedTick) / (double) PASS_GLOW_TICKS) : 0;
            String best = I18n.get("weakspot.target.best", bestAtStart);
            float bx = cx + font.width(hitText) + 8 + font.width(best) / 2F;
            drawScaled(g, font, best, bx, 19, (float) (1 + 0.6 * glow), passed ? GOLD : GRAY);
        }
        double left = Math.max(0, (TargetRules.ROUND_TICKS - into) / 20.0);
        String time = I18n.get("weakspot.target.timeLeft", String.format("%.1f", left));
        g.drawString(font, time, cx - font.width(time) / 2F, 32, 0xFFFFFFFF, true);
    }

    private static void drawPopups(GuiGraphics g, Minecraft mc, double now) {
        Font font = mc.font;
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
            g.drawString(font, text, (float) popup.x - font.width(text) / 2F, (float) (popup.y - 8 - t),
                    alpha << 24 | PENALTY_RGB, true);
        }
    }

    /** 最後の 1 秒で薄くなる。 */
    private static void drawResult(GuiGraphics g, Minecraft mc, double t) {
        Font font = mc.font;
        float fade = (float) Math.max(0.03, Math.min(1, (RESULT_TICKS - t) / 20.0));
        int alpha = (int) (255 * fade) << 24;
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {I18n.get("weakspot.target.result.this"), I18n.get("weakspot.combo.hit", resultHits)});
        String best = I18n.get("weakspot.combo.hit", resultBest);
        if (resultBest > resultHits) {
            best += I18n.get("weakspot.target.result.behind", resultBest - resultHits);
        }
        rows.add(new String[] {I18n.get("weakspot.target.result.best"), best});
        boolean youTop = resultTopScore > 0 && resultBest >= resultTopScore;
        String top = resultTopScore <= 0 ? "—"
                : I18n.get("weakspot.combo.hit", resultTopScore) + I18n.get("weakspot.target.result.topName",
                        youTop ? I18n.get("weakspot.target.result.you") : resultTopName);
        rows.add(new String[] {I18n.get("weakspot.target.result.top"), top});
        String counts = I18n.get("weakspot.target.result.counts", resultGood, resultDecoys,
                resultDecoys * TargetRules.MISS_PENALTY);
        TargetRules.Tier next = TargetRecords.tier().next();
        String reward = next == null ? I18n.get("weakspot.target.result.allDone")
                : I18n.get("weakspot.target.result.next",
                        I18n.get("weakspot.target.tier." + tierKey(next)), next.from - resultBest);

        int height = 14 + 30 + (resultNewBest ? 14 : 0) + LINE * rows.size() + 6 + LINE * 2 + 8;
        float cx = mc.getWindow().getGuiScaledWidth() / 2F;
        float top0 = mc.getWindow().getGuiScaledHeight() / 2F - height / 2F - 10;
        float left = cx - BOARD_WIDTH / 2F;
        beginShapes(g);
        ScreenProjection.rect(left, top0, left + BOARD_WIDTH, top0 + height, new float[] {0, 0, 0, 0.6F * fade});
        endShapes();
        float y = top0 + 6;
        String title = I18n.get("weakspot.target.result.title");
        g.drawString(font, title, cx - font.width(title) / 2F, y, alpha | GRAY, true);
        y += 14;
        drawScaled(g, font, ChatFormatting.BOLD + I18n.get("weakspot.combo.hit", resultHits), cx, y + 12, 3,
                alpha | 0xFFFFFF);
        y += 30;
        if (resultNewBest) {
            String text = ChatFormatting.BOLD + I18n.get("weakspot.target.newBest");
            g.drawString(font, text, cx - font.width(text) / 2F, y, alpha | GOLD, true);
            y += 14;
        }
        float labelX = left + 14;
        float valueRight = left + BOARD_WIDTH - 14;
        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            g.drawString(font, row[0], labelX, y, alpha | GRAY, true);
            int color = i == 2 && youTop ? GOLD : 0xFFFFFF;
            g.drawString(font, row[1], valueRight - font.width(row[1]), y, alpha | color, true);
            y += LINE;
        }
        y += 6;
        g.drawString(font, counts, cx - font.width(counts) / 2F, y, alpha | 0xFFFFFF, true);
        y += LINE;
        g.drawString(font, reward, cx - font.width(reward) / 2F, y, alpha | (next == null ? GOLD : 0xFFFFFF), true);
    }

    private static float[] rgb(int rgb) {
        return new float[] {(rgb >> 16 & 0xFF) / 255F, (rgb >> 8 & 0xFF) / 255F, (rgb & 0xFF) / 255F};
    }

    /** argb の濃さが 0 なら不透明として描く。 */
    private static void drawScaled(GuiGraphics g, Font font, String text, float cx, float cy, float scale, int argb) {
        int color = (argb >>> 24) == 0 ? 0xFF000000 | argb : argb;
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, text, -font.width(text) / 2F, -font.lineHeight / 2F, color, true);
        g.pose().popPose();
    }
}
