package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.common.FallMath;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

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

    /** 地面を探す深さ（ブロック）。これより下に何もなければ、この深さまで落ちるとみなす。 */
    private static final double MAX_DROP = 256;
    /** 今までの落下を減らしすぎたときの下限（負の落下距離は、着地まで「貯め」として残る）。 */
    public static final double MIN_FALL = -MAX_DROP;

    /**
     * このまま真下に落ちて着地したときの落下距離（今までの落下 + 足元から地面までの高さ）。水・溶岩に落ちるなら 0
     * （ダメージがない）。足元の中心から真下に調べるだけの目安。
     */
    public static double landingFall(EntityPlayer player) {
        Vec3d from = new Vec3d(player.posX, player.posY, player.posZ);
        Vec3d to = new Vec3d(player.posX, player.posY - MAX_DROP, player.posZ);
        RayTraceResult hit = player.world.rayTraceBlocks(from, to, true, true, false);
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
            return player.fallDistance + MAX_DROP;
        }
        if (player.world.getBlockState(hit.getBlockPos()).getMaterial().isLiquid()) {
            return 0;
        }
        return player.fallDistance + Math.max(0, player.posY - hit.hitVec.y);
    }

    /** 着地したときの見込み（landingFall から）。 */
    public static FallMath.Outlook landingOutlook(EntityPlayer player) {
        return outlook(player, landingFall(player));
    }

    private static int level(PotionEffect effect) {
        return effect == null ? 0 : effect.getAmplifier() + 1;
    }
}
