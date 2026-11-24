package io.github.zeusisgood.weakspot.server;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;

/** クライアントから来た座標・生き物を、読む前に確かめる。 */
public final class Reach {

    /**
     * 届く距離の属性に足す余裕（ブロック）。属性を使わずに範囲を伸ばす Mod も受け付けられるよう大きく取る。
     * 守りは読み込みの確認が受け持つので、距離は大きく外れていないかを見るだけ。
     */
    static final double MARGIN = 16;

    private Reach() {
    }

    /** 読み込まれていない座標を読むと、チャンクの読み込み・生成が走るので、距離より先に確かめる。目からブロックの中心まで。 */
    public static boolean block(EntityPlayer player, BlockPos pos) {
        if (!player.world.isBlockLoaded(pos)) {
            return false;
        }
        double reach = reach(player);
        return player.getPositionEyes(1).squareDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                < reach * reach;
    }

    public static boolean entity(EntityPlayer player, Entity entity) {
        double reach = reach(player);
        return player.getDistanceSq(entity) < reach * reach;
    }

    private static double reach(EntityPlayer player) {
        return player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue() + MARGIN;
    }
}
