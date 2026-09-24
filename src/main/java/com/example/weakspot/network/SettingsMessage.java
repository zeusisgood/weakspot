package com.example.weakspot.network;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.config.SyncedSettings;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** サーバー → クライアント: サーバーの設定値（ログイン時と、サーバーの設定が変わったとき）。 */
public class SettingsMessage implements IMessage {

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

    public static class Handler implements IMessageHandler<SettingsMessage, IMessage> {

        @Override
        public IMessage onMessage(SettingsMessage message, MessageContext ctx) {
            WeakSpotMod.proxy.onSettingsReceived(message.settings);
            return null;
        }
    }
}
