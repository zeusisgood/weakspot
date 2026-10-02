package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.MiningStats;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.network.MarkerMessage.MarkerData;
import io.github.zeusisgood.weakspot.network.TargetMessage;
import java.util.List;
import net.minecraft.util.math.BlockPos;

/** 物理サーバー用。クライアント専用の処理は ClientProxy で行う。 */
public class CommonProxy {

    public void init() {
    }

    /** クライアントが今使う [サーバー] の設定値。物理サーバーでは呼ばれないが、念のため自分の値を返す。 */
    public SyncedSettings clientSettings() {
        return SyncedSettings.server();
    }

    /** 他のプレイヤーがヒットした（サーバー → クライアントのパケットから呼ばれる）。 */
    public void onOtherPlayerHit(BlockPos pos, int streak, boolean miss) {
    }

    /** サーバーの設定値が届いた（サーバー → クライアントのパケットから呼ばれる）。 */
    public void onSettingsReceived(SyncedSettings settings) {
    }

    /** 自分の統計が届いた（サーバー → クライアントのパケットから呼ばれる）。 */
    public void onStatsReceived(MiningStats session, MiningStats total, List<String> topNames, List<Long> topScores) {
    }

    /** 近くの他のプレイヤーの弱点マークが届いた。marker が null なら消えた（サーバー → クライアントのパケットから呼ばれる）。 */
    public void onOtherMarker(int playerEntityId, MarkerData marker) {
    }

    /** 動物の状態が届いた（サーバー → クライアントのパケットから呼ばれる）。progress は Timer の順番の進み具合。 */
    public void onAnimalState(int entityId, int mask, float[] progress) {
    }

    /** 近くのほかのプレイヤーのコンボが届いた（1.6.0。サーバー → クライアントのパケットから呼ばれる）。 */
    public void onOtherCombo(int entityId, int count) {
    }

    /** 機械の進み具合が届いた（1.6.0。サーバー → クライアントのパケットから呼ばれる）。fuel が負なら燃料なし。 */
    public void onMachineState(BlockPos pos, float progress, float fuel) {
    }

    /** 釣りの浮きの状態が届いた（サーバー → クライアントのパケットから呼ばれる）。 */
    public void onFishingState(boolean waiting, float progress) {
    }

    /** クライアントが、この動物に弱点が出ていると知っているか（サーバーの返事を覚えているか）。 */
    public boolean animalWeakSpotActive(int entityId) {
        return false;
    }

    /** 自分のその種類の弱点がオンか（1.7.0。一時オフと、種類ごとのオフ）。物理サーバーでは呼ばれない。 */
    public boolean isKindEnabled(HitKind kind) {
        return true;
    }

    /**
     * ヒットの累計が節目に達した（サーバー → クライアントのパケットから呼ばれる）。type は MilestoneMessage の
     * MINING / KIND / TOTAL、kindId は種類ごとの節目の HitKind の番号。
     */
    public void onMilestone(int type, int kindId, long milestone) {
    }

    /** 的当ての状態が届いた（1.11.0。サーバー → クライアントのパケットから呼ばれる。END には結果の板の中身も付く）。 */
    public void onTarget(TargetMessage message) {
    }

    /** 近くのほかのプレイヤーの的当てのヒット数が届いた（1.11.0。負なら終わった）。 */
    public void onOtherTarget(int entityId, int hits) {
    }
}
