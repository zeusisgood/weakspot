package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.VillagerBreeding;
import io.github.zeusisgood.weakspot.common.VillagerBreeding.Mate;
import io.github.zeusisgood.weakspot.common.VillagerBreeding.Reason;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;

/**
 * 1.14 以降のバニラ（VillagerMakeLove）の条件: 空いているベッド・待ち時間・食べ物・相手。
 * 文は翻訳キーで送る（接続できるクライアントは必ずキーを持っている）。
 */
final class VillagerBreedHints {

    /** アクションバーは 3 秒ほどで消える。 */
    private static final long RESEND_TICKS = 20;
    /** VillagerMakeLove#takeVacantBed と同じ。 */
    private static final int BED_RANGE = 48;

    /** 同じものを続けて送らないため。 */
    private static final Map<UUID, String> LAST_TEXT = new HashMap<>();
    private static final Map<UUID, Long> LAST_TICK = new HashMap<>();

    private VillagerBreedHints() {
    }

    static void onQuery(ServerPlayer player, Villager villager, SyncedSettings settings) {
        if (villager.isBaby() || !ServerSwitches.isEnabled(player, HitKind.ANIMAL)) {
            return;
        }
        List<Reason> reasons = reasons(villager);
        List<Line> lines = lines(villager, reasons, settings);
        String text = signature(lines);
        UUID id = player.getUUID();
        long now = player.level().getGameTime();
        Long last = LAST_TICK.get(id);
        if (text.equals(LAST_TEXT.get(id)) && last != null && now - last < RESEND_TICKS) {
            return;
        }
        LAST_TEXT.put(id, text);
        LAST_TICK.put(id, now);
        player.displayClientMessage(component(lines)
                .withStyle(reasons.isEmpty() ? ChatFormatting.GREEN : ChatFormatting.GOLD), true);
    }

    static void onLogout(ServerPlayer player) {
        LAST_TEXT.remove(player.getUUID());
        LAST_TICK.remove(player.getUUID());
    }

    private static List<Reason> reasons(Villager villager) {
        Villager mate = villager.level().getNearestEntity(Villager.class,
                TargetingConditions.forNonCombat(), villager,
                villager.getX(), villager.getY(), villager.getZ(),
                villager.getBoundingBox().inflate(VillagerBreeding.MATE_RANGE, VillagerBreeding.MATE_RANGE_Y,
                        VillagerBreeding.MATE_RANGE));
        Mate mateState = mate == null ? Mate.NONE
                : VillagerBreeding.mate(true, mate.isBaby(), mate.getAge() > 0, isReady(mate));
        return VillagerBreeding.reasonsWithBeds(hasFreeBed(villager), villager.getAge() > 0, isReady(villager),
                mateState);
    }

    private static boolean hasFreeBed(Villager villager) {
        if (!(villager.level() instanceof ServerLevel level)) {
            return false;
        }
        return level.getPoiManager().find(holder -> holder.is(PoiTypes.HOME), pos -> true, villager.blockPosition(),
                BED_RANGE, PoiManager.Occupancy.HAS_SPACE).isPresent();
    }

    /** Villager#canBreed の食べ物の分（待ち時間は別に見る）。 */
    private static boolean isReady(Villager villager) {
        return VillagerBreeding.hasFoodPoints(villager.foodLevel + villager.countFoodPointsInInventory());
    }

    private static final class Line {
        final String key;
        final Object[] args;

        Line(String key, Object... args) {
            this.key = key;
            this.args = args;
        }
    }

    /** 理由がなければ「繁殖できる」の 1 つ。 */
    private static List<Line> lines(Villager villager, List<Reason> reasons, SyncedSettings settings) {
        List<Line> lines = new ArrayList<>();
        if (reasons.isEmpty()) {
            lines.add(new Line("weakspot.breed.ready"));
        }
        for (Reason reason : reasons) {
            lines.add(reasonLine(villager, reason, settings));
        }
        return lines;
    }

    private static Line reasonLine(Villager villager, Reason reason, SyncedSettings settings) {
        String key = "weakspot.breed." + reason.name().toLowerCase(Locale.ROOT);
        if (reason == Reason.COOLDOWN) {
            boolean boost = settings.animalBreedingEnabled && settings.animalBreedingHits > 0;
            return new Line(boost ? key + ".boost" : key, VillagerBreeding.clock(villager.getAge()));
        }
        return new Line(key);
    }

    private static String signature(List<Line> lines) {
        StringBuilder sb = new StringBuilder();
        for (Line line : lines) {
            sb.append(line.key).append(Arrays.toString(line.args)).append(';');
        }
        return sb.toString();
    }

    /** 理由の部分も翻訳キー。 */
    private static MutableComponent component(List<Line> lines) {
        if (lines.size() == 1 && lines.get(0).key.equals("weakspot.breed.ready")) {
            return Component.translatable("weakspot.breed.ready");
        }
        MutableComponent joined = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                joined.append(Component.translatable("weakspot.breed.separator"));
            }
            joined.append(Component.translatable(lines.get(i).key, lines.get(i).args));
        }
        return Component.translatable("weakspot.breed.line", joined);
    }
}
