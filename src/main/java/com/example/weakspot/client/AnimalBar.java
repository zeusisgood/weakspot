package com.example.weakspot.client;

import net.minecraft.entity.Entity;

/**
 * 弱点が出ている動物の足元の、進み具合のバー（黄 #FFD23F、背景 黒 #1E1E1E 半透明）。描き方は WorldBar（作物の成長バーと同じ部品）。
 * 幅は動物の幅の 80%（小さすぎる・大きすぎるときは上下限）。他のプレイヤーには見せない。
 */
final class AnimalBar {

    private static final double WIDTH_RATIO = 0.8;
    private static final double MIN_WIDTH = 0.3;
    private static final double MAX_WIDTH = 1.2;
    static final double THICKNESS = 0.06;
    /** 動物の足元（箱の下端）からバーの中心までの高さ。 */
    private static final double HEIGHT = 0.1;

    private static final float[] FILL = {0xFF / 255F, 0xD2 / 255F, 0x3F / 255F, 1.0F};
    private static final float[] BACK = {0x1E / 255F, 0x1E / 255F, 0x1E / 255F, 0.5F};

    private AnimalBar() {
    }

    static double width(double entityWidth) {
        return Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, entityWidth * WIDTH_RATIO));
    }

    /** 動物の足元（描く時点の位置）に描く。 */
    static void draw(Entity entity, double progress, float partialTicks, double cx, double cy, double cz) {
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        WorldBar.draw(x, y + HEIGHT, z, width(entity.width), THICKNESS, progress, FILL, BACK, cx, cy, cz);
    }
}
