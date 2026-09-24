package com.example.weakspot.client;

import com.example.weakspot.CommonProxy;
import com.example.weakspot.common.MiningStats;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.network.MarkerMessage.MarkerData;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;

public class ClientProxy extends CommonProxy {

    @Override
    public void init() {
        StatsKeyHandler.register();
        ToggleKeyHandler.register();
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
    public void onStatsReceived(MiningStats session, MiningStats total) {
        Minecraft.getMinecraft().addScheduledTask(() -> StatsScreen.receive(session, total));
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
    public boolean animalWeakSpotActive(int entityId) {
        return AnimalStates.isActive(entityId);
    }

    @Override
    public void onMilestone(int milestone) {
        Minecraft.getMinecraft().addScheduledTask(() -> MilestoneEffects.show(milestone));
    }
}
