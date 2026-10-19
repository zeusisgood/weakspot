package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.AnimalTargets;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.AnimalTimers;
import io.github.zeusisgood.weakspot.common.AnimalTimers.Timer;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import io.github.zeusisgood.weakspot.network.StateMessage;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;

/** 動物のタイマーはサーバーだけが持つので、弱点が出る条件もここで決める。 */
public final class AnimalHits {

    /** NetHandlerPlayServer#processUseEntity。見えていれば 6 ブロック。 */
    private static final double REACH_SQ = 36.0;

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
        if (entity instanceof EntityAgeable) {
            int age = ((EntityAgeable) entity).getGrowingAge();
            if (age < 0 && settings.animalBabyEnabled && settings.animalBabyHits > 0) {
                mask |= Timer.BABY.bit();
                progress[Timer.BABY.ordinal()] = (float) AnimalTimers.babyProgress(age);
            } else if (age > 0 && settings.animalBreedingEnabled && settings.animalBreedingHits > 0) {
                mask |= Timer.BREEDING.bit();
                progress[Timer.BREEDING.ordinal()] = (float) AnimalTimers.breedingProgress(age);
            }
        }
        if (entity instanceof EntitySheep) {
            EntitySheep sheep = (EntitySheep) entity;
            if (sheep.getSheared() && !sheep.isChild() && settings.sheepWoolEnabled && settings.sheepWoolHits > 0) {
                mask |= Timer.WOOL.bit();
                progress[Timer.WOOL.ordinal()] = (float) AnimalTimers.hitProgress(
                        counter(WOOL_HITS, entity).count(), settings.sheepWoolHits);
            }
        }
        if (entity instanceof EntityChicken) {
            EntityChicken chicken = (EntityChicken) entity;
            if (!chicken.isChild() && !chicken.isChickenJockey() && settings.chickenEggEnabled
                    && settings.chickenEggHits > 0) {
                mask |= Timer.EGG.bit();
                progress[Timer.EGG.ordinal()] = (float) AnimalTimers.eggProgress(chicken.timeUntilNextEgg);
            }
        }
        if (entity instanceof EntityVillager) {
            EntityVillager villager = (EntityVillager) entity;
            if (!villager.isChild() && settings.villagerTradeResetEnabled && settings.villagerTradeResetHits > 0
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

    /** サーバースレッドで呼ぶ。動物が見つからなければ、タイマーなしを返す。 */
    public static void onQuery(EntityPlayerMP player, int entityId) {
        Entity entity = player.world.getEntityByID(entityId);
        State state = new State(0);
        if (entity != null && AnimalTargets.isCandidate(entity) && player.getDistanceSq(entity) < REACH_SQ) {
            SyncedSettings settings = SyncedSettings.server();
            state = state(entity, settings);
            if (entity instanceof EntityVillager) {
                VillagerBreedHints.onQuery(player, (EntityVillager) entity, settings);
            }
        }
        WeakSpotMod.network.sendTo(StateMessage.animal(entityId, state.mask, state.progress), player);
    }

    /** サーバースレッドで呼ぶ。 */
    public static void onHit(EntityPlayerMP player, int entityId, int streak) {
        if (!HitGate.allowed(player, HitKind.ANIMAL)) {
            return;
        }
        Entity entity = player.world.getEntityByID(entityId);
        if (entity == null || player.getDistanceSq(entity) >= REACH_SQ) {
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
        HitGate.accept(player, HitKind.ANIMAL, new BlockPos(entity), streak);
    }

    /** すべて同時に進める。 */
    private static void apply(Entity entity, int mask, SyncedSettings settings) {
        if ((mask & Timer.BABY.bit()) != 0) {
            EntityAgeable animal = (EntityAgeable) entity;
            int age = animal.getGrowingAge();
            int next = AnimalTimers.babyAfterHit(age, AnimalTimers.ticksPerHit(AnimalTimers.BABY_TICKS,
                    settings.animalBabyHits));
            if (next == 0) {
                // バニラの ageUp で、大人になったときの処理（onGrowingAdult）も呼ぶ
                animal.ageUp(-age / 20 + 1, false);
            } else {
                animal.setGrowingAge(next);
            }
        }
        if ((mask & Timer.BREEDING.bit()) != 0) {
            EntityAgeable animal = (EntityAgeable) entity;
            animal.setGrowingAge(AnimalTimers.breedingAfterHit(animal.getGrowingAge(),
                    AnimalTimers.ticksPerHit(AnimalTimers.BREEDING_TICKS, settings.animalBreedingHits)));
        }
        if ((mask & Timer.EGG.bit()) != 0) {
            EntityChicken chicken = (EntityChicken) entity;
            int remaining = AnimalTimers.eggAfterHit(chicken.timeUntilNextEgg,
                    AnimalTimers.ticksPerHit(AnimalTimers.EGG_TICKS, settings.chickenEggHits));
            // 0 以下のままにすると、バニラが次の tick に（音も同じ処理で）卵を産む
            chicken.timeUntilNextEgg = remaining <= 0 ? 1 : remaining;
        }
        if ((mask & Timer.WOOL.bit()) != 0
                && counter(WOOL_HITS, entity).hit(settings.sheepWoolHits)) {
            ((EntitySheep) entity).setSheared(false);
        }
        if ((mask & Timer.TRADE.bit()) != 0
                && counter(TRADE_HITS, entity).hit(settings.villagerTradeResetHits)) {
            VillagerTrades.reset((EntityVillager) entity, WeakSpotConfig.server.animal.villagerResetUnlocksNewTier,
                    entity.world.rand);
        }
    }

}
