package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.common.FallMath;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

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
     * このまま真下に落ちて着地したときの落下距離（今までの落下 + 足元から地面までの高さ）。水・溶岩（流れているものも）に
     * 落ちるなら 0（ダメージがない）。足元の中心の列を 1 ブロックずつ下に調べ、ぶつかる形のあるブロックの上面を地面とする
     * （草・花のような形のないブロックは飛ばす。1.11.0 の 2 回目の試してもらったあと: 光線で調べていたときは、形のない水を
     * 素通りして水底を地面と見ていた）。目安なので、体の幅や横の動きは見ない。
     */
    public static double landingFall(EntityPlayer player) {
        World world = player.world;
        int x = MathHelper.floor(player.posX);
        int z = MathHelper.floor(player.posZ);
        int top = MathHelper.floor(player.posY);
        int bottom = Math.max(0, top - (int) MAX_DROP);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = top; y >= bottom; y--) {
            pos.setPos(x, y, z);
            if (!world.isBlockLoaded(pos)) {
                break;
            }
            IBlockState state = world.getBlockState(pos);
            if (state.getMaterial().isLiquid()) {
                return 0;
            }
            AxisAlignedBB box = state.getCollisionBoundingBox(world, pos);
            if (box != null && y + box.maxY <= player.posY + 1e-6) {
                return player.fallDistance + (player.posY - (y + box.maxY));
            }
        }
        return player.fallDistance + MAX_DROP;
    }

    /** 着地したときの見込み（landingFall から）。 */
    public static FallMath.Outlook landingOutlook(EntityPlayer player) {
        return outlook(player, landingFall(player));
    }

    private static int level(PotionEffect effect) {
        return effect == null ? 0 : effect.getAmplifier() + 1;
    }
}
