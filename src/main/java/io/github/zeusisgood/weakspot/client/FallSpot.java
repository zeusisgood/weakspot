package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.FallDamage;
import io.github.zeusisgood.weakspot.common.ComboFactor;
import io.github.zeusisgood.weakspot.common.FallMath;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.HitPitch;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
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
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.opengl.GL11;

/**
 * 落下の弱点（自分だけ。1.11.0）。高い所から落ちている間（落ちた距離が fallMinDistance を超えた）、照準のまわり（10〜20 度）に
 * 雲の白の弱点を出し、照準を合わせるだけでヒットにする。当てると、落ちた距離を fallReduceBlocks × コンボの掛け数だけ減らす
 * （ダメージはサーバーが着地のときに、サーバーの落ちた距離から決める。ここで減らすのは、見込みのダメージの表示のため）。
 * 照準の上に、着地したときの見込みのダメージを 3 段階で出す（死亡見込みは ☠ の点滅と画面の縁の赤）。当てて段階が下がったら
 * 「生存圏！」「ノーダメージ」、当てて、ダメージなしで着地したら「着地成功」の演出。見込みの計算は FallDamage（両側で同じ）。
 */
final class FallSpot extends AimSpotKind {

    private static final int STREAK_PARTICLES = 8;
    private static final int LANDING_PARTICLES = 16;
    private static final int HINT_OFFSET = 22;
    /** 見込みの 3 段階の色（死亡見込み・ダメージあり・ダメージなし）と、段階が下がったときの文字の色。 */
    private static final int LETHAL_RGB = 0xFF2020;
    private static final int HURT_RGB = 0xFFAA00;
    private static final int SAFE_RGB = 0x55FF55;
    private static final int SURVIVE_RGB = 0xAAFF55;
    /** 死亡見込みのときの、画面の縁の赤（#B00000）と、縁の幅（画面の短い辺に対する比率）。 */
    private static final float[] EDGE_RGB = {0xB0 / 255F, 0, 0};
    private static final double EDGE_RATIO = 0.12;
    /** 点滅の周期（ミリ秒）。 */
    private static final long FLASH_MS = 500;
    /** 段階が下がったときの文字を出す長さ（tick）。 */
    private static final int MESSAGE_TICKS = 30;

    /** この落下で当てたか、着地の直前の落ちた距離。 */
    private boolean hitThisFall;
    private float lastAirFall;
    private boolean inAir;
    /** 段階が下がったときの文字（翻訳キー）と色、出した tick。 */
    private String messageKey;
    private int messageRgb;
    private long messageTick = Long.MIN_VALUE / 2;

    FallSpot() {
        super(HitKind.FALL, new HudSpot(HitKind.FALL));
    }

    @Override
    void clear() {
        super.clear();
        hitThisFall = false;
        inAir = false;
        messageKey = null;
    }

    /** 落ちているか（出す条件。エリトラ・水・溶岩・はしご・乗り物・クリエイティブの飛行は除く）。 */
    private static boolean falling(EntityPlayerSP player) {
        return !player.onGround && !player.isElytraFlying() && !player.isInWater() && !player.isInLava()
                && !player.isRiding() && !player.isOnLadder() && !player.capabilities.isFlying;
    }

    /**
     * 落ち始めから出す（1.11.0 の試してもらったあと）: 下がっていて、このまま着地したときの落下距離（今までの落下 +
     * 地面までの高さ。FallDamage.landingFall）が fallMinDistance を超えるとき。当ててその見込みが下がれば消える。
     */
    @Override
    boolean wanted(EntityPlayerSP player, SyncedSettings settings) {
        return falling(player) && (player.posY < player.prevPosY || player.fallDistance != 0)
                && FallDamage.landingFall(player) > settings.fallMinDistance;
    }

    @Override
    int placement(EntityPlayerSP player) {
        return HudSpot.FREE;
    }

