package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;

/** 統計画面を開くキー（初期値 K。操作設定で変更できる）。 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
public final class StatsKeyHandler {

    private static final KeyBinding OPEN_STATS =
            new KeyBinding("key.weakspot.stats", Keyboard.KEY_K, "key.categories.weakspot");

    private StatsKeyHandler() {
    }

    static void register() {
        ClientRegistry.registerKeyBinding(OPEN_STATS);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (OPEN_STATS.isPressed() && mc.currentScreen == null && mc.player != null) {
            mc.displayGuiScreen(new StatsScreen());
        }
    }
}
