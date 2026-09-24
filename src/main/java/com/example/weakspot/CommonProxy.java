package com.example.weakspot;

import com.example.weakspot.common.MiningStats;
import com.example.weakspot.config.SyncedSettings;
import net.minecraft.util.math.BlockPos;

/** 物理サーバー用。クライアント専用の処理は ClientProxy で行う。 */
public class CommonProxy {

    public void init() {
    }

    /** 他のプレイヤーがヒットした（サーバー → クライアントのパケットから呼ばれる）。 */
    public void onOtherPlayerHit(BlockPos pos, int streak) {
    }

    /** サーバーの設定値が届いた（サーバー → クライアントのパケットから呼ばれる）。 */
    public void onSettingsReceived(SyncedSettings settings) {
    }

    /** 自分の統計が届いた（サーバー → クライアントのパケットから呼ばれる）。 */
    public void onStatsReceived(MiningStats session, MiningStats total) {
    }
}
