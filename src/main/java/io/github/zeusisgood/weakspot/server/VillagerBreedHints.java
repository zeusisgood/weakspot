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
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.village.Village;

/**
 * 村人が繁殖できない理由を、アクションバーに出す（1.8.4）。大人の村人への動物の状態の問い合わせ（しゃがんで素手で
 * 右クリックを押しっぱなし）のたびに調べる。文は翻訳キーで送り、プレイヤーの言語で表示する（キーは 1.8.4 から。
 * 接続できるクライアントは必ず持っている）。
 */
final class VillagerBreedHints {

    /** 同じ文章を送り直す間隔（アクションバーは 3 秒ほどで消える）。 */
    private static final long RESEND_TICKS = 20;

    /** 前に送った案内（翻訳キーと値を並べた文字列）。同じものを続けて送らないため。 */
    private static final Map<UUID, String> LAST_TEXT = new HashMap<>();
    private static final Map<UUID, Long> LAST_TICK = new HashMap<>();

    private VillagerBreedHints() {
    }

    static void onQuery(EntityPlayerMP player, EntityVillager villager, SyncedSettings settings) {
        if (villager.isChild() || !ServerSwitches.isEnabled(player, HitKind.ANIMAL)) {
            return;
        }
        List<Reason> reasons = reasons(villager);
        List<Line> lines = lines(villager, reasons, settings);
        String text = signature(lines);
        UUID id = player.getUniqueID();
        long now = player.world.getTotalWorldTime();
        Long last = LAST_TICK.get(id);
        if (text.equals(LAST_TEXT.get(id)) && last != null && now - last < RESEND_TICKS) {
            return;
        }
        LAST_TEXT.put(id, text);
        LAST_TICK.put(id, now);
        ITextComponent message = component(lines);
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

    /** 案内の 1 つ分: 翻訳キーと値。 */
    private static final class Line {
        final String key;
        final Object[] args;

        Line(String key, Object... args) {
            this.key = key;
            this.args = args;
        }
    }

    /** 案内の中身。理由がなければ「繁殖できる」の 1 つ、あれば理由を並べる。 */
    private static List<Line> lines(EntityVillager villager, List<Reason> reasons, SyncedSettings settings) {
        List<Line> lines = new ArrayList<>();
        if (reasons.isEmpty()) {
            lines.add(new Line("weakspot.breed.ready"));
        }
        for (Reason reason : reasons) {
            lines.add(reasonLine(villager, reason, settings));
        }
        return lines;
    }

    private static Line reasonLine(EntityVillager villager, Reason reason, SyncedSettings settings) {
        String key = "weakspot.breed." + reason.name().toLowerCase(Locale.ROOT);
        switch (reason) {
            case DOORS: {
                Village village = villager.world.getVillageCollection().getNearestVillage(new BlockPos(villager), 0);
                int villagers = village.getNumVillagers();
                int doors = village.getNumVillageDoors();
                return new Line(key, villagers, doors, VillagerBreeding.doorsNeeded(villagers, doors));
            }
            case COOLDOWN: {
                boolean boost = settings.animalBreedingEnabled && settings.animalBreedingHits > 0;
                return new Line(boost ? key + ".boost" : key, VillagerBreeding.clock(villager.getGrowingAge()));
            }
            default:
                return new Line(key);
        }
    }

    /** 同じ案内かを比べるための文字列（翻訳キーと値を並べる）。 */
    private static String signature(List<Line> lines) {
        StringBuilder sb = new StringBuilder();
        for (Line line : lines) {
            sb.append(line.key).append(Arrays.toString(line.args)).append(';');
        }
        return sb.toString();
    }

    /** 翻訳キーで送る形。「繁殖: 理由 / 理由」の、理由の部分も翻訳キー。 */
    private static ITextComponent component(List<Line> lines) {
        if (lines.size() == 1 && lines.get(0).key.equals("weakspot.breed.ready")) {
            return new TextComponentTranslation("weakspot.breed.ready");
        }
        ITextComponent joined = new TextComponentString("");
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                joined.appendSibling(new TextComponentTranslation("weakspot.breed.separator"));
            }
            joined.appendSibling(new TextComponentTranslation(lines.get(i).key, lines.get(i).args));
        }
        return new TextComponentTranslation("weakspot.breed.line", joined);
    }
}
