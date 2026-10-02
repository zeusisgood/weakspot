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

/**
 * 進捗（1.11.0。進捗の画面の「WeakSpot」のタブ）。定義は assets/weakspot/advancements/*.json で、条件はどれも
 * minecraft:impossible にして、ここ（サーバー）で判定して与える。1.10.x までの記録は、ログインのときに判定する。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class WeakSpotAdvancements {

    private static final String CRITERION = "done";

    private WeakSpotAdvancements() {
    }

    /** 受け付けたヒットのたび（ServerStats.countStreak から。combo はヒット後のコンボ）。 */
    static void onHit(EntityPlayerMP player, int combo) {
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

    /** 死ぬ見込みだった落下を、落下の弱点に当てて生き延びた（MoveHits。1.11.0）。 */
    static void grantCloseCall(EntityPlayerMP player) {
        grant(player, "close_call");
    }

    /** 的当ての自己ベストが変わったかもしれない（TargetRounds の終わり）。 */
    static void onTargetBest(EntityPlayerMP player, long best) {
        TargetRules.Tier tier = TargetRules.Tier.of(best);
        for (TargetRules.Tier t : TargetRules.Tier.values()) {
            if (t != TargetRules.Tier.NONE && tier.atLeast(t)) {
                grant(player, "target_" + t.name().toLowerCase(java.util.Locale.ROOT));
            }
        }
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
