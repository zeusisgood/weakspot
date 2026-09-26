package com.example.weakspot.server;

import com.example.weakspot.Reflect;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.config.WeakSpotConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Mod;

/**
 * エンチャントの弱点のヒット通知の検証と効果（論理サーバー。1.7.0）。エンチャント台の画面を開いて物を置いている間に
 * 受け付け、3 つの候補を引き直す。バニラの候補は、プレイヤーの「エンチャントの種」（非公開の xpSeed。エンチャント
 * するたびにバニラが引き直す）と本棚の数で決まるので、種を新しい乱数にして、開いている画面の候補を計算し直す
 * （バニラの送信でクライアントの画面にも届く）。何も減らさない。種は保存されるので、画面を閉じても引き直したまま。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class EnchantHits {

    /** EntityPlayer の非公開の xpSeed（SRG field_175152_f）。読めなければ、エンチャントの弱点を出さない。 */
    private static final Reflect.LazyField XP_SEED =
            Reflect.lazyField(EntityPlayer.class, "the enchanting weak spot", "xpSeed", "field_175152_f");

    private EnchantHits() {
    }

    /** 種を読み書きできるか（できなければ SyncedSettings で enchantWeakSpotEnabled を false にして送る）。 */
    public static boolean isAvailable() {
        return XP_SEED.get() != null;
    }

    public static void onHit(EntityPlayerMP player, int streak) {
        if (!HitGate.allowed(player, HitKind.ENCHANT) || !isAvailable()
                || !(player.openContainer instanceof ContainerEnchantment)) {
            return;
        }
        ContainerEnchantment container = (ContainerEnchantment) player.openContainer;
        if (container.tableInventory.getStackInSlot(0).isEmpty()) {
            return;
        }
        if (!HitGate.ready(player, HitKind.ENCHANT)) {
            return;
        }
        if (!reroll(player, container)) {
            return;
        }
        HitGate.accept(player, HitKind.ENCHANT, new BlockPos(player), streak);
    }

    /** 種を新しい乱数にして、候補を計算し直す。書けなければ false。 */
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
