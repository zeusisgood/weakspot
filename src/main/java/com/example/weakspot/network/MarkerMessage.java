package com.example.weakspot.network;

import com.example.weakspot.server.MarkerRelay;
import com.example.weakspot.server.ServerSwitches;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * クライアント → サーバー: 自分の採掘の弱点マークの状態（出た・動いた / 消えた）。
 * 位置はクライアントが決めた値のまま、サーバーは検証せずに近くの他のプレイヤーへ転送する（見えるだけで、破壊やヒットには影響しない）。
 */
public class MarkerMessage implements IMessage {

    private MarkerData marker;

    public MarkerMessage() {
    }

    /** marker が null なら「消えた」。 */
    public MarkerMessage(MarkerData marker) {
        this.marker = marker;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        marker = MarkerData.read(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        MarkerData.write(buf, marker);
    }

    public static class Handler implements IMessageHandler<MarkerMessage, IMessage> {

        @Override
        public IMessage onMessage(MarkerMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            MarkerData marker = message.marker;
            player.getServerWorld().addScheduledTask(() -> {
                // オフのプレイヤーのマークは転送しない（オフにする直前に送られたものが遅れて届いても消す）
                MarkerRelay.onMarker(player, ServerSwitches.isEnabled(player) ? marker : null);
            });
            return null;
        }
    }

    /** 弱点マークの中身: どのブロックの、どの面の、面の中のどの位置（ワールド座標系の u, v）か。 */
    public static final class MarkerData {
        public final BlockPos pos;
        public final EnumFacing face;
        public final double u;
        public final double v;

        public MarkerData(BlockPos pos, EnumFacing face, double u, double v) {
            this.pos = pos;
            this.face = face;
            this.u = u;
            this.v = v;
        }

        public boolean sameAs(MarkerData other) {
            return other != null && pos.equals(other.pos) && face == other.face && u == other.u && v == other.v;
        }

        /** null（消えた）も書ける。 */
        static void write(ByteBuf buf, MarkerData marker) {
            buf.writeBoolean(marker != null);
            if (marker != null) {
                buf.writeLong(marker.pos.toLong());
                buf.writeByte(marker.face.getIndex());
                buf.writeDouble(marker.u);
                buf.writeDouble(marker.v);
            }
        }

        /** 壊れた値（面の番号が範囲外、座標が数でない）は null（消えた）として扱う。 */
        static MarkerData read(ByteBuf buf) {
            if (!buf.readBoolean()) {
                return null;
            }
            BlockPos pos = BlockPos.fromLong(buf.readLong());
            int face = buf.readByte();
            double u = buf.readDouble();
            double v = buf.readDouble();
            if (face < 0 || face >= EnumFacing.values().length || !Double.isFinite(u) || !Double.isFinite(v)) {
                return null;
            }
            return new MarkerData(pos, EnumFacing.getFront(face), u, v);
        }
    }
}
