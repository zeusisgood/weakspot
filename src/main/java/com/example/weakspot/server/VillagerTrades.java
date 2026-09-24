package com.example.weakspot.server;

import com.example.weakspot.Reflect;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import org.apache.logging.log4j.LogManager;

/**
 * 村人の取引のロック（使用回数が上限に達した取引）の判定と、バニラの補充と同じリセット。
 * 取引の一覧（buyingList）と、新しい取引の段階を足すメソッド（populateBuyingList）は非公開なので、
 * 開発環境の MCP 名と実際の環境の SRG 名を順に試して読む（Reflect）。
 * 一覧が読めなければ、ロックはないものとして扱う（取引上限のリセットの弱点が出ないだけ）。
 * 一覧が未作成（まだ取引の画面を開いていない）の村人を、こちらから作ってしまわないように、getRecipes は呼ばない。
 */
final class VillagerTrades {

    private static final Field BUYING_LIST = Reflect.field(EntityVillager.class, "villager trade reset",
            "buyingList", "field_70963_i");
    private static final Method POPULATE = Reflect.method(EntityVillager.class, "unlocking new trade tiers",
            "populateBuyingList", "func_175554_cu");

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

    /** ロック（×印）された取引があるか。 */
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

    /**
     * ロックされた取引の使用回数の上限を、バニラの補充と同じだけ増やして、ロックを解除する。
     * unlockNewTier なら、バニラの補充と同じく、新しい取引の段階も解放する。
     */
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
