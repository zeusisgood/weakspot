package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.UpdateCheck;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.VersionChecker;

/** NeoForge の更新確認（neoforge.mods.toml の updateJSONURL）の結果を見て、起動ごとに 1 回出す。 */
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
        ModContainer mod = ModList.get().getModContainerById(WeakSpotMod.MODID).orElse(null);
        if (mod == null) {
            done = true;
            return;
        }
        VersionChecker.CheckResult result = VersionChecker.getResult(mod.getModInfo());
        if (result.status() == VersionChecker.Status.PENDING) {
            // NeoForge が取りに行っている途中（versionCheck=false のときも、ずっとこのまま）
            if (waitTicks >= DELAY_TICKS + PENDING_TICKS) {
                done = true;
            }
            return;
        }
        done = true;
        boolean newer = result.status() == VersionChecker.Status.OUTDATED
                || result.status() == VersionChecker.Status.BETA_OUTDATED;
        String target = result.target() == null ? null : result.target().toString();
        if (UpdateCheck.shouldNotify(newer, target, WeakSpotConfig.client.updates.skippedUpdateVersion)) {
            show(mc, target);
        }
    }

    private static void show(Minecraft mc, String target) {
        String current = WeakSpotMod.VERSION;
        MutableComponent first = Component.translatable("weakspot.updateCheck.available", target, current)
                .withStyle(ChatFormatting.GOLD);
        first.append(" ");
        first.append(UpdateNotes.link(Component.translatable("weakspot.updateCheck.link"),
                new ClickEvent(ClickEvent.Action.OPEN_URL, UpdateNotes.DOWNLOAD_URL)));
        first.append(" ");
        first.append(Component.translatable("weakspot.updateCheck.skip").withStyle(style -> style
                .withColor(ChatFormatting.GRAY).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/weakspot skipupdate " + target))));
        mc.player.displayClientMessage(first, false);

        if (UpdateCheck.differentMinor(current, target)) {
            mc.player.displayClientMessage(
                    Component.translatable("weakspot.updateCheck.minor").withStyle(ChatFormatting.GRAY), false);
        }
    }

    static void skip(Minecraft mc, String version) {
        String target = version.trim();
        if (target.isEmpty()) {
            return;
        }
        WeakSpotConfig.client.updates.skippedUpdateVersion = target;
        WeakSpotConfig.save();
        mc.player.displayClientMessage(
                Component.translatable("weakspot.updateCheck.skipped", target).withStyle(ChatFormatting.GRAY), false);
    }
}
