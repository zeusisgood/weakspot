package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.CommonProxy;
import io.github.zeusisgood.weakspot.common.MiningStats;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.network.MarkerMessage.MarkerData;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.ClientCommandHandler;

public class ClientProxy extends CommonProxy {

    @Override
    public void init() {
        StatsKeyHandler.register();
        ToggleKeyHandler.register();
        ClientCommandHandler.instance.registerCommand(new BugCommand());
    }

    @Override
    public void onOtherPlayerHit(BlockPos pos, int streak) {
        Minecraft.getMinecraft().addScheduledTask(() -> HitSounds.playOther(pos, streak));
    }

    @Override
    public SyncedSettings clientSettings() {
        return ClientSettings.get();
    }

    @Override
    public void onSettingsReceived(SyncedSettings settings) {
        Minecraft.getMinecraft().addScheduledTask(() -> ClientSettings.receive(settings));
    }

    @Override
    public void onStatsReceived(MiningStats session, MiningStats total, List<String> topNames, List<Long> topScores) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            TargetRecords.receive(total.targetBest, total.targetRounds, topNames, topScores);
            StatsScreen.receive(session, total);
        });
    }

    @Override
    public void onOtherMarker(int playerEntityId, MarkerData marker) {
        Minecraft.getMinecraft().addScheduledTask(() -> OtherMarkers.receive(playerEntityId, marker));
    }

    @Override
    public void onAnimalState(int entityId, int mask, float[] progress) {
        Minecraft.getMinecraft().addScheduledTask(() -> AnimalStates.receive(entityId, mask, progress));
    }

    @Override
    public void onOtherCombo(int entityId, int count) {
        Minecraft.getMinecraft().addScheduledTask(() -> OtherCombos.receive(entityId, count));
    }

    @Override
    public void onMachineState(BlockPos pos, float progress, float fuel) {
        Minecraft.getMinecraft().addScheduledTask(
                () -> MachineBars.receive(pos, progress, fuel, ClientWeakSpotHandler.clientTick));
    }

    @Override
    public void onFishingState(boolean waiting, float progress) {
        Minecraft.getMinecraft().addScheduledTask(() -> FishingSpot.receive(waiting, progress));
    }

    @Override
    public boolean animalWeakSpotActive(int entityId) {
        return AnimalStates.isActive(entityId);
    }

    @Override
    public boolean isKindEnabled(HitKind kind) {
        return KindSwitches.isEnabled(kind);
    }

    @Override
    public void onMilestone(int type, int kindId, long milestone) {
        Minecraft.getMinecraft().addScheduledTask(() -> MilestoneEffects.show(type, kindId, milestone));
    }

    @Override
    public void onTarget(byte type, int hits, long best, boolean newBest, int newTier) {
        Minecraft.getMinecraft().addScheduledTask(() -> TargetPlay.receive(type, hits, best, newBest, newTier));
    }
}
