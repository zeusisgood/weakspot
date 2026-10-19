package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.Milestones;
import io.github.zeusisgood.weakspot.network.MilestoneMessage;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.component.FireworkExplosion;

/** 達成した本人の画面だけ。 */
final class MilestoneEffects {

    /** コンボの 1000 と同じ。 */
    private static final int GOLD_RGB = 0xFFD700;
    private static final ChatFormatting[] RAINBOW = {
            ChatFormatting.RED, ChatFormatting.GOLD, ChatFormatting.YELLOW, ChatFormatting.GREEN,
            ChatFormatting.AQUA, ChatFormatting.BLUE, ChatFormatting.LIGHT_PURPLE};
    private static final int[] RAINBOW_RGB = {0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x55FFFF, 0x5555FF, 0xFF55FF};

    /** コンボの 1000 のタイトルは、これと重ならないようにする。 */
    static long lastShownTick = Long.MIN_VALUE / 2;

    private MilestoneEffects() {
    }

    /** type は MilestoneMessage の MINING / KIND / TOTAL。 */
    static void show(int type, int kindId, long milestone) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        boolean lucky = Milestones.isLucky(milestone);
        boolean grand = type == MilestoneMessage.TOTAL;
        lastShownTick = ClientWeakSpotHandler.clientTick;

        String title;
        String chatKey;
        Object[] chatArgs;
        HitKind kind = type == MilestoneMessage.KIND ? HitKind.byId(kindId) : null;
        if (kind != null) {
            String kindName = I18n.get("weakspot.kind." + kind.key());
            title = I18n.get("weakspot.milestone.kindTitle", kindName, milestone);
            chatKey = "weakspot.milestone.kindChat";
            chatArgs = new Object[] {Component.translatable("weakspot.kind." + kind.key()), milestone};
        } else if (grand) {
            title = I18n.get("weakspot.milestone.totalTitle", milestone);
            chatKey = "weakspot.milestone.totalChat";
            chatArgs = new Object[] {milestone};
        } else {
            title = I18n.get("weakspot.milestone.title", milestone);
            chatKey = "weakspot.milestone.chat";
            chatArgs = new Object[] {milestone};
        }
        mc.gui.setTimes(5, lucky || grand ? 80 : 50, 20);
        mc.gui.setSubtitle(Component.translatable(grand ? "weakspot.milestone.totalSubtitle"
                : "weakspot.milestone.subtitle"));
        Component shown = lucky ? rainbow(title)
                : grand ? Component.literal(title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                : Component.literal(title).withStyle(ChatFormatting.GOLD);
        mc.gui.setTitle(shown);

        player.displayClientMessage(Component.translatable(chatKey, chatArgs)
                .withStyle(lucky ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GOLD), false);

        if (grand && !lucky) {
            comboFireworks(3, GOLD_RGB);
        } else {
            int bursts = lucky ? 5 : 1;
            for (int i = 0; i < bursts; i++) {
                double dx = lucky ? (i - 2) * 1.2 : 0;
                mc.level.createFireworks(player.getX() + dx, player.getY() + 2.5, player.getZ(), 0, 0, 0,
                        List.of(fireworks(lucky, i)));
            }
        }

        HitSounds.accentScale(lucky || grand ? 1 : 2, 0);
        if (lucky || grand) {
            HitSounds.accentScale(1, HitSounds.scale().perOctave() + 3);
        }
    }

    /** 粒子と音はクライアントだけで出し、エンティティは出さない。 */
    static void comboFireworks(int count, int rgb) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        for (int i = 0; i < count; i++) {
            double dx = (i - (count - 1) / 2.0) * 1.5;
            FireworkExplosion explosion = new FireworkExplosion(
                    count > 1 ? FireworkExplosion.Shape.LARGE_BALL : FireworkExplosion.Shape.SMALL_BALL,
                    IntList.of(rgb, 0xFFFFFF), IntList.of(), count > 1, count > 1);
            mc.level.createFireworks(player.getX() + dx, player.getY() + 2.5, player.getZ(), 0, 0, 0,
                    List.of(explosion));
        }
    }

    private static Component rainbow(String text) {
        MutableComponent result = Component.empty();
        int n = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            result.append(Component.literal(new String(Character.toChars(cp)))
                    .withStyle(RAINBOW[n++ % RAINBOW.length], ChatFormatting.BOLD));
            i += Character.charCount(cp);
        }
        return result;
    }

    private static FireworkExplosion fireworks(boolean lucky, int index) {
        IntList colors = lucky
                ? IntList.of(RAINBOW_RGB[index % RAINBOW_RGB.length], RAINBOW_RGB[(index + 3) % RAINBOW_RGB.length])
                : IntList.of(0xFFAA00, 0xFFFF55);
        return new FireworkExplosion(lucky ? FireworkExplosion.Shape.LARGE_BALL : FireworkExplosion.Shape.SMALL_BALL,
                colors, IntList.of(), lucky, lucky);
    }
}
