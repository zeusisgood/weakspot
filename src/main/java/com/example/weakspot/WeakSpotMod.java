package com.example.weakspot;

import com.example.weakspot.network.HitMessage;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

// acceptableRemoteVersions を指定しないので、サーバーとクライアントの両方に同じバージョンが必要
@Mod(modid = WeakSpotMod.MODID, name = WeakSpotMod.NAME, version = WeakSpotMod.VERSION)
public class WeakSpotMod {

    public static final String MODID = "weakspot";
    public static final String NAME = "Weak Spot Mining";
    public static final String VERSION = "1.0.0";

    public static SimpleNetworkWrapper network;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        network = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        network.registerMessage(HitMessage.Handler.class, HitMessage.class, 0, Side.SERVER);
    }
}
