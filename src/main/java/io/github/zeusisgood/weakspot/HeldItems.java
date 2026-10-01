package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.config.SyncedSettings;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/**
 * 素手と同じに扱う手（1.11.0）。成長・収穫・機械・動物の弱点は「手が空いている」ときに出すが、右クリックで何もしないアイテム
 * （ツルハシ・斧・剣・棒・インゴットなど）を持っていても、空いているとみなす（設定 heldItemsCountAsEmptyHand）。
 * 何もしないかは、アイテムのクラス（Item より下の親を含む）が右クリックの処理（onItemUse・onItemRightClick・
 * itemInteractionForEntity・Forge の onItemUseFirst）を上書きしていないかで、自動で見分ける（Mod のアイテムにも効く）。
 * ブロックとして置く物（ItemBlock）と、除外の一覧（heldItemExcludes）に書いた物は外す。動物では、さらにその動物が反応する物
 * （繁殖の餌・オオカミの骨・馬の小麦と砂糖）を外す。クライアントとサーバーで同じ判定を使う。
 * 1.12.2 のバニラで確かめた（doc/spec/SPEC_v1.11.0.md の 7.2）。移植のときは、版ごとに調べ直す。
 */
public final class HeldItems {

    /** 右クリックの処理のメソッドの名前（開発環境の MCP 名と、実際の環境の SRG 名。onItemUseFirst は Forge が足したもの）。 */
    private static final Set<String> RIGHT_CLICK_METHODS = new HashSet<>(Arrays.asList(
            "onItemUse", "func_180614_a", "onItemRightClick", "func_77659_a",
            "itemInteractionForEntity", "func_111207_a", "onItemUseFirst"));
    /** アイテムのクラス → 右クリックで何もしないか。 */
    private static final Map<Class<?>, Boolean> DOES_NOTHING = new ConcurrentHashMap<>();

    private HeldItems() {
    }

    /** 手が空いている（空か、素手と同じに扱うアイテム）か。ブロック（成長・収穫・機械）の弱点の判定。 */
    public static boolean isFree(ItemStack stack, SyncedSettings settings) {
        if (stack.isEmpty()) {
            return true;
        }
        if (!settings.heldItemsCountAsEmptyHand) {
            return false;
        }
        Item item = stack.getItem();
        if (item instanceof ItemBlock || settings.heldItemExcludes.contains(String.valueOf(item.getRegistryName()))) {
            return false;
        }
        return DOES_NOTHING.computeIfAbsent(item.getClass(), HeldItems::doesNothing);
    }

    /** 動物の弱点の判定。isFree に加えて、その動物が反応するアイテム（餌など）を外す。 */
    public static boolean isFreeFor(ItemStack stack, Entity entity, SyncedSettings settings) {
        if (stack.isEmpty()) {
            return true;
        }
        if (!isFree(stack, settings)) {
            return false;
        }
        if (entity instanceof EntityAnimal && ((EntityAnimal) entity).isBreedingItem(stack)) {
            return false;
        }
        Item item = stack.getItem();
        if (entity instanceof EntityWolf && item == Items.BONE) {
            return false;
        }
        return !(entity instanceof AbstractHorse && (item == Items.WHEAT || item == Items.SUGAR));
    }

    /** そのクラス（Item より下の親を含む）が、右クリックの処理を上書きしていなければ true。 */
    static boolean doesNothing(Class<?> type) {
        try {
            for (Class<?> c = type; c != null && c != Item.class; c = c.getSuperclass()) {
                for (Method method : c.getDeclaredMethods()) {
                    if (RIGHT_CLICK_METHODS.contains(method.getName())) {
                        return false;
                    }
                }
            }
            return true;
        } catch (LinkageError | SecurityException e) {
            // 読めないクラス（反対側にしかないクラスを使っているなど）は、何かするものとして扱う
            return false;
        }
    }
}
