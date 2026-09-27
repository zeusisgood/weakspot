package io.github.zeusisgood.weakspot;

import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import io.github.zeusisgood.weakspot.network.HitMessage;
import io.github.zeusisgood.weakspot.network.MarkerMessage;
import io.github.zeusisgood.weakspot.network.MilestoneMessage;
import io.github.zeusisgood.weakspot.network.OtherComboMessage;
import io.github.zeusisgood.weakspot.network.QueryMessage;
import io.github.zeusisgood.weakspot.network.StateMessage;
import io.github.zeusisgood.weakspot.network.OtherHitMessage;
import io.github.zeusisgood.weakspot.network.OtherMarkerMessage;
import io.github.zeusisgood.weakspot.network.SettingsMessage;
import io.github.zeusisgood.weakspot.network.StatsMessage;
import io.github.zeusisgood.weakspot.network.StatsRequestMessage;
import io.github.zeusisgood.weakspot.network.SwitchMessage;
import io.github.zeusisgood.weakspot.server.MachineAccelerator;
import io.github.zeusisgood.weakspot.server.MachineStates;
import io.github.zeusisgood.weakspot.server.VehicleHits;
import io.github.zeusisgood.weakspot.server.WeakSpotCommand;
import java.io.File;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * サーバーとクライアントの両方に必要。同じマイナー同士（1.4.x）なら、パッチが違っても接続できる。
 * 通信内容を変えたらマイナーを上げ、ACCEPTED_VERSIONS も新しいマイナーに書き換える（CLAUDE.md のバージョンの方針）。
 */
@Mod(modid = WeakSpotMod.MODID, name = WeakSpotMod.NAME, version = WeakSpotMod.VERSION,
        acceptableRemoteVersions = WeakSpotMod.ACCEPTED_VERSIONS,
        updateJSON = WeakSpotMod.UPDATE_JSON,
        guiFactory = "io.github.zeusisgood.weakspot.client.WeakSpotGuiFactory")
public class WeakSpotMod {

    public static final String MODID = "weakspot";
    public static final String NAME = "Weak Spot Mining";
    public static final String VERSION = "1.9.4";
    /** 接続できる相手のバージョンの範囲（Maven の書式）。 */
    public static final String ACCEPTED_VERSIONS = "[1.9,1.10)";
    /** Forge の更新確認が読む、最新の版の情報（1.9.2）。リリースのたびに update.json の版を書き換える。 */
    public static final String UPDATE_JSON = "https://raw.githubusercontent.com/zeusisgood/weakspot/main/update.json";

    public static SimpleNetworkWrapper network;

    /**
     * 起動した時点で config/weakspot.cfg がもうあったか（1.7.1。更新のお知らせで、初めて入れた人を見分ける）。
     * Forge が設定ファイルを作るより前（Mod のクラスを作るとき）に確かめる。
     */
    public static boolean configExistedAtStart;

    public WeakSpotMod() {
        configExistedAtStart = new File(Loader.instance().getConfigDir(), MODID + ".cfg").exists();
    }

    @SidedProxy(clientSide = "io.github.zeusisgood.weakspot.client.ClientProxy", serverSide = "io.github.zeusisgood.weakspot.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        network = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        network.registerMessage(HitMessage.Handler.class, HitMessage.class, 0, Side.SERVER);
        network.registerMessage(OtherHitMessage.Handler.class, OtherHitMessage.class, 1, Side.CLIENT);
        network.registerMessage(SettingsMessage.Handler.class, SettingsMessage.class, 2, Side.CLIENT);
        network.registerMessage(StatsRequestMessage.Handler.class, StatsRequestMessage.class, 3, Side.SERVER);
        network.registerMessage(StatsMessage.Handler.class, StatsMessage.class, 4, Side.CLIENT);
        network.registerMessage(MilestoneMessage.Handler.class, MilestoneMessage.class, 5, Side.CLIENT);
        network.registerMessage(MarkerMessage.Handler.class, MarkerMessage.class, 6, Side.SERVER);
        network.registerMessage(OtherMarkerMessage.Handler.class, OtherMarkerMessage.class, 7, Side.CLIENT);
        network.registerMessage(SwitchMessage.Handler.class, SwitchMessage.class, 8, Side.SERVER);
        // 1.9.0 で、動物・釣り・機械の問い合わせと返事（9〜14）を QueryMessage / StateMessage にまとめ、番号を振り直した
        network.registerMessage(QueryMessage.Handler.class, QueryMessage.class, 9, Side.SERVER);
        network.registerMessage(StateMessage.Handler.class, StateMessage.class, 10, Side.CLIENT);
        network.registerMessage(OtherComboMessage.Handler.class, OtherComboMessage.class, 11, Side.CLIENT);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        WeakSpotConfig.migrate();
        SyncedSettings.invalidateServer();
        event.registerServerCommand(new WeakSpotCommand());
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        MachineAccelerator.clear();
        MachineStates.clear();
        VehicleHits.clear();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        WeakSpotConfig.warnIfMisconfigured();
        proxy.init();
    }
}
