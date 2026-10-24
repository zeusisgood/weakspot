package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.MiningStats;
import io.github.zeusisgood.weakspot.common.TargetRules;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** 進捗の条件はどれも minecraft:impossible にして、サーバーで判定して与える。1.10.x までの記録は、ログインのときに判定する。 */
@EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class WeakSpotAdvancements {

    private static final String CRITERION = "done";

    private WeakSpotAdvancements() {
    }

    /** combo はヒット後の数。 */
    static void onHit(ServerPlayer player, int combo) {
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

    /** 欠片の作り方もレシピ本に出す。 */
    static void onShard(ServerPlayer player) {
        if (!isDone(player, "shard")) {
            GuideBookGiver.unlockShardRecipes(player);
        }
        grant(player, "shard");
    }

    static void grantCloseCall(ServerPlayer player) {
        grant(player, "close_call");
    }

    static void onTargetBest(ServerPlayer player, long best) {
        TargetRules.Tier tier = TargetRules.Tier.of(best);
        for (TargetRules.Tier t : TargetRules.Tier.values()) {
            if (t != TargetRules.Tier.NONE && tier.atLeast(t)) {
                grant(player, "target_" + t.name().toLowerCase(java.util.Locale.ROOT));
            }
        }
    }

    /** しきい値を上げる前に解放した段階を残すため。進捗が見つからなければ数えない。 */
    static TargetRules.Tier earnedTargetTier(ServerPlayer player) {
        TargetRules.Tier[] all = TargetRules.Tier.values();
        for (int i = all.length - 1; i > 0; i--) {
            AdvancementHolder advancement = find(player, "target_" + all[i].name().toLowerCase(java.util.Locale.ROOT));
            if (advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone()) {
                return all[i];
            }
        }
        return TargetRules.Tier.NONE;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
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
        if (total.shards > 0) {
            grant(player, "shard");
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

    private static AdvancementHolder find(ServerPlayer player, String name) {
        return player.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath(WeakSpotMod.MODID, name));
    }

    private static boolean isDone(ServerPlayer player, String name) {
        AdvancementHolder advancement = find(player, name);
        return advancement == null || player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private static void grant(ServerPlayer player, String name) {
        AdvancementHolder advancement = find(player, name);
        if (advancement == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        if (!progress.isDone()) {
            player.getAdvancements().award(advancement, CRITERION);
        }
    }
}
