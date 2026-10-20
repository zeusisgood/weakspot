package io.github.zeusisgood.weakspot.network;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraftforge.network.NetworkEvent;

/**
 * サーバー → クライアント。ヒット音を鳴らすためだけに使う。pos は鳴らす位置、miss は的当ての ✕ に当てたとき。
 * ハンドラーは専用サーバーでもインスタンス化されるので、クライアントのクラスは proxy 経由で触る。
 */
public class OtherHitMessage extends Message {

    private BlockPos pos;
    private int streak;
    private boolean miss;

    public OtherHitMessage() {
    }

    public OtherHitMessage(BlockPos pos, int streak) {
        this(pos, streak, false);
    }

    public OtherHitMessage(BlockPos pos, int streak, boolean miss) {
        this.pos = pos;
        this.streak = streak;
        this.miss = miss;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.of(buf.readLong());
        streak = buf.readInt();
        miss = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.asLong());
        buf.writeInt(streak);
        buf.writeBoolean(miss);
    }

    @Override
    void handle(NetworkEvent.Context ctx) {
        WeakSpotMod.proxy.onOtherPlayerHit(pos, streak, miss);
    }
}
