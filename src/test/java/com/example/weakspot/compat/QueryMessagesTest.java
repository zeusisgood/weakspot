package com.example.weakspot.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.weakspot.common.HitKind;
import com.example.weakspot.network.QueryMessage;
import com.example.weakspot.network.StateMessage;
import com.example.weakspot.server.QueryHandlers;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Field;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;

/** 1.9.0 でまとめた状態の問い合わせ（QueryMessage / StateMessage）。 */
public class QueryMessagesTest {

    @Test
    public void everyQueriedKindHasAHandler() {
        for (HitKind kind : new HitKind[] {HitKind.ANIMAL, HitKind.FISHING, HitKind.MACHINE}) {
            assertTrue(kind.name(), QueryHandlers.handles(kind));
        }
    }

    @Test
    public void queryRoundTrip() throws Exception {
        BlockPos pos = new BlockPos(12, 64, -300);
        QueryMessage copy = roundTrip(QueryMessage.machine(pos), new QueryMessage());
        assertEquals(HitKind.MACHINE, get(copy, "kind"));
        assertEquals(pos.toLong(), get(copy, "target"));
        copy = roundTrip(QueryMessage.animal(4321), new QueryMessage());
        assertEquals(HitKind.ANIMAL, get(copy, "kind"));
        assertEquals(4321L, get(copy, "target"));
    }

    @Test
    public void stateRoundTrip() throws Exception {
        StateMessage copy = roundTrip(StateMessage.animal(77, 5, new float[] {0.25F, 0.5F, 0, 1}), new StateMessage());
        assertEquals(HitKind.ANIMAL, get(copy, "kind"));
        assertEquals(77L, get(copy, "target"));
        assertEquals(5, get(copy, "flags"));
        float[] values = (float[]) get(copy, "values");
        assertEquals(4, values.length);
        assertEquals(0.5F, values[1], 0);
        copy = roundTrip(StateMessage.machine(new BlockPos(1, 2, 3), 0.75F, -1), new StateMessage());
        values = (float[]) get(copy, "values");
        assertEquals(-1F, values[1], 0);
        assertEquals(new BlockPos(1, 2, 3).toLong(), get(copy, "target"));
    }

    private static <T extends net.minecraftforge.fml.common.network.simpleimpl.IMessage> T roundTrip(T message,
                                                                                                  T empty) {
        ByteBuf buf = Unpooled.buffer();
        message.toBytes(buf);
        empty.fromBytes(buf);
        assertEquals(0, buf.readableBytes());
        return empty;
    }

    private static Object get(Object message, String name) throws Exception {
        Field field = message.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(message);
    }
}
