package com.example.weakspot.client;

import com.example.weakspot.RightClickTargets;
import com.example.weakspot.common.GrowthProgress;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 成長の弱点を出している作物の足元の、成長の進み具合のバー（黄）。描き方は WorldBar に任せる。
 *
 * 値: 名前が age の整数のプロパティ（小麦、ニンジン、ジャガイモ、ビートルート、くき、ココア、ネザーウォート、
 * MOD の作物の多く）は、年齢 ÷ 最大の年齢。苗木は stage。サトウキビ・サボテンは、弱点の効果がかかる
 * 柱の一番上の節の年齢 ÷ 15。どれにも当たらなければバーを出さない。
 */
final class GrowthBar {

    static final double WIDTH = 0.6;
    static final double THICKNESS = 0.06;
    /** ブロックの下端からバーの中心までの高さ。 */
    private static final double HEIGHT = 0.08;

    /** 進み具合（黄 #FFD23F）と、バーの全体の背景（黒 #1E1E1E、半透明）。 */
    private static final float[] FILL = {0xFF / 255F, 0xD2 / 255F, 0x3F / 255F, 1.0F};
    private static final float[] BACK = {0x1E / 255F, 0x1E / 255F, 0x1E / 255F, 0.5F};

    private GrowthBar() {
    }

    /** pos の作物の進み具合（0〜1）。バーを出さないときは -1。 */
    static double progress(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        BlockPos target = RightClickTargets.growthTarget(world, pos, state);
        IBlockState source = target.equals(pos) ? state : world.getBlockState(target);
        PropertyInteger property = findProperty(source, "age");
        if (property == null) {
            property = findProperty(source, "stage");
        }
        if (property == null) {
            return -1;
        }
        int max = 0;
        for (Integer value : property.getAllowedValues()) {
            max = Math.max(max, value);
        }
        return GrowthProgress.fraction(source.getValue(property), max);
    }

    private static PropertyInteger findProperty(IBlockState state, String name) {
        for (IProperty<?> property : state.getPropertyKeys()) {
            if (property instanceof PropertyInteger && property.getName().equals(name)) {
                return (PropertyInteger) property;
            }
        }
        return null;
    }

    /** pos のブロックの足元（中央）に描く。 */
    static void draw(BlockPos pos, double progress, double cx, double cy, double cz) {
        WorldBar.draw(pos.getX() + 0.5, pos.getY() + HEIGHT, pos.getZ() + 0.5, WIDTH, THICKNESS, progress,
                FILL, BACK, cx, cy, cz);
    }
}
