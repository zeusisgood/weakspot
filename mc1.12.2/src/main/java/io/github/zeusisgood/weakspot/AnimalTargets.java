package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.AnimalTimers;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.server.AnimalHits;
import io.github.zeusisgood.weakspot.server.ServerSwitches;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** クライアントとサーバーで同じ条件を使う。動いているタイマーがあるか（弱点が出る条件）は、サーバーだけが知っている（AnimalHits）。 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class AnimalTargets {

    private AnimalTargets() {
    }

    /** EntityAnimal（Mod の動物を含む）と村人。 */
    public static boolean isCandidate(Entity entity) {
        return entity instanceof EntityAnimal || entity instanceof EntityVillager;
    }

    /** 登録がなければ空文字。 */
    public static String id(Entity entity) {
        ResourceLocation key = EntityList.getKey(entity);
        return key == null ? "" : key.toString();
    }

    public static boolean requiresSneak(Entity entity, SyncedSettings settings) {
        return AnimalTimers.requiresSneak(
                entity instanceof AbstractHorse,
                entity instanceof EntityTameable && ((EntityTameable) entity).isTamed(),
                entity instanceof EntityPig && ((EntityPig) entity).getSaddled(),
                entity instanceof EntityVillager,
                settings.animalSneakRequiredEntities.contains(id(entity)));
    }

    /** 動いているタイマーがあるかは別。餌を持った右クリックは「繁殖・餌やり」なので出さない。 */
    public static boolean isTarget(EntityPlayer player, Entity entity, SyncedSettings settings) {
        if (!settings.enabled(HitKind.ANIMAL) || !PlayerRules.canUse(player) || !isCandidate(entity)
                || settings.animalExcludedEntities.contains(id(entity))) {
            return false;
        }
        if (!HeldItems.isFreeFor(player.getHeldItemMainhand(), entity, settings)
                || !HeldItems.isFreeFor(player.getHeldItemOffhand(), entity, settings)) {
            return false;
        }
        return !requiresSneak(entity, settings) || player.isSneaking();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        cancelIfWeakSpotApplies(event, event.getTarget());
    }

    /** Mod の動物は、素手の右クリックの動作を applyPlayerInteraction で持つことがあるので、こちらも止める。 */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        cancelIfWeakSpotApplies(event, event.getTarget());
    }

    /**
    * サーバーは動いているタイマーを自分で調べる。クライアントは、サーバーの返事（AnimalStates）を覚えているときだけ止める。
    * 覚えていなくても、クライアントが送ったパケットをサーバーが止めるので、バニラの動作は起きない。
    */
    private static void cancelIfWeakSpotApplies(PlayerInteractEvent event, Entity target) {
        EntityPlayer player = event.getEntityPlayer();
        World world = event.getWorld();
        if (world.isRemote ? !WeakSpotMod.proxy.isKindEnabled(HitKind.ANIMAL) : !ServerSwitches.isEnabled(player, HitKind.ANIMAL)) {
            return;
        }
        SyncedSettings settings = RightClickTargets.settings(world);
        if (!isTarget(player, target, settings)) {
            return;
        }
        boolean active = world.isRemote
                ? WeakSpotMod.proxy.animalWeakSpotActive(target.getEntityId())
                : AnimalHits.hasActiveTimer(target, settings);
        if (active) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
        }
    }
}
