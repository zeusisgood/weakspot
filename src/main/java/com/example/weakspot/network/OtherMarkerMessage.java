package com.example.weakspot.network;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.network.MarkerMessage.MarkerData;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * サーバー → クライアント: 近くの他のプレイヤーの弱点マークの状態（転送、範囲に入ったときの現在の状態、消えた通知）。
 * ハンドラーは専用サーバーでもインスタンス化されるので、クライアントのクラスは proxy 経由で触る。
 */
public class OtherMarkerMessage implements IMessage {

    private int playerEntityId;
    private MarkerData marker;

    public OtherMarkerMessage() {
    }

    /** marker が null なら「消えた」。 */
    public OtherMarkerMessage(int playerEntityId, MarkerData marker) {
        this.playerEntityId = playerEntityId;
        this.marker = marker;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        playerEntityId = buf.readInt();
        marker = MarkerData.read(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(playerEntityId);
        MarkerData.write(buf, marker);
    }

    public static class Handler implements IMessageHandler<OtherMarkerMessage, IMessage> {

        @Override
        public IMessage onMessage(OtherMarkerMessage message, MessageContext ctx) {
            WeakSpotMod.proxy.onOtherMarker(message.playerEntityId, message.marker);
            return null;
        }
    }
}
