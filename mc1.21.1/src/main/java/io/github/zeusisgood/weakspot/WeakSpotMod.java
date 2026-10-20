package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import io.github.zeusisgood.weakspot.network.Network;
import io.github.zeusisgood.weakspot.server.MachineAccelerator;
import io.github.zeusisgood.weakspot.server.MachineStates;
import io.github.zeusisgood.weakspot.server.VehicleHits;
import io.github.zeusisgood.weakspot.server.WeakSpotCommand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** 同じマイナー同士なら、パッチが違っても接続できる（Network.PROTOCOL）。 */
@Mod(WeakSpotMod.MODID)
public final class WeakSpotMod {

    public static final String MODID = "weakspot";
    public static final String NAME = "Weak Spot Mining";
    public static final String VERSION = "1.12.3";

    public static final Network network = new Network();

    /** 更新のお知らせで、初めて入れた人を見分ける。設定ファイルを作るより前（Mod のクラスを作るとき）に確かめる。 */
    public static boolean configExistedAtStart;

    public static CommonProxy proxy;

    public WeakSpotMod(IEventBus modBus, ModContainer container) {
        configExistedAtStart = FMLPaths.CONFIGDIR.get().resolve(MODID + "-client.toml").toFile().exists();
        proxy = FMLEnvironment.dist.isClient() ? newClientProxy() : new CommonProxy();
        WeakSpotConfig.register(container);
        modBus.addListener(ModConfigEvent.Loading.class, WeakSpotConfig::onLoad);
        modBus.addListener(ModConfigEvent.Reloading.class, WeakSpotConfig::onLoad);
        modBus.addListener(Network::register);
        Registration.register(modBus);
        NeoForge.EVENT_BUS.addListener(WeakSpotMod::onServerStarting);
        NeoForge.EVENT_BUS.addListener(WeakSpotMod::onServerStopped);
        NeoForge.EVENT_BUS.addListener(WeakSpotMod::onRegisterCommands);
        proxy.init(modBus, container);
    }

    /** 物理サーバーではクライアントのクラスを読まない（名前を文字列で持つ）。 */
    private static CommonProxy newClientProxy() {
        try {
            return (CommonProxy) Class.forName("io.github.zeusisgood.weakspot.client.ClientProxy")
                    .getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void onServerStarting(ServerStartingEvent event) {
        SyncedSettings.invalidateServer();
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        MachineAccelerator.clear();
        MachineStates.clear();
        VehicleHits.clear();
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        WeakSpotCommand.register(event.getDispatcher());
    }
}
