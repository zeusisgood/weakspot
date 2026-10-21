package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.CommonProxy;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.MiningStats;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.network.HelloMessage;
import io.github.zeusisgood.weakspot.network.MarkerMessage.MarkerData;
import io.github.zeusisgood.weakspot.network.TargetMessage;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;

/** パケットのハンドラーはメインのスレッドで呼ばれるが、1.12.2 と同じく execute で次の区切りに回す（描画の途中に割り込まない）。 */
public class ClientProxy extends CommonProxy {

    @Override
    public void init(IEventBus modBus, ModContainer container) {
        modBus.addListener(ClientProxy::onRegisterKeys);
        MinecraftForge.EVENT_BUS.addListener(ClientProxy::onRegisterClientCommands);
        MinecraftForge.EVENT_BUS.addListener(ClientProxy::onLoggingIn);
        container.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (mc, parent) -> WeakSpotGuiFactory.createConfigGui(parent)));
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        StatsKeyHandler.register(event);
        ToggleKeyHandler.register(event);
    }

    private static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        BugCommand.register(event.getDispatcher());
    }

    /** サーバーが版を知るため（Forge は相手の Mod の版を教えてくれない）。 */
    private static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        WeakSpotMod.network.sendToServer(new HelloMessage());
    }

    @Override
    public void onOtherPlayerHit(BlockPos pos, int streak, boolean miss) {
        Minecraft.getInstance().execute(() -> {
            if (miss) {
                HitSounds.playOtherMiss(pos);
            } else {
                HitSounds.playOther(pos, streak);
            }
        });
    }

    @Override
    public SyncedSettings clientSettings() {
        return ClientSettings.get();
    }

    @Override
    public void onSettingsReceived(SyncedSettings settings) {
        Minecraft.getInstance().execute(() -> ClientSettings.receive(settings));
    }

    @Override
    public void onStatsReceived(MiningStats session, MiningStats total, List<String> topNames, List<Long> topScores) {
        Minecraft.getInstance().execute(() -> {
            TargetRecords.receive(total.targetBest, total.targetRounds, topNames, topScores);
            StatsScreen.receive(session, total);
        });
    }

    @Override
    public void onOtherMarker(int playerEntityId, MarkerData marker) {
        Minecraft.getInstance().execute(() -> OtherMarkers.receive(playerEntityId, marker));
    }

    @Override
    public void onAnimalState(int entityId, int mask, float[] progress) {
        Minecraft.getInstance().execute(() -> AnimalStates.receive(entityId, mask, progress));
    }

    @Override
    public void onOtherCombo(int entityId, int count) {
        Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && entityId == mc.player.getId()) {
                // 自分の ID はサーバーの数での直し
                OwnHits.correctStreak(count);
                return;
            }
            OtherCombos.receive(entityId, count);
        });
    }

    @Override
    public void onMachineState(BlockPos pos, float progress, float fuel) {
        Minecraft.getInstance().execute(
                () -> MachineBars.receive(pos, progress, fuel, ClientWeakSpotHandler.clientTick));
    }

    @Override
    public void onFishingState(boolean waiting, float progress) {
        Minecraft.getInstance().execute(() -> FishingSpot.receive(waiting, progress));
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
        Minecraft.getInstance().execute(() -> MilestoneEffects.show(type, kindId, milestone));
    }

    @Override
    public void onTarget(TargetMessage message) {
        Minecraft.getInstance().execute(() -> TargetPlay.receive(message));
    }

    @Override
    public void onOtherTarget(int entityId, int hits) {
        Minecraft.getInstance().execute(() -> OtherCombos.receiveTarget(entityId, hits));
    }
}
