package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.WeakSpotSwitch;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.network.SwitchMessage;
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
 * 弱点の一時オフのキー（初期値 J。操作設定で変更できる）。押すたびに、自分の弱点のオンとオフが切り替わる。
 * 状態は設定 weakSpotsEnabled に保存するので、次に起動したときも引き継ぐ。
 *
 * オフの間に止める処理は ClientWeakSpotHandler.stopOwnWeakSpots と、それを呼ぶ側にある
 * （弱点の表示、ヒット判定と通知、ブースト、マークの送信、耐久バー・成長バー、右クリックの抑止）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
public final class ToggleKeyHandler {

    private static final KeyBinding TOGGLE =
            new KeyBinding("key.weakspot.toggle", Keyboard.KEY_J, "key.categories.weakspot");

    /** 今のワールド（サーバー）に入ってから、オフのことを知らせたか。 */
    private static boolean reminded;

    private ToggleKeyHandler() {
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
        WeakSpotConfig.weakSpotsEnabled = WeakSpotSwitch.toggled(WeakSpotConfig.weakSpotsEnabled);
        WeakSpotConfig.save();
        boolean on = WeakSpotConfig.weakSpotsEnabled;
        if (!on) {
            ClientWeakSpotHandler.stopOwnWeakSpots();
        }
        mc.ingameGUI.setOverlayMessage(I18n.format(on ? "weakspot.toggle.on" : "weakspot.toggle.off"), false);
        mc.getSoundHandler().playSound(
                PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, on ? 1.2F : 0.8F));
    }

    /** 最後にサーバーへ伝えた状態。null なら、このワールド（サーバー）にはまだ伝えていない。 */
    private static Boolean lastSent;

    /**
     * 弱点のオン・オフを、サーバーに伝える（ワールドに入ったとき、切り替えたとき。設定画面で変えたときも）。
     * クライアントの tick から毎 tick 呼ぶ。届く前のサーバーは、オンとして扱う。
     */
    static void syncToServer(Minecraft mc) {
        if (mc.world == null || mc.player == null || mc.getConnection() == null) {
            lastSent = null;
            return;
        }
        boolean on = WeakSpotConfig.weakSpotsEnabled;
        if (lastSent == null || lastSent != on) {
            lastSent = on;
            WeakSpotMod.network.sendToServer(new SwitchMessage(on));
        }
    }

    /** ワールドに入った直後に、オフのままなら、アクションバーで知らせる（クライアントの tick から毎 tick 呼ぶ）。 */
    static void remindIfOff(Minecraft mc) {
        if (mc.world == null || mc.player == null) {
            reminded = false;
            return;
        }
        if (reminded) {
            return;
        }
        reminded = true;
        if (!WeakSpotConfig.weakSpotsEnabled) {
            mc.ingameGUI.setOverlayMessage(I18n.format("weakspot.toggle.offReminder", TOGGLE.getDisplayName()), false);
        }
    }
}
