package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import io.github.zeusisgood.weakspot.network.Network;
import io.github.zeusisgood.weakspot.server.MachineAccelerator;
import io.github.zeusisgood.weakspot.server.MachineStates;
import io.github.zeusisgood.weakspot.server.VehicleHits;
import io.github.zeusisgood.weakspot.server.WeakSpotCommand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

/** 同じマイナー同士なら、パッチが違っても接続できる（Network.PROTOCOL）。 */
@Mod(WeakSpotMod.MODID)
public final class WeakSpotMod {

    public static final String MODID = "weakspot";
    public static final String NAME = "Weak Spot Mining";
    public static final String VERSION = "1.13.0";

    public static final Network network = new Network();

    /** 更新のお知らせで、初めて入れた人を見分ける。設定ファイルを作るより前（Mod のクラスを作るとき）に確かめる。 */
    public static boolean configExistedAtStart;

    public static CommonProxy proxy;

    public WeakSpotMod(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ModContainer container = context.getContainer();
        configExistedAtStart = FMLPaths.CONFIGDIR.get().resolve(MODID + "-client.toml").toFile().exists();
        proxy = FMLEnvironment.dist.isClient() ? newClientProxy() : new CommonProxy();
        WeakSpotConfig.register(context);
        modBus.addListener(WeakSpotConfig::onLoad);
        modBus.addListener(WeakSpotConfig::onReload);
        network.register();
        Registration.register(modBus);
        MinecraftForge.EVENT_BUS.addListener(WeakSpotMod::onServerStarting);
        MinecraftForge.EVENT_BUS.addListener(WeakSpotMod::onServerStopped);
        MinecraftForge.EVENT_BUS.addListener(WeakSpotMod::onRegisterCommands);
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
