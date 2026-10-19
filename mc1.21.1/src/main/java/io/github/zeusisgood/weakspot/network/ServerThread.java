package io.github.zeusisgood.weakspot.network;

import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** ハンドラーはもうサーバーのメインのスレッドで呼ばれる（Message）。値はメッセージから取り出してから渡す。 */
final class ServerThread {

    private ServerThread() {
    }

    static void run(IPayloadContext ctx, Consumer<ServerPlayer> task) {
        if (ctx.player() instanceof ServerPlayer player) {
            task.accept(player);
        }
    }
}
