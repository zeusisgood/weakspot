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
* 右クリックで何もしないアイテム（ツルハシ・斧・剣・棒・インゴットなど）を持っていても、手が空いているとみなす。
* 何もしないかは、アイテムのクラス（Item より下の親を含む）が右クリックの処理を上書きしていないかで、自動で見分ける
* （Mod のアイテムにも効く）。クライアントとサーバーで同じ判定を使う。
* 1.12.2 のバニラで確かめた（doc/spec/SPEC_v1.11.0.md の 7.2）。移植のときは、版ごとに調べ直す。
*/
public final class HeldItems {

    /** 開発環境の MCP 名と、実際の環境の SRG 名。onItemUseFirst は Forge が足したもの。 */
    private static final Set<String> RIGHT_CLICK_METHODS = new HashSet<>(Arrays.asList(
            "onItemUse", "func_180614_a", "onItemRightClick", "func_77659_a",
            "itemInteractionForEntity", "func_111207_a", "onItemUseFirst"));
    private static final Map<Class<?>, Boolean> DOES_NOTHING = new ConcurrentHashMap<>();

    private HeldItems() {
    }

    /** ブロック（成長・収穫・機械）の弱点の判定。 */
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

    /** isFree に加えて、その動物が反応するアイテム（餌など）を外す。 */
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

    /** Item より下の親を含む。 */
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
