package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.ComboDisplay;
import com.example.weakspot.common.ComboMilestones;
import com.example.weakspot.common.ComboTier;
import com.example.weakspot.common.HitPitch;
import com.example.weakspot.common.HitStreak;
import com.example.weakspot.config.WeakSpotConfig;
import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;

/**
 * 自分の連続ヒット（コンボ）の画面表示。数は ClientWeakSpotHandler.STREAK（ヒット音の音階と同じ数）。
 * 2 以上で「12 HIT」と出し、ヒットのたびに弾ませる。下のバーは途切れるまでの残り時間。
 * 途切れたら薄くして消す（5 以上なら「MAX 23」を少し残す）。10、25、50、100 で強調音と光を出す。
 * 時間は ClientWeakSpotHandler.clientTick（一時停止中は止まる）で数える。F1 で HUD を隠している間は描かない。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
final class ComboHud {

    /** 照準からの距離（照準の下の攻撃のクールダウン表示を避ける）。 */
    private static final int CROSSHAIR_GAP = 16;
    private static final int BAR_WIDTH = 40;
    /** 残りがこの割合を切ったら、バーを点滅させる。 */
    private static final double LOW_FRACTION = 0.25;
    private static final int LOW_RGB = 0xFF3333;
    /** 虹色が一周する時間（ミリ秒）。 */
    private static final long RAINBOW_PERIOD_MS = 4000;
    /** 段階の演出の強調音を、ヒット音から少し遅らせる（tick）。 */
    private static final int ACCENT_DELAY_TICKS = 2;

    private static final ComboMilestones MILESTONES = new ComboMilestones();

    /** 表示中のコンボ（途切れたら 0）。 */
    private static int combo;
    private static double lastHitTime;
    /** 途切れて消えていく途中のコンボ（0 なら何も残っていない）。 */
    private static int brokenCombo;
    private static double brokenTime;
    private static double stepTime = Double.NEGATIVE_INFINITY;
    /** このフレームのボスバーの下端（TOP_CENTER のときに避ける）。 */
    private static int bossBottom;

    private ComboHud() {
    }

    /** 自分のヒット。time はヒットした瞬間（tick。フレームの途中の値を含む）。 */
    static void onHit(int newCombo, double time) {
        combo = newCombo;
        lastHitTime = time;
        // 新しいコンボが始まったら、残していた数は消す
        brokenCombo = 0;
        if (MILESTONES.reached(newCombo) && WeakSpotConfig.comboDisplayEnabled
                && WeakSpotConfig.comboMilestoneEffects) {
            stepTime = time;
            HitSounds.schedule(ACCENT_DELAY_TICKS, () -> HitSounds.playOwn(HitPitch.SCALE_LENGTH));
        }
    }

    /** 40 tick ヒットがなく途切れた。 */
    static void onBreak(int broken, long tick) {
        combo = 0;
        if (broken >= ComboDisplay.MIN_SHOWN) {
            brokenCombo = broken;
            brokenTime = tick;
        }
    }

    /** ワールドを出た、死亡した、ディメンションを移動した（その場で消す）。 */
    static void clear() {
        combo = 0;
        brokenCombo = 0;
        stepTime = Double.NEGATIVE_INFINITY;
        MILESTONES.reset();
    }

    @SubscribeEvent
    public static void onOverlayPre(RenderGameOverlayEvent.Pre event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            bossBottom = 0;
        }
    }

    @SubscribeEvent
    public static void onBossInfo(RenderGameOverlayEvent.BossInfo event) {
        // 名前はバーの 9 上、バーの高さは 5
        bossBottom = Math.max(bossBottom, event.getY() + 5);
    }

    @SubscribeEvent
    public static void onOverlayPost(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (!WeakSpotConfig.comboDisplayEnabled || mc.gameSettings.hideGUI || mc.player == null) {
            return;
        }
        double now = ClientWeakSpotHandler.clientTick + event.getPartialTicks();
        if (combo >= ComboDisplay.MIN_SHOWN) {
            draw(mc, event.getResolution(), I18n.format("weakspot.combo.hit", combo), colorOf(combo), 1,
                    ComboDisplay.bounceScale(now - lastHitTime), HitStreak.remainingFraction(now - lastHitTime),
                    ComboDisplay.glowAlpha(now - stepTime), now);
        } else if (brokenCombo > 0) {
            double alpha = ComboDisplay.fadeAlpha(brokenCombo, now - brokenTime);
            if (alpha <= 0) {
                brokenCombo = 0;
                return;
            }
            String text = ComboDisplay.showsMax(brokenCombo)
                    ? I18n.format("weakspot.combo.max", brokenCombo)
                    : I18n.format("weakspot.combo.hit", brokenCombo);
            draw(mc, event.getResolution(), text, colorOf(brokenCombo), alpha, 1, -1, 0, now);
        }
    }

    /**
     * @param barFraction 残り時間のバー（0〜1）。負なら描かない
     * @param glow 段階の演出の光の濃さ（0 なら描かない）
     */
    private static void draw(Minecraft mc, ScaledResolution res, String text, int rgb, double alpha, double bounce,
                             double barFraction, double glow, double now) {
        FontRenderer font = mc.fontRenderer;
        float scale = (float) WeakSpotConfig.comboScale;
        String shown = TextFormatting.BOLD + text;
        float textWidth = font.getStringWidth(shown) * scale;
        float textHeight = font.FONT_HEIGHT * scale;
        float barHeight = Math.max(1, Math.round(2 * scale));
        float barGap = 2 * scale;

        // 数字の中心
        float cx;
        float cy;
        switch (WeakSpotConfig.comboPosition) {
            case RIGHT_OF_CROSSHAIR:
                cx = res.getScaledWidth() / 2F + CROSSHAIR_GAP + textWidth / 2;
                cy = res.getScaledHeight() / 2F;
                break;
            case TOP_CENTER:
                cx = res.getScaledWidth() / 2F;
                cy = Math.max(4, bossBottom + 4) + textHeight / 2;
                break;
            case BELOW_CROSSHAIR:
            default:
                cx = res.getScaledWidth() / 2F;
                cy = res.getScaledHeight() / 2F + CROSSHAIR_GAP + textHeight / 2;
                break;
        }

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        if (glow > 0) {
            float pad = (float) (2 + 6 * (1 - glow)) * scale;
            fillRect(cx - textWidth / 2 - pad, cy - textHeight / 2 - pad, cx + textWidth / 2 + pad,
                    cy + textHeight / 2 + pad, rgb, 0.5 * glow * alpha);
        }
        if (barFraction >= 0) {
            float width = BAR_WIDTH * scale;
            float top = cy + textHeight / 2 + barGap;
            float left = cx - width / 2;
            fillRect(left, top, left + width, top + barHeight, 0x000000, 0.4 * alpha);
            boolean low = barFraction < LOW_FRACTION;
            // 途切れそうなときは赤く点滅させる
            double barAlpha = low && (long) (now / 2) % 2 == 0 ? 0.5 : 1;
            fillRect(left, top, left + (float) (width * barFraction), top + barHeight, low ? LOW_RGB : rgb,
                    barAlpha * alpha);
        }

        int a = (int) Math.round(alpha * 255);
        // FontRenderer は濃さがほぼ 0 の色を不透明として扱うので、薄すぎるときは描かない
        if (a >= 8) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(cx, cy, 0);
            float k = (float) (scale * bounce);
            GlStateManager.scale(k, k, 1);
            font.drawStringWithShadow(shown, -font.getStringWidth(shown) / 2F, -font.FONT_HEIGHT / 2F + 1,
                    a << 24 | rgb);
            GlStateManager.popMatrix();
        }
        GlStateManager.color(1, 1, 1, 1);
    }

    private static int colorOf(int value) {
        ComboTier tier = ComboTier.of(value);
        if (tier != ComboTier.RAINBOW) {
            return tier.rgb;
        }
        float hue = (Minecraft.getSystemTime() % RAINBOW_PERIOD_MS) / (float) RAINBOW_PERIOD_MS;
        return Color.HSBtoRGB(hue, 0.55F, 1.0F) & 0xFFFFFF;
    }

    private static void fillRect(float x1, float y1, float x2, float y2, int rgb, double alpha) {
        if (x2 <= x1 || alpha <= 0) {
            return;
        }
        float r = (rgb >> 16 & 0xFF) / 255F;
        float g = (rgb >> 8 & 0xFF) / 255F;
        float b = (rgb & 0xFF) / 255F;
        float a = (float) alpha;
        GlStateManager.disableTexture2D();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x1, y2, 0).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, 0).color(r, g, b, a).endVertex();
        buffer.pos(x2, y1, 0).color(r, g, b, a).endVertex();
        buffer.pos(x1, y1, 0).color(r, g, b, a).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
    }
}
