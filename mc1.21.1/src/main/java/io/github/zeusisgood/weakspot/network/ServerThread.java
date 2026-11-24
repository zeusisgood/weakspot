package io.github.zeusisgood.weakspot.network;

import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** ハンドラーはもうサーバーのメインのスレッドで呼ばれる（Message）。値はメッセージから取り出してから渡す。 */
final class ServerThread {

    private ServerThread() {
    }

    static void run(IPayloadContext ctx, Consumer<ServerPlayer> task) {
        // メインのスレッドに回ったあとに切断が処理されることがある。そのまま動かすと、ログアウトで片付けた記憶を作り直す
        if (ctx.player() instanceof ServerPlayer player && player.server.getPlayerList().getPlayer(player.getUUID()) == player) {
            task.accept(player);
        }
    }
}
