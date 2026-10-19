package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.MiningStats;
import io.github.zeusisgood.weakspot.common.TargetRules;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/** 進捗の条件はどれも minecraft:impossible にして、サーバーで判定して与える。1.10.x までの記録は、ログインのときに判定する。 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class WeakSpotAdvancements {

    private static final String CRITERION = "done";

    private WeakSpotAdvancements() {
    }

    /** combo はヒット後の数。 */
    static void onHit(EntityPlayerMP player, int combo) {
        if (!isDone(player, "root")) {
            // 弱点の的のレシピもレシピ本に出す
            GuideBookGiver.unlockTargetRecipe(player);
        }
        grant(player, "root");
        if (combo >= 100) {
            grant(player, "combo_100");
        }
        if (combo >= 300) {
            grant(player, "combo_300");
        }
        if (!isDone(player, "all_kinds") && hitEveryKind(ServerStats.total(player))) {
            grant(player, "all_kinds");
        }
    }

    static void grantCloseCall(EntityPlayerMP player) {
        grant(player, "close_call");
    }

    static void onTargetBest(EntityPlayerMP player, long best) {
        TargetRules.Tier tier = TargetRules.Tier.of(best);
        for (TargetRules.Tier t : TargetRules.Tier.values()) {
            if (t != TargetRules.Tier.NONE && tier.atLeast(t)) {
                grant(player, "target_" + t.name().toLowerCase(java.util.Locale.ROOT));
            }
        }
    }

    /** しきい値を上げる前に解放した段階を残すため。進捗が見つからなければ数えない。 */
    static TargetRules.Tier earnedTargetTier(EntityPlayerMP player) {
        TargetRules.Tier[] all = TargetRules.Tier.values();
        for (int i = all.length - 1; i > 0; i--) {
            Advancement advancement = find(player, "target_" + all[i].name().toLowerCase(java.util.Locale.ROOT));
            if (advancement != null && player.getAdvancements().getProgress(advancement).isDone()) {
                return all[i];
            }
        }
        return TargetRules.Tier.NONE;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        MiningStats total = ServerStats.total(player);
        if (total.totalHits() > 0) {
            grant(player, "root");
        }
        if (total.maxStreak >= 100) {
            grant(player, "combo_100");
        }
        if (total.maxStreak >= 300) {
            grant(player, "combo_300");
        }
        if (hitEveryKind(total)) {
            grant(player, "all_kinds");
        }
        onTargetBest(player, total.targetBest);
    }

    private static boolean hitEveryKind(MiningStats total) {
        for (HitKind kind : HitKind.values()) {
            if (total.count(kind) <= 0) {
                return false;
            }
        }
        return true;
    }

    private static Advancement find(EntityPlayerMP player, String name) {
        return player.getServerWorld().getAdvancementManager()
                .getAdvancement(new ResourceLocation(WeakSpotMod.MODID, name));
    }

    private static boolean isDone(EntityPlayerMP player, String name) {
        Advancement advancement = find(player, name);
        return advancement == null || player.getAdvancements().getProgress(advancement).isDone();
    }

    private static void grant(EntityPlayerMP player, String name) {
        Advancement advancement = find(player, name);
        if (advancement == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getProgress(advancement);
        if (!progress.isDone()) {
            player.getAdvancements().grantCriterion(advancement, CRITERION);
        }
    }
}
