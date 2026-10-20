package io.github.zeusisgood.weakspot.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import io.github.zeusisgood.weakspot.network.SwitchMessage;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 弱点の一時オフのキー（初期値 HOME。J は JourneyMap と重なるため）。状態は設定 weakSpotsEnabled に保存する。
 * オフの間に止める処理は ClientWeakSpotHandler.stopOwnWeakSpots と、それを呼ぶ側にある。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Dist.CLIENT)
public final class ToggleKeyHandler {

    private static final KeyMapping TOGGLE =
            new KeyMapping("key.weakspot.toggle", InputConstants.KEY_HOME, "key.categories.weakspot");

    /** 今のワールド（サーバー）に入ってから。 */
    private static boolean reminded;

    private ToggleKeyHandler() {
    }

    static String keyName() {
        return TOGGLE.getTranslatedKeyMessage().getString();
    }

    static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!TOGGLE.consumeClick() || mc.screen != null || mc.player == null) {
            return;
        }
        WeakSpotConfig.client.markers.weakSpotsEnabled = !WeakSpotConfig.client.markers.weakSpotsEnabled;
        WeakSpotConfig.save();
        boolean on = WeakSpotConfig.client.markers.weakSpotsEnabled;
        if (!on) {
            ClientWeakSpotHandler.stopOwnWeakSpots();
        }
        mc.gui.setOverlayMessage(Component.translatable(on ? "weakspot.toggle.on" : "weakspot.toggle.off"), false);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, on ? 1.2F : 0.8F));
    }

    /** null なら、このワールド（サーバー）にはまだ伝えていない。 */
    private static Boolean lastSent;
    private static Boolean lastSentParticles;
    private static Integer lastSentKinds;

    /** 毎 tick 呼ぶ。届く前のサーバーは、オンとして扱う。 */
    static void syncToServer(Minecraft mc) {
        if (mc.level == null || mc.player == null || mc.getConnection() == null) {
            lastSent = null;
            lastSentParticles = null;
            lastSentKinds = null;
            return;
        }
        boolean on = WeakSpotConfig.client.markers.weakSpotsEnabled;
        boolean particles = WeakSpotConfig.client.hud.machineParticlesVisible;
        int kinds = KindSwitches.disabledMask();
        if (lastSent == null || lastSent != on || lastSentParticles == null || lastSentParticles != particles
                || lastSentKinds == null || lastSentKinds != kinds) {
            lastSent = on;
            lastSentParticles = particles;
            lastSentKinds = kinds;
            WeakSpotMod.network.sendToServer(new SwitchMessage(on, particles, kinds));
        }
    }

    /** 毎 tick 呼ぶ。 */
    static void remindIfOff(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            reminded = false;
            return;
        }
        if (reminded) {
            return;
        }
        reminded = true;
        if (!WeakSpotConfig.client.markers.weakSpotsEnabled) {
            mc.gui.setOverlayMessage(Component.translatable("weakspot.toggle.offReminder", keyName()), false);
        }
    }
}
