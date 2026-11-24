package io.github.zeusisgood.weakspot.server;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** クライアントから来た座標・生き物を、読む前に確かめる。 */
public final class Reach {

    /**
     * 届く距離の属性に足す余裕（ブロック）。属性を使わずに範囲を伸ばす Mod も受け付けられるよう大きく取る。
     * 守りは読み込みの確認が受け持つので、距離は大きく外れていないかを見るだけ。
     */
    static final double MARGIN = 16;

    private Reach() {
    }

    /** 読み込まれていない座標を読むと、チャンクの読み込み・生成が走るので、距離より先に確かめる。 */
    public static boolean block(Player player, BlockPos pos) {
        return player.level().isLoaded(pos) && player.canReach(pos, MARGIN);
    }

    public static boolean entity(Player player, Entity entity) {
        return player.canReach(entity, MARGIN);
    }
}
