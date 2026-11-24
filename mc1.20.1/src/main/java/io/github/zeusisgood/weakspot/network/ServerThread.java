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
        // メインのスレッドに回ったあとに切断が処理されることがある。そのまま動かすと、ログアウトで片付けた記憶を作り直す
        if (player != null && player.server.getPlayerList().getPlayer(player.getUUID()) == player) {
            task.accept(player);
        }
    }
}
