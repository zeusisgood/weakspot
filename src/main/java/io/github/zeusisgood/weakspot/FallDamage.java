package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.common.FallMath;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;

/**
 * プレイヤーの今の状態から、落ちた距離 fallDistance で着地したときの見込み（1.11.0。両側。クライアントの表示と、
 * サーバーの進捗「九死に一生」で同じ計算を使う）。跳躍力上昇・耐性のポーション効果・防具のエンチャント・衝撃吸収・
 * 手に持った不死のトーテムを見る。
 */
public final class FallDamage {

    private FallDamage() {
    }

    /** 見込みのダメージ（体力の点）。 */
    public static double expected(EntityPlayer player, double fallDistance) {
        return FallMath.damage(fallDistance, level(player.getActivePotionEffect(MobEffects.JUMP_BOOST)),
                level(player.getActivePotionEffect(MobEffects.RESISTANCE)),
                EnchantmentHelper.getEnchantmentModifierDamage(player.getArmorInventoryList(), DamageSource.FALL));
    }

    public static FallMath.Outlook outlook(EntityPlayer player, double fallDistance) {
        boolean totem = player.getHeldItemMainhand().getItem() == Items.TOTEM_OF_UNDYING
                || player.getHeldItemOffhand().getItem() == Items.TOTEM_OF_UNDYING;
        return FallMath.outlook(expected(player, fallDistance), player.getHealth(), player.getAbsorptionAmount(),
                totem);
    }

    private static int level(PotionEffect effect) {
        return effect == null ? 0 : effect.getAmplifier() + 1;
    }
}
