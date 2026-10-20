package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 版が変わってから初めてワールドに入ったときに 1 回だけ出す（クライアントだけで判定する）。
 * 初めて入れた人（lastSeenVersion が空で、起動した時点で weakspot の設定ファイルがなかった）には出さない。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Dist.CLIENT)
final class UpdateNotes {

    /** 入ったときのほかのメッセージに埋もれないように。 */
    private static final int DELAY_TICKS = 40;
    /** 版ごとの変更点もここで読める。 */
    static final String DOWNLOAD_URL = "https://www.curseforge.com/minecraft/mc-mods/weakspot/files";

    private static boolean checked;
    /** ワールドを出たら 0 に戻す。新しい版の通知 UpdateCheckNotice もこれで数える。 */
    private static int waitTicks;

    private UpdateNotes() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (checked && UpdateCheckNotice.done) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            waitTicks = 0;
            return;
        }
        UpdateCheckNotice.tick(mc, ++waitTicks);
        if (checked || waitTicks < DELAY_TICKS) {
            return;
        }
        checked = true;
        String version = WeakSpotMod.VERSION;
        String last = WeakSpotConfig.client.updates.lastSeenVersion == null ? "" : WeakSpotConfig.client.updates.lastSeenVersion.trim();
        if (version.equals(last)) {
            return;
        }
        boolean fresh = last.isEmpty() && !WeakSpotMod.configExistedAtStart;
        WeakSpotConfig.client.updates.lastSeenVersion = version;
        WeakSpotConfig.save();
        if (!fresh && WeakSpotConfig.client.updates.showUpdateNotes) {
            show(mc, version);
        }
    }

    private static void show(Minecraft mc, String version) {
        String key = "weakspot.news." + version;
        String summary = I18n.exists(key) ? I18n.get(key) : I18n.get("weakspot.news.default");
        MutableComponent first = Component.translatable("weakspot.news.header", version, summary)
                .withStyle(ChatFormatting.GOLD);
        first.append(" ");
        first.append(link(Component.translatable("weakspot.news.link"),
                new ClickEvent(ClickEvent.Action.OPEN_URL, DOWNLOAD_URL)));
        mc.player.displayClientMessage(first, false);

        MutableComponent second = Component.translatable("weakspot.news.bugHint",
                link(Component.literal("/weakspot bug"),
                        new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/weakspot bug")))
                .withStyle(ChatFormatting.GRAY);
        mc.player.displayClientMessage(second, false);
    }

    static MutableComponent link(MutableComponent text, ClickEvent click) {
        return text.withStyle(style -> style.withColor(ChatFormatting.AQUA).withUnderlined(true).withClickEvent(click));
    }
}
