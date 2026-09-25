package com.example.weakspot.client;

import com.example.weakspot.common.HitPitch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

/** 節目の演出（達成した本人の画面だけ）: 画面中央の大きな文字、チャット1行、花火、音階の駆け上がり。777 は派手にする。 */
final class MilestoneEffects {

    private static final int LUCKY = 777;
    private static final TextFormatting[] RAINBOW = {
            TextFormatting.RED, TextFormatting.GOLD, TextFormatting.YELLOW, TextFormatting.GREEN,
            TextFormatting.AQUA, TextFormatting.BLUE, TextFormatting.LIGHT_PURPLE};
    private static final int[] RAINBOW_RGB = {0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x55FFFF, 0x5555FF, 0xFF55FF};

    /** 最後に節目のタイトルを出した clientTick（コンボの 1000 のタイトルは、これと重ならないようにする）。 */
    static long lastShownTick = Long.MIN_VALUE / 2;

    private MilestoneEffects() {
    }

    static void show(int milestone) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null) {
            return;
        }
        boolean lucky = milestone == LUCKY;
        lastShownTick = ClientWeakSpotHandler.clientTick;

        String title = I18n.format("weakspot.milestone.title", milestone);
        mc.ingameGUI.displayTitle(null, null, 5, lucky ? 80 : 50, 20);
        mc.ingameGUI.displayTitle(null, I18n.format("weakspot.milestone.subtitle"), -1, -1, -1);
        mc.ingameGUI.displayTitle(lucky ? rainbow(title) : TextFormatting.GOLD + title, null, -1, -1, -1);

        TextComponentTranslation chat = new TextComponentTranslation("weakspot.milestone.chat", milestone);
        chat.getStyle().setColor(lucky ? TextFormatting.LIGHT_PURPLE : TextFormatting.GOLD);
        player.sendMessage(chat);

        int bursts = lucky ? 5 : 1;
        for (int i = 0; i < bursts; i++) {
            double dx = lucky ? (i - 2) * 1.2 : 0;
            mc.world.makeFireworks(player.posX + dx, player.posY + 2.5, player.posZ, 0, 0, 0,
                    fireworks(lucky, i));
        }

        HitSounds.playScale(HitSounds::playOwn, lucky ? 1 : 2, 0);
        if (lucky) {
            HitSounds.playScale(HitSounds::playOwn, 1, HitPitch.SCALE_LENGTH + 2);
        }
    }

    /**
     * コンボの段階の花火（自分の画面だけ。粒子と音はクライアントだけで出し、エンティティは出さない）。
     * count 発を、プレイヤーの頭の上に横に並べる。
     */
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

    /** 1文字ずつ色を変える。 */
    private static String rainbow(String text) {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (char c : text.toCharArray()) {
            sb.append(RAINBOW[n++ % RAINBOW.length]).append(TextFormatting.BOLD).append(c);
        }
        return sb.toString();
    }

    /** バニラの花火の爆発（Fireworks タグ）。 */
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
