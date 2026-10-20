package io.github.zeusisgood.weakspot.network;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** サーバー → クライアント。 */
public class SettingsMessage extends Message {

    private SyncedSettings settings;

    public SettingsMessage() {
    }

    public SettingsMessage(SyncedSettings settings) {
        this.settings = settings;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        settings = SyncedSettings.read(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        settings.write(buf);
    }

    @Override
    void handle(NetworkEvent.Context ctx) {
        WeakSpotMod.proxy.onSettingsReceived(settings);
    }
}
