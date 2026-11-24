package io.github.zeusisgood.weakspot.network;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

/** 1.12.2 の ByteBufUtils.writeUTF8String / readUTF8String の代わり（相手は同じ版だけなので、形は版ごとでよい）。 */
public final class Bytes {

    /** 壊れた・悪意のある長さで大きな配列を作らないための上限（バイト）。 */
    private static final int MAX_STRING_BYTES = 32767 * 4;

    private Bytes() {
    }

    public static void writeString(ByteBuf buf, String string) {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }

    public static String readString(ByteBuf buf) {
        int length = buf.readInt();
        if (length < 0 || length > MAX_STRING_BYTES || length > buf.readableBytes()) {
            throw new IllegalArgumentException("bad string length " + length);
        }
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
