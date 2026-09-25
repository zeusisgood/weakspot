package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.config.WeakSpotConfig;
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
 * 更新のお知らせ（1.7.1）。版が変わってから初めてワールドに入ったときに 1 回だけ、自分のチャットに要約と変更点のリンク、
 * /weakspot bug の案内を出す。ソロでもマルチでも同じ（クライアントだけで判定する）。最後に見た版は lastSeenVersion に覚える。
 * 初めて入れた人（lastSeenVersion が空で、起動した時点で weakspot.cfg がなかった）には出さない。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
final class UpdateNotes {

    /** ワールドに入ってから出すまでの tick（入ったときのほかのメッセージに埋もれないように）。 */
    private static final int DELAY_TICKS = 40;
    static final String RELEASES_URL = "https://github.com/zeusisgood/weakspot/releases/tag/v";

    /** この起動で、もう確かめたか。 */
    private static boolean checked;
    private static int waitTicks;

    private UpdateNotes() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || checked) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) {
            waitTicks = 0;
            return;
        }
        if (++waitTicks < DELAY_TICKS) {
            return;
        }
        checked = true;
        String version = WeakSpotMod.VERSION;
        String last = WeakSpotConfig.lastSeenVersion == null ? "" : WeakSpotConfig.lastSeenVersion.trim();
        if (version.equals(last)) {
            return;
        }
        boolean fresh = last.isEmpty() && !WeakSpotMod.configExistedAtStart;
        WeakSpotConfig.lastSeenVersion = version;
        WeakSpotConfig.save();
        if (!fresh && WeakSpotConfig.showUpdateNotes) {
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
                new ClickEvent(ClickEvent.Action.OPEN_URL, RELEASES_URL + version)));
        mc.player.sendMessage(first);

        TextComponentTranslation second = new TextComponentTranslation("weakspot.news.bugHint",
                link(new TextComponentString("/weakspot bug"),
                        new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/weakspot bug")));
        second.getStyle().setColor(TextFormatting.GRAY);
        mc.player.sendMessage(second);
    }

    /** 水色 #55FFFF に下線の、クリックできる文字。 */
    static ITextComponent link(ITextComponent text, ClickEvent click) {
        text.getStyle().setColor(TextFormatting.AQUA).setUnderlined(true).setClickEvent(click);
        return text;
    }
}
