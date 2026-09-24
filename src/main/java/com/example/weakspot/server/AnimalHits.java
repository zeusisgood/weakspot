package com.example.weakspot.server;

import com.example.weakspot.AnimalTargets;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.AnimalTimers;
import com.example.weakspot.common.AnimalTimers.Timer;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.network.AnimalStateMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * 動物の弱点（子どもの成長、繁殖の待ち時間、羊毛、卵、村人の取引上限）の、状態の問い合わせへの返事と、
 * ヒット通知の検証と効果（論理サーバー）。動物のタイマーはサーバーだけが持つので、弱点が出る条件もここで決める。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class AnimalHits {

    /** バニラが動物への右クリックを受け付ける距離（NetHandlerPlayServer#processUseEntity。見えていれば 6 ブロック）。 */
    private static final double REACH_SQ = 36.0;
    /** 通知の間隔はネットワークの揺らぎで縮むので、この tick 数だけ甘く見る。 */
    private static final int INTERVAL_JITTER_TICKS = 2;

    /** プレイヤーごとの最後のヒットの tick。 */
    private static final Map<UUID, Long> LAST_HIT = new HashMap<>();
    /** 羊ごと・村人ごとのヒット数（メモリだけ。動物がいなくなれば消える）。 */
    private static final Map<Entity, AnimalTimers.HitCounter> WOOL_HITS = new WeakHashMap<>();
    private static final Map<Entity, AnimalTimers.HitCounter> TRADE_HITS = new WeakHashMap<>();

    private AnimalHits() {
    }

    /** 動いているタイマーの一覧（mask は Timer#bit の合計）と、それぞれの進み具合。 */
    public static final class State {
        public final int mask;
        public final float[] progress = new float[Timer.values().length];

        State(int mask) {
            this.mask = mask;
        }
    }

    /** 弱点が出る条件（加速できるタイマーが動いている）を満たすか。 */
    public static boolean hasActiveTimer(Entity entity, SyncedSettings settings) {
        return state(entity, settings).mask != 0;
    }

    /** この動物で動いているタイマーと、進み具合。設定で無効なものは含めない。 */
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

    /** クライアントからの状態の問い合わせ（サーバースレッドで実行される）。動物が見つからなければ、タイマーなしを返す。 */
    public static void onQuery(EntityPlayerMP player, int entityId) {
        Entity entity = player.world.getEntityByID(entityId);
        State state = new State(0);
        if (entity != null && AnimalTargets.isCandidate(entity) && player.getDistanceSq(entity) < REACH_SQ) {
            state = state(entity, SyncedSettings.fromConfig());
        }
        WeakSpotMod.network.sendTo(new AnimalStateMessage(entityId, state), player);
    }

    /** クライアントからのヒット通知（サーバースレッドで実行される）。 */
    public static void onHit(EntityPlayerMP player, int entityId) {
        if (!ServerSwitches.isEnabled(player)) {
            return;
        }
        Entity entity = player.world.getEntityByID(entityId);
        if (entity == null || player.getDistanceSq(entity) >= REACH_SQ) {
            return;
        }
        SyncedSettings settings = SyncedSettings.fromConfig();
        if (!AnimalTargets.isTarget(player, entity, settings)) {
            return;
        }
        long now = player.world.getTotalWorldTime();
        int minInterval = Math.max(0, WeakSpotConfig.animalMinHitIntervalTicks - INTERVAL_JITTER_TICKS);
        Long last = LAST_HIT.get(player.getUniqueID());
        if (last != null && now - last < minInterval) {
            return;
        }
        State state = state(entity, settings);
        if (state.mask == 0) {
            return;
        }
        LAST_HIT.put(player.getUniqueID(), now);
        apply(entity, state.mask, settings);
        ServerStats.record(player, stats -> stats.recordAnimalHit());
        ServerStats.countStreak(player);
    }

    /** 動いているタイマーを、すべて同時に進める。 */
    private static void apply(Entity entity, int mask, SyncedSettings settings) {
        if ((mask & Timer.BABY.bit()) != 0) {
            EntityAgeable animal = (EntityAgeable) entity;
            int age = animal.getGrowingAge();
            int next = AnimalTimers.babyAfterHit(age, AnimalTimers.ticksPerHit(AnimalTimers.BABY_TICKS,
                    settings.animalBabyHits));
            if (next == 0) {
                // 大人になる。バニラの ageUp で、大人になったときの処理（onGrowingAdult）も呼ぶ
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
            VillagerTrades.reset((EntityVillager) entity, WeakSpotConfig.villagerResetUnlocksNewTier,
                    entity.world.rand);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_HIT.remove(event.player.getUniqueID());
    }
}
