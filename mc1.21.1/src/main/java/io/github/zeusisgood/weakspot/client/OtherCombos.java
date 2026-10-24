package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import io.github.zeusisgood.weakspot.common.ItemRules;
import java.util.HashMap;
import java.util.Map;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/** 機械はしゃがんで叩くので、名前と違って、しゃがんでいる人の分も出す。壁の向こうは見えない（深度テストあり）。 */
@EventBusSubscriber(modid = WeakSpotMod.MODID, value = Dist.CLIENT)
final class OtherCombos {

    static final int MIN_SHOWN = 10;
    /** 途切れたらサーバーが 0 を送る。これは届かなかったときの保険なので、コンボの猶予の効果中（約 6 秒）より長くする。 */
    private static final int STALE_TICKS = ItemRules.GRACE_RESET_TICKS + 20;
    /** 名前の表示（体の高さ + 0.5）より、さらに上に出す高さ。 */
    private static final double ABOVE_NAME = 0.3;
    private static final float TEXT_SCALE = 0.025F;

    private static final Map<Integer, int[]> COMBOS = new HashMap<>();
    /** {ヒット数, 届いた tick}。ラウンドは 33 秒なので、それより長く届かなければ消す。 */
    private static final Map<Integer, int[]> TARGETS = new HashMap<>();
    private static final int TARGET_STALE_TICKS = 700;
    private static final double TARGET_ABOVE = 0.28;

    private OtherCombos() {
    }

    /** クライアントのスレッドで呼ぶ。 */
    static void receive(int entityId, int count) {
        if (count <= 0) {
            COMBOS.remove(entityId);
        } else {
            COMBOS.put(entityId, new int[] {count, (int) ClientWeakSpotHandler.clientTick});
        }
    }

    /** 負なら終わった。 */
    static void receiveTarget(int entityId, int hits) {
        if (hits < 0) {
            TARGETS.remove(entityId);
        } else {
            TARGETS.put(entityId, new int[] {hits, (int) ClientWeakSpotHandler.clientTick});
        }
    }

    static void clear() {
        COMBOS.clear();
        TARGETS.clear();
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = event.getEntity();
        if (!WeakSpotConfig.client.combo.othersComboDisplay || mc.options.hideGui || player == mc.player
                || player.isInvisible()) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        double y = player.getBbHeight() + 0.5 + ABOVE_NAME;
        int[] entry = COMBOS.get(player.getId());
        if (entry != null && ClientWeakSpotHandler.clientTick - entry[1] > STALE_TICKS) {
            COMBOS.remove(player.getId());
            entry = null;
        }
        if (entry != null && entry[0] >= MIN_SHOWN) {
            draw(mc, pose, buffers, I18n.get("weakspot.combo.hit", entry[0]), ComboHud.colorOf(entry[0]), y);
            y += TARGET_ABOVE;
        }
        int[] target = TARGETS.get(player.getId());
        if (target != null && ClientWeakSpotHandler.clientTick - target[1] > TARGET_STALE_TICKS) {
            TARGETS.remove(player.getId());
            target = null;
        }
        if (target != null) {
            draw(mc, pose, buffers, I18n.get("weakspot.target.others", target[0]), TargetPlay.OTHERS_RGB, y);
        }
    }

    /** pose はそのプレイヤーの足元が原点。y はそこからの高さ（ブロック）。明るさは周りによらず一定。 */
    private static void draw(Minecraft mc, PoseStack pose, MultiBufferSource buffers, String text, int rgb, double y) {
        Font font = mc.font;
        String shown = ChatFormatting.BOLD + text;
        pose.pushPose();
        pose.translate(0, y, 0);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
        font.drawInBatch(shown, -font.width(shown) / 2F, 0, 0xFF000000 | rgb, true, pose.last().pose(), buffers,
                Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        pose.popPose();
    }
}
