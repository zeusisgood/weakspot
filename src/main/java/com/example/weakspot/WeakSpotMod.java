package com.example.weakspot;

import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.network.HitMessage;
import com.example.weakspot.network.MilestoneMessage;
import com.example.weakspot.network.OtherHitMessage;
import com.example.weakspot.network.SettingsMessage;
import com.example.weakspot.network.StatsMessage;
import com.example.weakspot.network.StatsRequestMessage;
import com.example.weakspot.server.MachineAccelerator;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

// acceptableRemoteVersions を指定しないので、サーバーとクライアントの両方に同じバージョンが必要
@Mod(modid = WeakSpotMod.MODID, name = WeakSpotMod.NAME, version = WeakSpotMod.VERSION,
        guiFactory = "com.example.weakspot.client.WeakSpotGuiFactory")
public class WeakSpotMod {

    public static final String MODID = "weakspot";
    public static final String NAME = "Weak Spot Mining";
    public static final String VERSION = "1.0.5";

    public static SimpleNetworkWrapper network;

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
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        MachineAccelerator.clear();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        WeakSpotConfig.warnIfMisconfigured();
        proxy.init();
    }
}
