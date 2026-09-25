package com.example.weakspot;

import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.network.AnimalQueryMessage;
import com.example.weakspot.network.AnimalStateMessage;
import com.example.weakspot.network.FishingQueryMessage;
import com.example.weakspot.network.FishingStateMessage;
import com.example.weakspot.network.HitMessage;
import com.example.weakspot.network.MachineQueryMessage;
import com.example.weakspot.network.MachineStateMessage;
import com.example.weakspot.network.MarkerMessage;
import com.example.weakspot.network.MilestoneMessage;
import com.example.weakspot.network.OtherComboMessage;
import com.example.weakspot.network.OtherHitMessage;
import com.example.weakspot.network.OtherMarkerMessage;
import com.example.weakspot.network.SettingsMessage;
import com.example.weakspot.network.StatsMessage;
import com.example.weakspot.network.StatsRequestMessage;
import com.example.weakspot.network.SwitchMessage;
import com.example.weakspot.server.MachineAccelerator;
import com.example.weakspot.server.MachineStates;
import com.example.weakspot.server.VehicleHits;
import com.example.weakspot.server.WeakSpotCommand;
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
        guiFactory = "com.example.weakspot.client.WeakSpotGuiFactory")
public class WeakSpotMod {

    public static final String MODID = "weakspot";
    public static final String NAME = "Weak Spot Mining";
    public static final String VERSION = "1.7.0";
    /** 接続できる相手のバージョンの範囲（Maven の書式）。 */
    public static final String ACCEPTED_VERSIONS = "[1.7,1.8)";

    public static SimpleNetworkWrapper network;

    /**
     * 起動した時点で config/weakspot.cfg がもうあったか（1.7.1。更新のお知らせで、初めて入れた人を見分ける）。
     * Forge が設定ファイルを作るより前（Mod のクラスを作るとき）に確かめる。
     */
    public static boolean configExistedAtStart;

    public WeakSpotMod() {
        configExistedAtStart = new File(Loader.instance().getConfigDir(), MODID + ".cfg").exists();
    }

    @SidedProxy(clientSide = "com.example.weakspot.client.ClientProxy", serverSide = "com.example.weakspot.CommonProxy")
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
        network.registerMessage(AnimalQueryMessage.Handler.class, AnimalQueryMessage.class, 9, Side.SERVER);
        network.registerMessage(AnimalStateMessage.Handler.class, AnimalStateMessage.class, 10, Side.CLIENT);
        network.registerMessage(FishingQueryMessage.Handler.class, FishingQueryMessage.class, 11, Side.SERVER);
        network.registerMessage(FishingStateMessage.Handler.class, FishingStateMessage.class, 12, Side.CLIENT);
        network.registerMessage(MachineQueryMessage.Handler.class, MachineQueryMessage.class, 13, Side.SERVER);
        network.registerMessage(MachineStateMessage.Handler.class, MachineStateMessage.class, 14, Side.CLIENT);
        network.registerMessage(OtherComboMessage.Handler.class, OtherComboMessage.class, 15, Side.CLIENT);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        WeakSpotConfig.migrate();
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
