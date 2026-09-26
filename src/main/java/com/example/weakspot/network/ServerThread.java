package com.example.weakspot.network;

import java.util.function.Consumer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * クライアント → サーバーのパケットの受け取りの共通の形（1.8.9）。受け取りはネットワークのスレッドで呼ばれるので、
 * 送ってきたプレイヤーを取り出し、処理をサーバーのスレッドで実行する。値はこれを呼ぶ前にメッセージから取り出しておく。
 */
final class ServerThread {

    private ServerThread() {
    }

    /** task をサーバーのスレッドで実行する。返事のパケットはないので null を返す（onMessage の戻り値にする）。 */
    static IMessage run(MessageContext ctx, Consumer<EntityPlayerMP> task) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        player.getServerWorld().addScheduledTask(() -> task.accept(player));
        return null;
    }
}
