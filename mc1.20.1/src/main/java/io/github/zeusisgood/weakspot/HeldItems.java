package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.config.SyncedSettings;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 右クリックで何もしないアイテム（ツルハシ・斧・剣・棒・インゴットなど）を持っていても、手が空いているとみなす。
 * 何もしないかは、アイテムのクラス（Item より下の親を含む）が右クリックの処理を上書きしていないかで、自動で見分ける
 * （Mod のアイテムにも効く）。クライアントとサーバーで同じ判定を使う。
 * 1.20.1 のバニラで確かめた: 食べ物は Item.use そのもので食べる（クラスは上書きしない）ので、isEdible で見分ける。
 */
public final class HeldItems {

    /**
     * 開発環境の Mojang の名前と、配布した jar が動くときの SRG の名前（useOn・use・interactLivingEntity）。
     * onItemUseFirst は Forge が足したもので、名前は変わらない。
     */
    private static final Set<String> RIGHT_CLICK_METHODS = new HashSet<>(Arrays.asList(
            "useOn", "use", "interactLivingEntity", "m_6225_", "m_7203_", "m_6880_", "onItemUseFirst"));

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
        if (item instanceof BlockItem || stack.isEdible()
                || settings.heldItemExcludes.contains(BuiltInRegistries.ITEM.getKey(item).toString())) {
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
        if (entity instanceof Animal animal && animal.isFood(stack)) {
            return false;
        }
        if (entity instanceof Wolf && stack.is(Items.BONE)) {
            return false;
        }
        return !(entity instanceof AbstractHorse && (stack.is(Items.WHEAT) || stack.is(Items.SUGAR)));
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
