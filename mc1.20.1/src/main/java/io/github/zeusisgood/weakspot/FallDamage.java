package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.common.FallMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 両側（クライアントの表示と、サーバーの進捗「九死に一生」で同じ計算を使う）。 */
public final class FallDamage {

    private FallDamage() {
    }

    /** 体力の点。 */
    public static double expected(Player player, double fallDistance) {
        return FallMath.damage(fallDistance, level(player.getEffect(MobEffects.JUMP)),
                level(player.getEffect(MobEffects.DAMAGE_RESISTANCE)), protection(player));
    }

    /** バニラと同じ数え方（落下耐性・ダメージ軽減などの防具のエンチャント。Mod のエンチャントも含む）。 */
    private static int protection(Player player) {
        return EnchantmentHelper.getDamageProtection(player.getArmorSlots(), player.damageSources().fall());
    }

    public static FallMath.Outlook outlook(Player player, double fallDistance) {
        boolean totem = player.getMainHandItem().is(Items.TOTEM_OF_UNDYING)
                || player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
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
    public static double landingFall(Player player) {
        Level world = player.level();
        int x = Mth.floor(player.getX());
        int z = Mth.floor(player.getZ());
        int top = Mth.floor(player.getY());
        int bottom = Math.max(world.getMinBuildHeight(), top - (int) MAX_DROP);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = top; y >= bottom; y--) {
            pos.set(x, y, z);
            if (!world.isLoaded(pos)) {
                break;
            }
            BlockState state = world.getBlockState(pos);
            if (!state.getFluidState().isEmpty()) {
                return 0;
            }
            VoxelShape shape = state.getCollisionShape(world, pos);
            if (!shape.isEmpty()) {
                double maxY = shape.max(Direction.Axis.Y);
                if (y + maxY <= player.getY() + 1e-6) {
                    return player.fallDistance + (player.getY() - (y + maxY));
                }
            }
        }
        return player.fallDistance + MAX_DROP;
    }

    public static FallMath.Outlook landingOutlook(Player player) {
        return outlook(player, landingFall(player));
    }

    private static int level(MobEffectInstance effect) {
        return effect == null ? 0 : effect.getAmplifier() + 1;
    }
}
