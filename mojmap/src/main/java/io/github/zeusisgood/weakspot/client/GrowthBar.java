package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.RightClickTargets;
import io.github.zeusisgood.weakspot.common.GrowthProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * 値は、名前が age の整数のプロパティなら年齢 ÷ 最大の年齢、苗木は stage。サトウキビ・サボテンは、弱点の効果がかかる
 * 柱の一番上の節の年齢 ÷ 15。どれにも当たらなければバーを出さない。
 */
final class GrowthBar {

    static final double WIDTH = 0.6;
    static final double THICKNESS = 0.06;
    /** ブロックの下端から（ブロック）。 */
    private static final double HEIGHT = 0.08;

    private static final float[] FILL = {0xFF / 255F, 0xD2 / 255F, 0x3F / 255F, 1.0F};
    private static final float[] BACK = {0x1E / 255F, 0x1E / 255F, 0x1E / 255F, 0.5F};

    private GrowthBar() {
    }

    /** 0〜1。バーを出さないときは -1。 */
    static double progress(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        BlockPos target = RightClickTargets.growthTarget(world, pos, state);
        BlockState source = target.equals(pos) ? state : world.getBlockState(target);
        IntegerProperty property = findProperty(source, "age");
        if (property == null) {
            property = findProperty(source, "stage");
        }
        if (property == null) {
            return -1;
        }
        int max = 0;
        for (Integer value : property.getPossibleValues()) {
            max = Math.max(max, value);
        }
        return GrowthProgress.fraction(source.getValue(property), max);
    }

    private static IntegerProperty findProperty(BlockState state, String name) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty integer && property.getName().equals(name)) {
                return integer;
            }
        }
        return null;
    }

    static void draw(BlockPos pos, double progress, double cx, double cy, double cz) {
        WorldBar.draw(pos.getX() + 0.5, pos.getY() + HEIGHT, pos.getZ() + 0.5, WIDTH, THICKNESS, progress,
                FILL, BACK, cx, cy, cz);
    }
}
