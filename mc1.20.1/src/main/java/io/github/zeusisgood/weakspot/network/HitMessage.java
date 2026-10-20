package io.github.zeusisgood.weakspot.network;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.server.HitHandlers;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraftforge.network.NetworkEvent;

/** クライアント → サーバー。streak は、周りのプレイヤーのヒット音の音階を合わせるためだけに使う。 */
public class HitMessage extends Message {

    private HitKind kind;
    private BlockPos pos;
    private int streak;
    /** 動物・近接のとき。それ以外は -1。 */
    private int entityId = -1;

    public HitMessage() {
    }

    public HitMessage(HitKind kind, BlockPos pos, int streak) {
        this.kind = kind;
        this.pos = pos;
        this.streak = streak;
    }

    public static HitMessage entity(HitKind kind, int entityId, int streak) {
        HitMessage message = new HitMessage(kind, BlockPos.ZERO, streak);
        message.entityId = entityId;
        return message;
    }

    /** 釣り・弓。位置も動物も使わない。 */
    public static HitMessage withoutTarget(HitKind kind, int streak) {
        return new HitMessage(kind, BlockPos.ZERO, streak);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        kind = HitKind.byId(buf.readByte());
        pos = BlockPos.of(buf.readLong());
        streak = buf.readInt();
        entityId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(kind.ordinal());
        buf.writeLong(pos.asLong());
        buf.writeInt(streak);
        buf.writeInt(entityId);
    }

    @Override
    void handle(NetworkEvent.Context ctx) {
        HitKind kind = this.kind;
        BlockPos pos = this.pos;
        int streak = this.streak;
        int entityId = this.entityId;
        if (kind == null) {
            return;
        }
        ServerThread.run(ctx, player -> HitHandlers.onHit(player, kind, pos, entityId, streak));
    }
}
