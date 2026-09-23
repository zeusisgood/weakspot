package com.example.weakspot;

import net.minecraft.util.math.BlockPos;

/** 物理サーバー用。クライアント専用の処理は ClientProxy で行う。 */
public class CommonProxy {

    public void init() {
    }

    /** 他のプレイヤーがヒットした（サーバー → クライアントのパケットから呼ばれる）。 */
    public void onOtherPlayerHit(BlockPos pos, int streak) {
    }
}
