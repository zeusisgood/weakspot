package com.example.weakspot.server;

import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.VillagerBreeding;
import com.example.weakspot.common.VillagerBreeding.Mate;
import com.example.weakspot.common.VillagerBreeding.Reason;
import com.example.weakspot.config.SyncedSettings;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.village.Village;

/**
 * 村人が繁殖できない理由を、アクションバーに出す（1.8.4）。大人の村人への動物の状態の問い合わせ（しゃがんで素手で
 * 右クリックを押しっぱなし）のたびに調べる。古いクライアントは翻訳キーを持たないので、文章は ServerLang で作る。
 */
final class VillagerBreedHints {

    /** 同じ文章を送り直す間隔（アクションバーは 3 秒ほどで消える）。 */
    private static final long RESEND_TICKS = 20;

    private static final Map<UUID, String> LAST_TEXT = new HashMap<>();
    private static final Map<UUID, Long> LAST_TICK = new HashMap<>();

    private VillagerBreedHints() {
    }

    static void onQuery(EntityPlayerMP player, EntityVillager villager, SyncedSettings settings) {
        if (villager.isChild() || !ServerSwitches.isEnabled(player, HitKind.ANIMAL)) {
            return;
        }
        List<Reason> reasons = reasons(villager);
        String text = text(player, villager, reasons, settings);
        UUID id = player.getUniqueID();
        long now = player.world.getTotalWorldTime();
        Long last = LAST_TICK.get(id);
        if (text.equals(LAST_TEXT.get(id)) && last != null && now - last < RESEND_TICKS) {
            return;
        }
        LAST_TEXT.put(id, text);
        LAST_TICK.put(id, now);
        TextComponentString message = new TextComponentString(text);
        message.getStyle().setColor(reasons.isEmpty() ? TextFormatting.GREEN : TextFormatting.GOLD);
        player.sendStatusMessage(message, true);
    }

    static void onLogout(EntityPlayerMP player) {
        LAST_TEXT.remove(player.getUniqueID());
        LAST_TICK.remove(player.getUniqueID());
    }

    private static List<Reason> reasons(EntityVillager villager) {
        Village village = villager.world.getVillageCollection().getNearestVillage(new BlockPos(villager), 0);
        EntityVillager mate = villager.world.findNearestEntityWithinAABB(EntityVillager.class,
                villager.getEntityBoundingBox().grow(VillagerBreeding.MATE_RANGE, VillagerBreeding.MATE_RANGE_Y,
                        VillagerBreeding.MATE_RANGE), villager);
        Mate mateState = mate == null ? Mate.NONE
                : VillagerBreeding.mate(true, mate.isChild(), mate.getGrowingAge() > 0, isReady(mate));
        return VillagerBreeding.reasons(village != null, village != null && village.isMatingSeason(),
                village == null ? 0 : village.getNumVillagers(), village == null ? 0 : village.getNumVillageDoors(),
                villager.getGrowingAge() > 0, isReady(villager), mateState);
    }

    /** その気か、その気になれる食べ物を持っているか。getIsWillingToMate(true) は食べ物を減らすので呼ばない。 */
    private static boolean isReady(EntityVillager villager) {
        if (villager.getIsWillingToMate(false)) {
            return true;
        }
        InventoryBasic inventory = villager.getVillagerInventory();
        return VillagerBreeding.hasFood(maxStack(inventory, Items.BREAD), maxStack(inventory, Items.POTATO),
                maxStack(inventory, Items.CARROT));
    }

    private static int maxStack(InventoryBasic inventory, Item item) {
        int max = 0;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                max = Math.max(max, stack.getCount());
            }
        }
        return max;
    }

    private static String text(EntityPlayerMP player, EntityVillager villager, List<Reason> reasons,
            SyncedSettings settings) {
        if (reasons.isEmpty()) {
            return ServerLang.format(player, "weakspot.breed.ready");
        }
        StringBuilder joined = new StringBuilder();
        for (Reason reason : reasons) {
            if (joined.length() > 0) {
                joined.append(ServerLang.format(player, "weakspot.breed.separator"));
            }
            joined.append(reasonText(player, villager, reason, settings));
        }
        return ServerLang.format(player, "weakspot.breed.line", joined);
    }

    private static String reasonText(EntityPlayerMP player, EntityVillager villager, Reason reason,
            SyncedSettings settings) {
        String key = "weakspot.breed." + reason.name().toLowerCase(Locale.ROOT);
        switch (reason) {
            case DOORS: {
                Village village = villager.world.getVillageCollection().getNearestVillage(new BlockPos(villager), 0);
                int villagers = village.getNumVillagers();
                int doors = village.getNumVillageDoors();
                return ServerLang.format(player, key, villagers, doors,
                        VillagerBreeding.doorsNeeded(villagers, doors));
            }
            case COOLDOWN: {
                boolean boost = settings.animalBreedingEnabled && settings.animalBreedingHits > 0;
                return ServerLang.format(player, boost ? key + ".boost" : key,
                        VillagerBreeding.clock(villager.getGrowingAge()));
            }
            default:
                return ServerLang.format(player, key);
        }
    }
}
