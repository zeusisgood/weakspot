package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.Reflect;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import org.apache.logging.log4j.LogManager;

/**
* 取引の一覧（buyingList）と populateBuyingList は非公開なので Reflect で読む。一覧が読めなければ、ロックはないものとして扱う。
* 一覧が未作成（まだ取引の画面を開いていない）の村人を、こちらから作ってしまわないように、getRecipes は呼ばない。
*/
final class VillagerTrades {

    private static final Field BUYING_LIST = Reflect.field(EntityVillager.class, "villager trade reset",
            "buyingList", "field_70963_i");
    private static final Method POPULATE = Reflect.method(EntityVillager.class, "unlocking new trade tiers",
            new Class<?>[0], "populateBuyingList", "func_175554_cu");

    private VillagerTrades() {
    }

    private static MerchantRecipeList recipes(EntityVillager villager) {
        if (BUYING_LIST == null) {
            return null;
        }
        try {
            return (MerchantRecipeList) BUYING_LIST.get(villager);
        } catch (IllegalAccessException | ClassCastException e) {
            return null;
        }
    }

    static boolean hasLockedTrade(EntityVillager villager) {
        MerchantRecipeList list = recipes(villager);
        if (list == null) {
            return false;
        }
        for (MerchantRecipe recipe : list) {
            if (recipe.isRecipeDisabled()) {
                return true;
            }
        }
        return false;
    }

    /** バニラの補充と同じだけ増やす。unlockNewTier なら、新しい取引の段階も解放する。 */
    static void reset(EntityVillager villager, boolean unlockNewTier, Random random) {
        MerchantRecipeList list = recipes(villager);
        if (list == null) {
            return;
        }
        for (MerchantRecipe recipe : list) {
            if (recipe.isRecipeDisabled()) {
                recipe.increaseMaxTradeUses(random.nextInt(6) + random.nextInt(6) + 2);
            }
        }
        if (unlockNewTier && POPULATE != null) {
            try {
                POPULATE.invoke(villager);
            } catch (ReflectiveOperationException | RuntimeException e) {
                LogManager.getLogger("weakspot").warn("Cannot unlock a new trade tier", e);
            }
        }
    }
}
