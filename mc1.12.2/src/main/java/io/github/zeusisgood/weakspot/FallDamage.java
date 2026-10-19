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

/** 両側（クライアントの表示と、サーバーの進捗「九死に一生」で同じ計算を使う）。 */
public final class FallDamage {

    private FallDamage() {
    }

    /** 体力の点。 */
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

    /** ブロック。これより下に何もなければ、この深さまで落ちるとみなす。 */
    private static final double MAX_DROP = 256;
    /** 負の落下距離は、着地まで「貯め」として残る。 */
    public static final double MIN_FALL = -MAX_DROP;

    /**
    * 水・溶岩（流れているものも）に落ちるなら 0。ぶつかる形のあるブロックの上面を地面とする（光線で調べると、形のない水を
    * 素通りして水底を地面と見てしまう）。目安なので、体の幅や横の動きは見ない。
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

    public static FallMath.Outlook landingOutlook(EntityPlayer player) {
        return outlook(player, landingFall(player));
    }

    private static int level(PotionEffect effect) {
        return effect == null ? 0 : effect.getAmplifier() + 1;
    }
}
