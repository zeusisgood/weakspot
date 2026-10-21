package io.github.zeusisgood.weakspot.network;

import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** ハンドラーはもうサーバーのメインのスレッドで呼ばれる（Network）。値はメッセージから取り出してから渡す。 */
final class ServerThread {

    private ServerThread() {
    }

    static void run(NetworkEvent.Context ctx, Consumer<ServerPlayer> task) {
        ServerPlayer player = ctx.getSender();
        if (player != null) {
            task.accept(player);
        }
    }
}
