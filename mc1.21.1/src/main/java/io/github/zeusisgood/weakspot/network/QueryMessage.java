package io.github.zeusisgood.weakspot.network;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.server.QueryHandlers;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** クライアント → サーバー。サーバーは StateMessage で返す。問い合わせの間隔は、クライアントの QueryThrottle が決める。 */
public class QueryMessage extends Message {

    private HitKind kind;
    private long target;

    public QueryMessage() {
    }

    private QueryMessage(HitKind kind, long target) {
        this.kind = kind;
        this.target = target;
    }

    public static QueryMessage animal(int entityId) {
        return new QueryMessage(HitKind.ANIMAL, entityId);
    }

    public static QueryMessage fishing() {
        return new QueryMessage(HitKind.FISHING, 0);
    }

    public static QueryMessage machine(BlockPos pos) {
        return new QueryMessage(HitKind.MACHINE, pos.asLong());
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        kind = HitKind.byId(buf.readUnsignedByte());
        target = buf.readLong();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(kind.ordinal());
        buf.writeLong(target);
    }

    @Override
    void handle(IPayloadContext ctx) {
        HitKind kind = this.kind;
        long target = this.target;
        if (kind == null) {
            return;
        }
        ServerThread.run(ctx, player -> QueryHandlers.onQuery(player, kind, target));
    }
}