    @Override
    void onHit(Minecraft mc, EntityPlayerSP player, SyncedSettings settings, int streak) {
        FallMath.Outlook before = FallDamage.landingOutlook(player);
        // サーバーと同じく、落ち始めに当てた分は負の「貯め」として着地まで残す
        player.fallDistance = (float) Math.max(FallDamage.MIN_FALL,
                player.fallDistance - settings.fallReduceBlocks * ComboFactor.factor(streak));
        FallMath.Outlook after = FallDamage.landingOutlook(player);
        hitThisFall = true;
        for (int i = 0; i < STREAK_PARTICLES; i++) {
            mc.world.spawnParticle(EnumParticleTypes.CLOUD, player.posX + player.getRNG().nextGaussian() * 0.4,
                    player.posY + 0.2, player.posZ + player.getRNG().nextGaussian() * 0.4, 0, 0.3, 0);
        }
        mc.world.playSound(player.posX, player.posY, player.posZ, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                SoundCategory.PLAYERS, 0.4F, 1.6F, false);
        // 段階が下がった瞬間の文字と音（死亡見込みから一気にダメージなしなら「ノーダメージ」だけ）
        if (after == FallMath.Outlook.SAFE && before != FallMath.Outlook.SAFE) {
            showMessage("weakspot.fall.noDamage", SAFE_RGB);
            HitSounds.accentRun(new float[] {HitPitch.forStreak(1), HitPitch.forStreak(3), HitPitch.forStreak(5)},
                    0, 2);
        } else if (after == FallMath.Outlook.HURT && before == FallMath.Outlook.LETHAL) {
            showMessage("weakspot.fall.survive", SURVIVE_RGB);
            HitSounds.accentRun(new float[] {HitPitch.forStreak(3), HitPitch.forStreak(8)}, 2, 2);
        }
    }

    private void showMessage(String key, int rgb) {
        messageKey = key;
        messageRgb = rgb;
        messageTick = ClientWeakSpotHandler.clientTick;
    }

    private boolean messageShown() {
        return messageKey != null && ClientWeakSpotHandler.clientTick - messageTick < MESSAGE_TICKS;
    }

    /** 着地を見て、当てていてダメージなしなら「着地成功」。 */
    @Override
    void tickEnd(Minecraft mc) {
        EntityPlayerSP player = mc.player;
        if (!player.onGround) {
            inAir = true;
            lastAirFall = player.fallDistance;
            return;
        }
        if (inAir && hitThisFall && !player.isInWater()
                && FallDamage.outlook(player, lastAirFall) == FallMath.Outlook.SAFE) {
            for (int i = 0; i < LANDING_PARTICLES; i++) {
                double angle = Math.PI * 2 * i / LANDING_PARTICLES;
                mc.world.spawnParticle(EnumParticleTypes.CLOUD, player.posX, player.posY + 0.1, player.posZ,
                        Math.cos(angle) * 0.2, 0.02, Math.sin(angle) * 0.2);
            }
            mc.world.playSound(player.posX, player.posY, player.posZ, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    SoundCategory.PLAYERS, 0.5F, 1.2F, false);
        }
        inAir = false;
        hitThisFall = false;
    }

    @Override
    boolean hasGauge(Minecraft mc, float partialTicks) {
        return WeakSpotConfig.client.hud.fallDamageHintEnabled && (spot.has() || messageShown());
    }

    /** 点滅の濃さ（0.35〜1）。 */
    private static double flash() {
        double t = (Minecraft.getSystemTime() % FLASH_MS) / (double) FLASH_MS;
        return 0.35 + 0.65 * (0.5 + 0.5 * Math.cos(t * 2 * Math.PI));
    }

