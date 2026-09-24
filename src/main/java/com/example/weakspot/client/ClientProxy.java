package com.example.weakspot.client;

import com.example.weakspot.CommonProxy;
import com.example.weakspot.config.SyncedSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;

public class ClientProxy extends CommonProxy {

    @Override
    public void init() {
        StatsKeyHandler.register();
    }

    @Override
    public void onOtherPlayerHit(BlockPos pos, int streak) {
        Minecraft.getMinecraft().addScheduledTask(() -> OtherHitSounds.play(pos, streak));
    }

    @Override
    public void onSettingsReceived(SyncedSettings settings) {
        Minecraft.getMinecraft().addScheduledTask(() -> ClientSettings.receive(settings));
    }
}
