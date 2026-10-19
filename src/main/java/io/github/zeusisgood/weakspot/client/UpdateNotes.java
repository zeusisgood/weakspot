package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 版が変わってから初めてワールドに入ったときに 1 回だけ出す（クライアントだけで判定する）。
 * 初めて入れた人（lastSeenVersion が空で、起動した時点で weakspot.cfg がなかった）には出さない。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
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
        if (event.phase != TickEvent.Phase.END || checked && UpdateCheckNotice.done) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) {
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
        String summary = I18n.hasKey(key) ? I18n.format(key) : I18n.format("weakspot.news.default");
        TextComponentTranslation first = new TextComponentTranslation("weakspot.news.header", version, summary);
        first.getStyle().setColor(TextFormatting.GOLD);
        first.appendText(" ");
        first.appendSibling(link(new TextComponentTranslation("weakspot.news.link"),
                new ClickEvent(ClickEvent.Action.OPEN_URL, DOWNLOAD_URL)));
        mc.player.sendMessage(first);

        TextComponentTranslation second = new TextComponentTranslation("weakspot.news.bugHint",
                link(new TextComponentString("/weakspot bug"),
                        new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/weakspot bug")));
        second.getStyle().setColor(TextFormatting.GRAY);
        mc.player.sendMessage(second);
    }

    static ITextComponent link(ITextComponent text, ClickEvent click) {
        text.getStyle().setColor(TextFormatting.AQUA).setUnderlined(true).setClickEvent(click);
        return text;
    }
}
