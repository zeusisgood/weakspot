package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.AnimalTargets;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.AnimalTimers.Timer;
import io.github.zeusisgood.weakspot.common.AnimalTimers;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import io.github.zeusisgood.weakspot.network.StateMessage;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.npc.Villager;

/**
 * 動物のタイマーはサーバーだけが持つので、弱点が出る条件もここで決める（1.20.1 にアルマジロはない）。
 */
public final class AnimalHits {

    /** バニラが生き物への右クリックを受け付ける距離の余裕（ServerGamePacketListenerImpl#handleInteract と同じ）。 */
    private static final double REACH_MARGIN = 3.0;

    /** メモリだけ（動物がいなくなれば消える）。 */
    private static final Map<Entity, AnimalTimers.HitCounter> WOOL_HITS = new WeakHashMap<>();
    private static final Map<Entity, AnimalTimers.HitCounter> TRADE_HITS = new WeakHashMap<>();

    private AnimalHits() {
    }

    /** mask は Timer#bit の合計。 */
    public static final class State {
        public final int mask;
        public final float[] progress = new float[Timer.values().length];

        State(int mask) {
            this.mask = mask;
        }
    }

    /** 加速できるタイマーが動いている。 */
    public static boolean hasActiveTimer(Entity entity, SyncedSettings settings) {
        return state(entity, settings).mask != 0;
    }

    /** 設定で無効なものは含めない。 */
    public static State state(Entity entity, SyncedSettings settings) {
        int mask = 0;
        float[] progress = new float[Timer.values().length];
        if (entity instanceof AgeableMob ageable) {
            int age = ageable.getAge();
            if (age < 0 && settings.animalBabyEnabled && settings.animalBabyHits > 0) {
                mask |= Timer.BABY.bit();
                progress[Timer.BABY.ordinal()] = (float) AnimalTimers.babyProgress(age);
            } else if (age > 0 && settings.animalBreedingEnabled && settings.animalBreedingHits > 0) {
                mask |= Timer.BREEDING.bit();
                progress[Timer.BREEDING.ordinal()] = (float) AnimalTimers.breedingProgress(age);
            }
        }
        if (entity instanceof Sheep sheep) {
            if (sheep.isSheared() && !sheep.isBaby() && settings.sheepWoolEnabled && settings.sheepWoolHits > 0) {
                mask |= Timer.WOOL.bit();
                progress[Timer.WOOL.ordinal()] = (float) AnimalTimers.hitProgress(
                        counter(WOOL_HITS, entity).count(), settings.sheepWoolHits);
            }
        }
        boolean eggs = settings.chickenEggEnabled && settings.chickenEggHits > 0;
        if (entity instanceof Chicken chicken && !chicken.isBaby() && !chicken.isChickenJockey && eggs) {
            mask |= Timer.EGG.bit();
            progress[Timer.EGG.ordinal()] = (float) AnimalTimers.eggProgress(chicken.eggTime);
        }
        if (entity instanceof Villager villager) {
            if (!villager.isBaby() && settings.villagerTradeResetEnabled && settings.villagerTradeResetHits > 0
                    && VillagerTrades.hasLockedTrade(villager)) {
                mask |= Timer.TRADE.bit();
                progress[Timer.TRADE.ordinal()] = (float) AnimalTimers.hitProgress(
                        counter(TRADE_HITS, entity).count(), settings.villagerTradeResetHits);
            }
        }
        State state = new State(mask);
        System.arraycopy(progress, 0, state.progress, 0, progress.length);
        return state;
    }

    private static AnimalTimers.HitCounter counter(Map<Entity, AnimalTimers.HitCounter> map, Entity entity) {
        return map.computeIfAbsent(entity, e -> new AnimalTimers.HitCounter());
    }

    /** 動物が見つからなければ、タイマーなしを返す。 */
    public static void onQuery(ServerPlayer player, int entityId) {
        Entity entity = player.level().getEntity(entityId);
        State state = new State(0);
        if (entity != null && AnimalTargets.isCandidate(entity) && player.canReachRaw(entity, REACH_MARGIN)) {
            SyncedSettings settings = SyncedSettings.server();
            state = state(entity, settings);
            if (entity instanceof Villager villager) {
                VillagerBreedHints.onQuery(player, villager, settings);
            }
        }
        WeakSpotMod.network.sendTo(StateMessage.animal(entityId, state.mask, state.progress), player);
    }

    public static void onHit(ServerPlayer player, int entityId, int streak) {
        if (!HitGate.allowed(player, HitKind.ANIMAL)) {
            return;
        }
        Entity entity = player.level().getEntity(entityId);
        if (entity == null || !player.canReachRaw(entity, REACH_MARGIN)) {
            return;
        }
        SyncedSettings settings = SyncedSettings.server();
        if (!AnimalTargets.isTarget(player, entity, settings) || !HitGate.ready(player, HitKind.ANIMAL)) {
            return;
        }
        State state = state(entity, settings);
        if (state.mask == 0) {
            return;
        }
        apply(entity, state.mask, settings);
        HitGate.accept(player, HitKind.ANIMAL, entity.blockPosition(), streak);
    }

    /** すべて同時に進める。 */
    private static void apply(Entity entity, int mask, SyncedSettings settings) {
        if ((mask & Timer.BABY.bit()) != 0) {
            AgeableMob animal = (AgeableMob) entity;
            // 0 になると、バニラの setAge が大人になったときの処理（ageBoundaryReached）も呼ぶ
            animal.setAge(AnimalTimers.babyAfterHit(animal.getAge(), AnimalTimers.ticksPerHit(AnimalTimers.BABY_TICKS,
                    settings.animalBabyHits)));
        }
        if ((mask & Timer.BREEDING.bit()) != 0) {
            AgeableMob animal = (AgeableMob) entity;
            animal.setAge(AnimalTimers.breedingAfterHit(animal.getAge(),
                    AnimalTimers.ticksPerHit(AnimalTimers.BREEDING_TICKS, settings.animalBreedingHits)));
        }
        int eggTicks = AnimalTimers.ticksPerHit(AnimalTimers.EGG_TICKS, settings.chickenEggHits);
        if ((mask & Timer.EGG.bit()) != 0) {
            Chicken chicken = (Chicken) entity;
            // 0 以下のままにすると、バニラが次の tick に（音も同じ処理で）卵を産む
            chicken.eggTime = Math.max(1, AnimalTimers.eggAfterHit(chicken.eggTime, eggTicks));
        }
        if ((mask & Timer.WOOL.bit()) != 0
                && counter(WOOL_HITS, entity).hit(settings.sheepWoolHits)) {
            ((Sheep) entity).setSheared(false);
        }
        if ((mask & Timer.TRADE.bit()) != 0
                && counter(TRADE_HITS, entity).hit(settings.villagerTradeResetHits)) {
            VillagerTrades.reset((Villager) entity, WeakSpotConfig.server.animal.villagerResetUnlocksNewTier);
        }
    }
}
