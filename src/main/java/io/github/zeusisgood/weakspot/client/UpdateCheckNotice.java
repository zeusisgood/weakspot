package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.UpdateCheck;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

/** Forge の更新確認（@Mod の updateJSON）の結果を見て、起動ごとに 1 回出す。 */
final class UpdateCheckNotice {

    /** 更新のお知らせ UpdateNotes の 40 tick より後。 */
    private static final int DELAY_TICKS = 60;
    /** 確認がまだ終わっていないときに、待つ tick。 */
    private static final int PENDING_TICKS = 200;

    static boolean done;

    private UpdateCheckNotice() {
    }

    /** waitTicks はワールドに入ってからの tick（UpdateNotes が数える）。 */
    static void tick(Minecraft mc, int waitTicks) {
        if (done || waitTicks < DELAY_TICKS) {
            return;
        }
        if (!WeakSpotConfig.client.updates.checkForUpdates) {
            done = true;
            return;
        }
        ModContainer mod = Loader.instance().getIndexedModList().get(WeakSpotMod.MODID);
        if (mod == null) {
            done = true;
            return;
        }
        ForgeVersion.CheckResult result = ForgeVersion.getResult(mod);
        if (result.status == ForgeVersion.Status.PENDING) {
            // Forge が取りに行っている途中（versionCheck=false のときも、ずっとこのまま）
            if (waitTicks >= DELAY_TICKS + PENDING_TICKS) {
                done = true;
            }
            return;
        }
        done = true;
        boolean newer = result.status == ForgeVersion.Status.OUTDATED
                || result.status == ForgeVersion.Status.BETA_OUTDATED;
        String target = result.target == null ? null : result.target.toString();
        if (UpdateCheck.shouldNotify(newer, target, WeakSpotConfig.client.updates.skippedUpdateVersion)) {
            show(mc, target);
        }
    }

    private static void show(Minecraft mc, String target) {
        String current = WeakSpotMod.VERSION;
        TextComponentTranslation first = new TextComponentTranslation("weakspot.updateCheck.available", target, current);
        first.getStyle().setColor(TextFormatting.GOLD);
        first.appendText(" ");
        first.appendSibling(UpdateNotes.link(new TextComponentTranslation("weakspot.updateCheck.link"),
                new ClickEvent(ClickEvent.Action.OPEN_URL, UpdateNotes.DOWNLOAD_URL)));
        first.appendText(" ");
        ITextComponent skip = new TextComponentTranslation("weakspot.updateCheck.skip");
        skip.getStyle().setColor(TextFormatting.GRAY).setUnderlined(true)
                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/weakspot skipupdate " + target));
        first.appendSibling(skip);
        mc.player.sendMessage(first);

        if (UpdateCheck.differentMinor(current, target)) {
            TextComponentTranslation minor = new TextComponentTranslation("weakspot.updateCheck.minor");
            minor.getStyle().setColor(TextFormatting.GRAY);
            mc.player.sendMessage(minor);
        }
    }

    static void skip(Minecraft mc, String version) {
        String target = version.trim();
        if (target.isEmpty()) {
            return;
        }
        WeakSpotConfig.client.updates.skippedUpdateVersion = target;
        WeakSpotConfig.save();
        TextComponentTranslation reply = new TextComponentTranslation("weakspot.updateCheck.skipped", target);
        reply.getStyle().setColor(TextFormatting.GRAY);
        mc.player.sendMessage(reply);
    }
}
