package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.common.HitKind;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.EnchantmentMenu;

/**
 * バニラの候補は、プレイヤーの「エンチャントの種」（enchantmentSeed。エンチャントするたびにバニラが引き直す）と本棚の数で
 * 決まるので、種を新しい乱数にして、開いている画面の候補を計算し直す（バニラの送信でクライアントの画面にも届く）。
 * 種は保存されるので、画面を閉じても引き直したまま。非公開のフィールドは accesstransformer.cfg。
 */
public final class EnchantHits {

    private EnchantHits() {
    }

    /** 1.12.2 版はリフレクションで読めないと false。1.20.1・1.21.1 版はいつも使える。 */
    public static boolean isAvailable() {
        return true;
    }

    public static void onHit(ServerPlayer player, int streak) {
        if (!HitGate.allowed(player, HitKind.ENCHANT) || !(player.containerMenu instanceof EnchantmentMenu menu)) {
            return;
        }
        if (menu.enchantSlots.getItem(0).isEmpty() || !HitGate.ready(player, HitKind.ENCHANT)) {
            return;
        }
        int seed = player.getRandom().nextInt();
        player.enchantmentSeed = seed;
        menu.enchantmentSeed.set(seed);
        menu.slotsChanged(menu.enchantSlots);
        HitGate.accept(player, HitKind.ENCHANT, player.blockPosition(), streak);
    }
}
