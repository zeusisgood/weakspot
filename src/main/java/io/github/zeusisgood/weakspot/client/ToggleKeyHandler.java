package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import io.github.zeusisgood.weakspot.network.SwitchMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.SoundEvents;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;

/**
* 弱点の一時オフのキー（初期値 HOME。J は JourneyMap と重なるため）。状態は設定 weakSpotsEnabled に保存する。
* オフの間に止める処理は ClientWeakSpotHandler.stopOwnWeakSpots と、それを呼ぶ側にある。
*/
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
public final class ToggleKeyHandler {

    private static final KeyBinding TOGGLE =
            new KeyBinding("key.weakspot.toggle", Keyboard.KEY_HOME, "key.categories.weakspot");

    /** 今のワールド（サーバー）に入ってから。 */
    private static boolean reminded;

    private ToggleKeyHandler() {
    }

    static String keyName() {
        return TOGGLE.getDisplayName();
    }

    static void register() {
        ClientRegistry.registerKeyBinding(TOGGLE);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!TOGGLE.isPressed() || mc.currentScreen != null || mc.player == null) {
            return;
        }
        WeakSpotConfig.client.markers.weakSpotsEnabled = !WeakSpotConfig.client.markers.weakSpotsEnabled;
        WeakSpotConfig.save();
        boolean on = WeakSpotConfig.client.markers.weakSpotsEnabled;
        if (!on) {
            ClientWeakSpotHandler.stopOwnWeakSpots();
        }
        mc.ingameGUI.setOverlayMessage(I18n.format(on ? "weakspot.toggle.on" : "weakspot.toggle.off"), false);
        mc.getSoundHandler().playSound(
                PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, on ? 1.2F : 0.8F));
    }

    /** null なら、このワールド（サーバー）にはまだ伝えていない。 */
    private static Boolean lastSent;
    private static Boolean lastSentParticles;
    private static Integer lastSentKinds;

    /** 毎 tick 呼ぶ。届く前のサーバーは、オンとして扱う。 */
    static void syncToServer(Minecraft mc) {
        if (mc.world == null || mc.player == null || mc.getConnection() == null) {
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
        if (mc.world == null || mc.player == null) {
            reminded = false;
            return;
        }
        if (reminded) {
            return;
        }
        reminded = true;
        if (!WeakSpotConfig.client.markers.weakSpotsEnabled) {
            mc.ingameGUI.setOverlayMessage(I18n.format("weakspot.toggle.offReminder", TOGGLE.getDisplayName()), false);
        }
    }
}
