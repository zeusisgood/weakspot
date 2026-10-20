package io.github.zeusisgood.weakspot.network;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * パケットの登録と送信（1.12.2 の SimpleNetworkWrapper と同じ呼び方: WeakSpotMod.network.sendTo / sendToServer）。
 * 両方に Mod が要るので、相手に同じ印のチャンネルがなければ接続できない。
 */
public final class Network {

    /** 接続できる相手の版の印。マイナーを上げたら変える（WeakSpotMod.ACCEPTED_VERSIONS と同じ考え）。 */
    private static final String PROTOCOL = "1.13";

    private final SimpleChannel channel = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(WeakSpotMod.MODID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private int nextId;

    public void register() {
        toServer(HitMessage.class, HitMessage::new);
        toClient(OtherHitMessage.class, OtherHitMessage::new);
        toClient(SettingsMessage.class, SettingsMessage::new);
        toServer(StatsRequestMessage.class, StatsRequestMessage::new);
        toClient(StatsMessage.class, StatsMessage::new);
        toServer(MarkerMessage.class, MarkerMessage::new);
        toClient(OtherMarkerMessage.class, OtherMarkerMessage::new);
        toServer(SwitchMessage.class, SwitchMessage::new);
        toServer(QueryMessage.class, QueryMessage::new);
        toClient(StateMessage.class, StateMessage::new);
        toClient(OtherComboMessage.class, OtherComboMessage::new);
        toClient(MilestoneMessage.class, MilestoneMessage::new);
        toClient(TargetMessage.class, TargetMessage::new);
        toServer(TargetActionMessage.class, TargetActionMessage::new);
        toClient(OtherTargetMessage.class, OtherTargetMessage::new);
        toServer(HelloMessage.class, HelloMessage::new);
    }

    private <T extends Message> void toServer(Class<T> type, Supplier<T> factory) {
        define(type, factory, NetworkDirection.PLAY_TO_SERVER);
    }

    private <T extends Message> void toClient(Class<T> type, Supplier<T> factory) {
        define(type, factory, NetworkDirection.PLAY_TO_CLIENT);
    }

    /** ハンドラーはネットワークのスレッドで呼ばれるので、メインのスレッドへ回す。 */
    private <T extends Message> void define(Class<T> type, Supplier<T> factory, NetworkDirection direction) {
        channel.messageBuilder(type, nextId++, direction)
                .encoder((message, buf) -> message.toBytes(buf))
                .decoder(buf -> {
                    T message = factory.get();
                    message.fromBytes(buf);
                    return message;
                })
                .consumerMainThread((message, ctx) -> handle(message, ctx))
                .add();
    }

    private static void handle(Message message, Supplier<NetworkEvent.Context> ctx) {
        message.handle(ctx.get());
    }

    public void sendToServer(Message message) {
        channel.sendToServer(message);
    }

    public void sendTo(Message message, ServerPlayer player) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
