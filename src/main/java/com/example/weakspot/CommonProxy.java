package com.example.weakspot;

import com.example.weakspot.common.MiningStats;
import com.example.weakspot.config.SyncedSettings;
import net.minecraft.util.math.BlockPos;

/** 物理サーバー用。クライアント専用の処理は ClientProxy で行う。 */
public class CommonProxy {

    public void init() {
    }

    /** クライアントが今使う [サーバー] の設定値。物理サーバーでは呼ばれないが、念のため自分の値を返す。 */
    public SyncedSettings clientSettings() {
        return SyncedSettings.fromConfig();
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

    /** 採掘ヒットの累計が節目に達した（サーバー → クライアントのパケットから呼ばれる）。 */
    public void onMilestone(int milestone) {
    }
}
