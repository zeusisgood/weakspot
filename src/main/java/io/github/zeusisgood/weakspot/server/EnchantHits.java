package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.Reflect;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.util.math.BlockPos;

/**
 * バニラの候補は、プレイヤーの「エンチャントの種」（非公開の xpSeed。エンチャントするたびにバニラが引き直す）と本棚の数で
 * 決まるので、種を新しい乱数にして、開いている画面の候補を計算し直す（バニラの送信でクライアントの画面にも届く）。
 * 種は保存されるので、画面を閉じても引き直したまま。
 */
public final class EnchantHits {

    /** 読めなければ、エンチャントの弱点を出さない。 */
    private static final Reflect.LazyField XP_SEED =
            Reflect.lazyField(EntityPlayer.class, "the enchanting weak spot", "xpSeed", "field_175152_f");

    private EnchantHits() {
    }

    /** できなければ SyncedSettings で enchantWeakSpotEnabled を false にして送る。 */
    public static boolean isAvailable() {
        return XP_SEED.get() != null;
    }

    public static void onHit(EntityPlayerMP player, int streak) {
        if (!HitGate.allowed(player, HitKind.ENCHANT) || !isAvailable()
                || !(player.openContainer instanceof ContainerEnchantment)) {
            return;
        }
        ContainerEnchantment container = (ContainerEnchantment) player.openContainer;
        if (container.tableInventory.getStackInSlot(0).isEmpty() || !HitGate.ready(player, HitKind.ENCHANT)) {
            return;
        }
        if (!reroll(player, container)) {
            return;
        }
        HitGate.accept(player, HitKind.ENCHANT, new BlockPos(player), streak);
    }

    /** 書けなければ false。 */
    private static boolean reroll(EntityPlayerMP player, ContainerEnchantment container) {
        int seed = player.getRNG().nextInt();
        try {
            XP_SEED.get().setInt(player, seed);
        } catch (IllegalAccessException | RuntimeException e) {
            return false;
        }
        container.xpSeed = seed;
        container.onCraftMatrixChanged(container.tableInventory);
        return true;
    }

}
