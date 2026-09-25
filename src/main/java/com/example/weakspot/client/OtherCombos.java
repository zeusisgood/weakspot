package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.config.WeakSpotConfig;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 近くのほかのプレイヤーのコンボを、頭の上（名前の少し上）に「25 HIT」と出す（1.6.0）。数はサーバーから届く
 * （OtherComboMessage）。MIN_SHOWN 以上のときだけ出し、STALE_TICKS 更新がなければ消す。色はコンボの表示と同じ段階の色。
 * 機械はしゃがんで叩くので、名前と違って、しゃがんでいる人の分も出す（1.6.3）。壁の向こうは見えない（深度テストあり）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
final class OtherCombos {

    static final int MIN_SHOWN = 10;
    private static final int STALE_TICKS = 60;
    /** 名前の表示（体の高さ + 0.5）より、さらに上に出す高さ。 */
    private static final double ABOVE_NAME = 0.3;
    private static final float TEXT_SCALE = 0.025F;

    private static final Map<Integer, int[]> COMBOS = new HashMap<>();

    private OtherCombos() {
    }

    /** 届いた数（クライアントのスレッドで呼ぶ）。{数, 届いた tick} を覚える。 */
    static void receive(int entityId, int count) {
        if (count <= 0) {
            COMBOS.remove(entityId);
        } else {
            COMBOS.put(entityId, new int[] {count, (int) ClientWeakSpotHandler.clientTick});
        }
    }

    static void clear() {
        COMBOS.clear();
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = event.getEntityPlayer();
        if (!WeakSpotConfig.othersComboDisplay || mc.gameSettings.hideGUI || player == mc.player
                || player.isInvisible()) {
            return;
        }
        int[] entry = COMBOS.get(player.getEntityId());
        if (entry == null || entry[0] < MIN_SHOWN) {
            return;
        }
        if (ClientWeakSpotHandler.clientTick - entry[1] > STALE_TICKS) {
            COMBOS.remove(player.getEntityId());
            return;
        }
        draw(mc, I18n.format("weakspot.combo.hit", entry[0]), ComboHud.colorOf(entry[0]), event.getX(),
                event.getY() + player.height + 0.5 + ABOVE_NAME, event.getZ());
    }

    /** カメラからの相対位置 (x, y, z) に、カメラの方を向いた文字を描く（深度テストあり）。 */
    private static void draw(Minecraft mc, String text, int rgb, double x, double y, double z) {
        RenderManager manager = mc.getRenderManager();
        FontRenderer font = mc.fontRenderer;
        String shown = TextFormatting.BOLD + text;
        boolean thirdPersonFront = manager.options != null && manager.options.thirdPersonView == 2;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.glNormal3f(0, 1, 0);
        GlStateManager.rotate(-manager.playerViewY, 0, 1, 0);
        GlStateManager.rotate((thirdPersonFront ? -1 : 1) * manager.playerViewX, 1, 0, 0);
        GlStateManager.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        font.drawStringWithShadow(shown, -font.getStringWidth(shown) / 2F, 0, 0xFF000000 | rgb);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }
}
