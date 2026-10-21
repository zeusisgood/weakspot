package io.github.zeusisgood.weakspot.server;

import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * 取引の一覧は非公開の offers を読む（accesstransformer.cfg）。一覧が未作成（まだ取引の画面を開いていない）の村人を、
 * こちらから作ってしまわないように、getOffers は呼ばない。
 */
final class VillagerTrades {

    private VillagerTrades() {
    }

    static boolean hasLockedTrade(Villager villager) {
        MerchantOffers offers = villager.offers;
        if (offers == null) {
            return false;
        }
        for (MerchantOffer offer : offers) {
            if (offer.isOutOfStock()) {
                return true;
            }
        }
        return false;
    }

    /**
    * バニラの補充（Villager#restock）と同じく使った回数を戻す。restock そのものは呼ばない（1 日の補充の回数に数えられるため）。
    * unlockNewTier なら、次の職業レベルに上げて新しい取引を足す（1.12 の「新しい段階の解放」に当たる）。
    */
    static void reset(Villager villager, boolean unlockNewTier) {
        MerchantOffers offers = villager.offers;
        if (offers == null) {
            return;
        }
        for (MerchantOffer offer : offers) {
            if (offer.isOutOfStock()) {
                offer.resetUses();
            }
        }
        int level = villager.getVillagerData().getLevel();
        if (unlockNewTier && VillagerData.canLevelUp(level)) {
            villager.setVillagerXp(Math.max(villager.getVillagerXp(), VillagerData.getMinXpPerLevel(level + 1)));
            villager.increaseMerchantCareer();
        }
    }
}
