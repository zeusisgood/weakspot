package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.MiningStats;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.network.MarkerMessage.MarkerData;
import io.github.zeusisgood.weakspot.network.TargetMessage;
import java.util.List;
import net.minecraft.core.BlockPos;

/** 物理サーバー用。on〜 はサーバー → クライアントのパケットから呼ばれる。serverSince・isKindEnabled は物理サーバーでは呼ばれない。 */
public class CommonProxy {

    public void init(net.minecraftforge.eventbus.api.IEventBus modBus, net.minecraftforge.fml.ModContainer container) {
    }

    /** 物理サーバーでは呼ばれないが、念のため自分の値を返す。 */
    public SyncedSettings clientSettings() {
        return SyncedSettings.server();
    }

    public void onOtherPlayerHit(BlockPos pos, int streak, boolean miss) {
    }

    public void onSettingsReceived(SyncedSettings settings) {
    }

    public void onStatsReceived(MiningStats session, MiningStats total, List<String> topNames, List<Long> topScores) {
    }

    /** marker が null なら消えた。 */
    public void onOtherMarker(int playerEntityId, MarkerData marker) {
    }

    /** progress は Timer の順番。 */
    public void onAnimalState(int entityId, int mask, float[] progress) {
    }

    public void onOtherCombo(int entityId, int count) {
    }

    /** fuel が負なら燃料なし。 */
    public void onMachineState(BlockPos pos, float progress, float fuel) {
    }

    public void onFishingState(boolean waiting, float progress) {
    }

    /** サーバーの返事を覚えているか。 */
    public boolean animalWeakSpotActive(int entityId) {
        return false;
    }

    public boolean serverSince(String version) {
        return true;
    }

    public boolean isKindEnabled(HitKind kind) {
        return true;
    }

    /** type は MilestoneMessage の MINING / KIND / TOTAL。 */
    public void onMilestone(int type, int kindId, long milestone) {
    }

    public void onTarget(TargetMessage message) {
    }

    /** 負なら終わった。 */
    public void onOtherTarget(int entityId, int hits) {
    }
}
