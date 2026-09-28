package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** ほかのプレイヤーの節目の知らせ（サーバーの MiningRewards.broadcast。1.10.0）を、showOthersMilestones がオフなら出さない。 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
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

    private static boolean isBroadcast(ITextComponent message) {
        return message instanceof TextComponentTranslation
                && ((TextComponentTranslation) message).getKey().startsWith(KEY_PREFIX);
    }
}
