package io.github.zeusisgood.weakspot.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Dist.CLIENT)
public final class StatsKeyHandler {

    private static final KeyMapping OPEN_STATS =
            new KeyMapping("key.weakspot.stats", InputConstants.KEY_K, "key.categories.weakspot");

    private StatsKeyHandler() {
    }

    static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_STATS);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (OPEN_STATS.consumeClick() && mc.screen == null && mc.player != null) {
            StatsScreen.open(mc);
        }
    }
}
