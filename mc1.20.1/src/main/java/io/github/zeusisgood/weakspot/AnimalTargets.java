package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.common.AnimalTimers;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.server.AnimalHits;
import io.github.zeusisgood.weakspot.server.ServerSwitches;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** クライアントとサーバーで同じ条件を使う。動いているタイマーがあるか（弱点が出る条件）は、サーバーだけが知っている（AnimalHits）。 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class AnimalTargets {

    private AnimalTargets() {
    }

    /** Animal（Mod の動物を含む）と村人。 */
    public static boolean isCandidate(Entity entity) {
        return entity instanceof Animal || entity instanceof Villager;
    }

    public static String id(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }

    public static boolean requiresSneak(Entity entity, SyncedSettings settings) {
        return AnimalTimers.requiresSneak(
                entity instanceof AbstractHorse,
                entity instanceof TamableAnimal tamable && tamable.isTame(),
                entity instanceof Pig pig && pig.isSaddled() || entity instanceof Strider strider && strider.isSaddled(),
                entity instanceof Villager,
                settings.animalSneakRequiredEntities.contains(id(entity)));
    }

    /** 動いているタイマーがあるかは別。餌を持った右クリックは「繁殖・餌やり」なので出さない。 */
    public static boolean isTarget(Player player, Entity entity, SyncedSettings settings) {
        if (!settings.enabled(HitKind.ANIMAL) || !PlayerRules.canUse(player) || !isCandidate(entity)
                || settings.animalExcludedEntities.contains(id(entity))) {
            return false;
        }
        if (!HeldItems.isFreeFor(player.getMainHandItem(), entity, settings)
                || !HeldItems.isFreeFor(player.getOffhandItem(), entity, settings)) {
            return false;
        }
        return !requiresSneak(entity, settings) || player.isShiftKeyDown();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (cancelIfWeakSpotApplies(event.getEntity(), event.getLevel(), event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** Mod の動物は、素手の右クリックの動作を interactAt で持つことがあるので、こちらも止める。 */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (cancelIfWeakSpotApplies(event.getEntity(), event.getLevel(), event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /**
     * サーバーは動いているタイマーを自分で調べる。クライアントは、サーバーの返事（AnimalStates）を覚えているときだけ止める。
     * 覚えていなくても、クライアントが送ったパケットをサーバーが止めるので、バニラの動作は起きない。
     */
    private static boolean cancelIfWeakSpotApplies(Player player, Level world, Entity target) {
        if (world.isClientSide ? !WeakSpotMod.proxy.isKindEnabled(HitKind.ANIMAL)
                : !ServerSwitches.isEnabled(player, HitKind.ANIMAL)) {
            return false;
        }
        SyncedSettings settings = RightClickTargets.settings(world);
        if (!isTarget(player, target, settings)) {
            return false;
        }
        return world.isClientSide
                ? WeakSpotMod.proxy.animalWeakSpotActive(target.getId())
                : AnimalHits.hasActiveTimer(target, settings);
    }
}
