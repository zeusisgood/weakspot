package io.github.zeusisgood.weakspot.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = WeakSpotMod.MODID, value = Dist.CLIENT)
public final class StatsKeyHandler {

    private static final KeyMapping OPEN_STATS =
            new KeyMapping("key.weakspot.stats", InputConstants.KEY_K, "key.categories.weakspot");

    private StatsKeyHandler() {
    }

    static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_STATS);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (OPEN_STATS.consumeClick() && mc.screen == null && mc.player != null) {
            StatsScreen.open(mc);
        }
    }
}