    /** 死亡見込みの間、画面の縁を赤くする（beginOverlay と endOverlay の間）。 */
    @Override
    void drawGauge(Minecraft mc, float partialTicks) {
        if (!spot.has() || FallDamage.landingOutlook(mc.player) != FallMath.Outlook.LETHAL) {
            return;
        }
        ScaledResolution res = new ScaledResolution(mc);
        double w = res.getScaledWidth();
        double h = res.getScaledHeight();
        double edge = Math.min(w, h) * EDGE_RATIO;
        float a = (float) (0.55 * flash());
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        // 上・下・左・右の帯（外側が濃く、内側へ透明に）
        quad(buffer, 0, 0, w, 0, w, edge, 0, edge, a, a, 0, 0);
        quad(buffer, 0, h - edge, w, h - edge, w, h, 0, h, 0, 0, a, a);
        quad(buffer, 0, 0, edge, 0, edge, h, 0, h, a, 0, 0, a);
        quad(buffer, w - edge, 0, w, 0, w, h, w - edge, h, 0, a, a, 0);
        tessellator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
    }

    private static void quad(BufferBuilder buffer, double x1, double y1, double x2, double y2, double x3, double y3,
                             double x4, double y4, float a1, float a2, float a3, float a4) {
        buffer.pos(x1, y1, 0).color(EDGE_RGB[0], EDGE_RGB[1], EDGE_RGB[2], a1).endVertex();
        buffer.pos(x4, y4, 0).color(EDGE_RGB[0], EDGE_RGB[1], EDGE_RGB[2], a4).endVertex();
        buffer.pos(x3, y3, 0).color(EDGE_RGB[0], EDGE_RGB[1], EDGE_RGB[2], a3).endVertex();
        buffer.pos(x2, y2, 0).color(EDGE_RGB[0], EDGE_RGB[1], EDGE_RGB[2], a2).endVertex();
    }

    /**
     * 照準の上に、見込みのダメージを 3 段階で出す（死亡見込みは濃い赤で点滅する「☠」、ダメージありはオレンジ、
     * なしは緑）。段階が下がった直後は、その上に「生存圏！」「ノーダメージ」。
     */
    @Override
    void drawAfterOverlay(Minecraft mc, float partialTicks) {
        EntityPlayerSP player = mc.player;
        FontRenderer font = mc.fontRenderer;
        ScaledResolution res = new ScaledResolution(mc);
        float cx = res.getScaledWidth() / 2F;
        float y = res.getScaledHeight() / 2F - HINT_OFFSET - font.FONT_HEIGHT / 2F;
        if (spot.has()) {
            double landing = FallDamage.landingFall(player);
            double damage = FallDamage.expected(player, landing);
            FallMath.Outlook outlook = FallDamage.outlook(player, landing);
            String text = (outlook == FallMath.Outlook.LETHAL ? "\u2620 " : "\u2665 ") + FallMath.heartsLabel(damage);
            int rgb = outlook == FallMath.Outlook.LETHAL ? LETHAL_RGB : outlook == FallMath.Outlook.HURT ? HURT_RGB
                    : SAFE_RGB;
            int alpha = outlook == FallMath.Outlook.LETHAL ? (int) (255 * flash()) : 255;
            GlStateManager.enableBlend();
            font.drawStringWithShadow(text, cx - font.getStringWidth(text) / 2F, y, alpha << 24 | rgb);
        }
        if (messageShown()) {
            String text = TextFormatting.BOLD + I18n.format(messageKey);
            double t = ClientWeakSpotHandler.clientTick - messageTick + partialTicks;
            int alpha = (int) Math.max(8, Math.min(255, 255 * (MESSAGE_TICKS - t) / 10.0));
            GlStateManager.enableBlend();
            GlStateManager.pushMatrix();
            GlStateManager.translate(cx, y - 12, 0);
            GlStateManager.scale(1.5F, 1.5F, 1);
            font.drawStringWithShadow(text, -font.getStringWidth(text) / 2F, -font.FONT_HEIGHT / 2F,
                    alpha << 24 | messageRgb);
            GlStateManager.popMatrix();
            GlStateManager.color(1, 1, 1, 1);
        }
    }
}
