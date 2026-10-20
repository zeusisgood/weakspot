package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Dist.CLIENT)
public final class OthersMilestoneFilter {

    static final String KEY_PREFIX = "weakspot.milestone.broadcast.";

    private OthersMilestoneFilter() {
    }

    @SubscribeEvent
    public static void onChat(ClientChatReceivedEvent event) {
        if (!WeakSpotConfig.client.combo.showOthersMilestones && isBroadcast(event.getMessage())) {
            event.setCanceled(true);
        }
    }

    private static boolean isBroadcast(Component message) {
        return message.getContents() instanceof TranslatableContents tc && tc.getKey().startsWith(KEY_PREFIX);
    }
}
