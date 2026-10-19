package io.github.zeusisgood.weakspot.network;

import java.util.function.Consumer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** 受け取りはネットワークのスレッドで呼ばれる。値はこれを呼ぶ前にメッセージから取り出しておく。 */
final class ServerThread {

    private ServerThread() {
    }

    /** 返事のパケットはないので null を返す（onMessage の戻り値にする）。 */
    static IMessage run(MessageContext ctx, Consumer<EntityPlayerMP> task) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        player.getServerWorld().addScheduledTask(() -> task.accept(player));
        return null;
    }
}
