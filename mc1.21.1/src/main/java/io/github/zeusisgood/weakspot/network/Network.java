package io.github.zeusisgood.weakspot.network;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * パケットの登録と送信（1.12.2 の SimpleNetworkWrapper と同じ呼び方: WeakSpotMod.network.sendTo / sendToServer）。
 * 両方に Mod が要るので、optional にしない（入っていない相手とは接続できない）。
 */
public final class Network {

    /** 接続できる相手の版の印。マイナーを上げたら変える（WeakSpotMod.ACCEPTED_VERSIONS と同じ考え）。 */
    private static final String PROTOCOL = "1.12";

    private static final Map<Class<?>, CustomPacketPayload.Type<?>> TYPES = new HashMap<>();

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        toServer(registrar, "hit", HitMessage.class, HitMessage::new);
        toClient(registrar, "other_hit", OtherHitMessage.class, OtherHitMessage::new);
        toClient(registrar, "settings", SettingsMessage.class, SettingsMessage::new);
        toServer(registrar, "stats_request", StatsRequestMessage.class, StatsRequestMessage::new);
        toClient(registrar, "stats", StatsMessage.class, StatsMessage::new);
        toServer(registrar, "marker", MarkerMessage.class, MarkerMessage::new);
        toClient(registrar, "other_marker", OtherMarkerMessage.class, OtherMarkerMessage::new);
        toServer(registrar, "switch", SwitchMessage.class, SwitchMessage::new);
        toServer(registrar, "query", QueryMessage.class, QueryMessage::new);
        toClient(registrar, "state", StateMessage.class, StateMessage::new);
        toClient(registrar, "other_combo", OtherComboMessage.class, OtherComboMessage::new);
        toClient(registrar, "milestone", MilestoneMessage.class, MilestoneMessage::new);
        toClient(registrar, "target", TargetMessage.class, TargetMessage::new);
        toServer(registrar, "target_action", TargetActionMessage.class, TargetActionMessage::new);
        toClient(registrar, "other_target", OtherTargetMessage.class, OtherTargetMessage::new);
        toServer(registrar, "hello", HelloMessage.class, HelloMessage::new);
    }

    @SuppressWarnings("unchecked")
    static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> typeOf(Class<?> messageClass) {
        return (CustomPacketPayload.Type<T>) TYPES.get(messageClass);
    }

    private static <T extends Message> void toServer(PayloadRegistrar registrar, String name, Class<T> type,
                                                     Supplier<T> factory) {
        registrar.playToServer(define(name, type), codec(factory), (message, ctx) -> message.handle(ctx));
    }

    private static <T extends Message> void toClient(PayloadRegistrar registrar, String name, Class<T> type,
                                                     Supplier<T> factory) {
        registrar.playToClient(define(name, type), codec(factory), (message, ctx) -> message.handle(ctx));
    }

    private static <T extends Message> CustomPacketPayload.Type<T> define(String name, Class<T> messageClass) {
        CustomPacketPayload.Type<T> type =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(WeakSpotMod.MODID, name));
        TYPES.put(messageClass, type);
        return type;
    }

    private static <T extends Message> StreamCodec<RegistryFriendlyByteBuf, T> codec(Supplier<T> factory) {
        return StreamCodec.of((buf, message) -> message.toBytes(buf), buf -> {
            T message = factory.get();
            message.fromBytes(buf);
            return message;
        });
    }

    public void sendToServer(Message message) {
        PacketDistributor.sendToServer(message);
    }

    public void sendTo(Message message, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, message);
    }
}
