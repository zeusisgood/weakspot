package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.Milestones;
import io.github.zeusisgood.weakspot.network.MilestoneMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

/** 達成した本人の画面だけ。 */
final class MilestoneEffects {

    /** コンボの 1000 と同じ。 */
    private static final int GOLD_RGB = 0xFFD700;
    private static final String GOLD_BOLD = TextFormatting.GOLD + "" + TextFormatting.BOLD;
    private static final TextFormatting[] RAINBOW = {
            TextFormatting.RED, TextFormatting.GOLD, TextFormatting.YELLOW, TextFormatting.GREEN,
            TextFormatting.AQUA, TextFormatting.BLUE, TextFormatting.LIGHT_PURPLE};
    private static final int[] RAINBOW_RGB = {0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x55FFFF, 0x5555FF, 0xFF55FF};

    /** コンボの 1000 のタイトルは、これと重ならないようにする。 */
    static long lastShownTick = Long.MIN_VALUE / 2;

    private MilestoneEffects() {
    }

    /** type は MilestoneMessage の MINING / KIND / TOTAL。 */
    static void show(int type, int kindId, long milestone) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null) {
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
            String kindName = I18n.format("weakspot.kind." + kind.key());
            title = I18n.format("weakspot.milestone.kindTitle", kindName, milestone);
            chatKey = "weakspot.milestone.kindChat";
            chatArgs = new Object[] {new TextComponentTranslation("weakspot.kind." + kind.key()), milestone};
        } else if (grand) {
            title = I18n.format("weakspot.milestone.totalTitle", milestone);
            chatKey = "weakspot.milestone.totalChat";
            chatArgs = new Object[] {milestone};
        } else {
            title = I18n.format("weakspot.milestone.title", milestone);
            chatKey = "weakspot.milestone.chat";
            chatArgs = new Object[] {milestone};
        }
        mc.ingameGUI.displayTitle(null, null, 5, lucky || grand ? 80 : 50, 20);
        mc.ingameGUI.displayTitle(null, I18n.format(grand ? "weakspot.milestone.totalSubtitle"
                : "weakspot.milestone.subtitle"), -1, -1, -1);
        String shown = lucky ? rainbow(title) : (grand ? GOLD_BOLD : TextFormatting.GOLD) + title;
        mc.ingameGUI.displayTitle(shown, null, -1, -1, -1);

        TextComponentTranslation chat = new TextComponentTranslation(chatKey, chatArgs);
        chat.getStyle().setColor(lucky ? TextFormatting.LIGHT_PURPLE : TextFormatting.GOLD);
        player.sendMessage(chat);

        if (grand && !lucky) {
            comboFireworks(3, GOLD_RGB);
        } else {
            int bursts = lucky ? 5 : 1;
            for (int i = 0; i < bursts; i++) {
                double dx = lucky ? (i - 2) * 1.2 : 0;
                mc.world.makeFireworks(player.posX + dx, player.posY + 2.5, player.posZ, 0, 0, 0,
                        fireworks(lucky, i));
            }
        }

        HitSounds.accentScale(lucky || grand ? 1 : 2, 0);
        if (lucky || grand) {
            HitSounds.accentScale(1, HitSounds.scale().perOctave() + 3);
        }
    }

    /** 粒子と音はクライアントだけで出し、エンティティは出さない。 */
    static void comboFireworks(int count, int rgb) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null) {
            return;
        }
        for (int i = 0; i < count; i++) {
            double dx = (i - (count - 1) / 2.0) * 1.5;
            NBTTagCompound explosion = new NBTTagCompound();
            explosion.setByte("Type", (byte) (count > 1 ? 1 : 0));
            explosion.setBoolean("Flicker", count > 1);
            explosion.setBoolean("Trail", count > 1);
            explosion.setIntArray("Colors", new int[] {rgb, 0xFFFFFF});
            mc.world.makeFireworks(player.posX + dx, player.posY + 2.5, player.posZ, 0, 0, 0, wrap(explosion));
        }
    }

    private static String rainbow(String text) {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (char c : text.toCharArray()) {
            sb.append(RAINBOW[n++ % RAINBOW.length]).append(TextFormatting.BOLD).append(c);
        }
        return sb.toString();
    }

    private static NBTTagCompound fireworks(boolean lucky, int index) {
        NBTTagCompound explosion = new NBTTagCompound();
        explosion.setByte("Type", (byte) (lucky ? 1 : 0));
        explosion.setBoolean("Flicker", lucky);
        explosion.setBoolean("Trail", lucky);
        int[] colors = lucky
                ? new int[] {RAINBOW_RGB[index % RAINBOW_RGB.length], RAINBOW_RGB[(index + 3) % RAINBOW_RGB.length]}
                : new int[] {0xFFAA00, 0xFFFF55};
        explosion.setIntArray("Colors", colors);
        return wrap(explosion);
    }

    private static NBTTagCompound wrap(NBTTagCompound explosion) {
        NBTTagList explosions = new NBTTagList();
        explosions.appendTag(explosion);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("Explosions", explosions);
        return tag;
    }
}
