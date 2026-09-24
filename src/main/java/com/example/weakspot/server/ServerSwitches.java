package com.example.weakspot.server;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.PlayerSwitches;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * プレイヤーごとの弱点のオン・オフ（クライアントの J キー）。ログインのたびにクライアントから届く。メモリだけ。
 * 届く前はオン。オフのプレイヤーについて、サーバーは右クリック・左クリックの抑止、ヒットの受け付け、ブースト、
 * マークの転送を止める。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class ServerSwitches {

    private static final PlayerSwitches<UUID> SWITCHES = new PlayerSwitches<>();

    private ServerSwitches() {
    }

    public static boolean isEnabled(EntityPlayer player) {
        return SWITCHES.isEnabled(player.getUniqueID());
    }

    /** クライアントからオン・オフが届いた（サーバースレッド）。オフにした瞬間に、出ていたマークを消す。 */
    public static void set(EntityPlayerMP player, boolean enabled) {
        SWITCHES.set(player.getUniqueID(), enabled);
        if (!enabled) {
            MarkerRelay.onMarker(player, null);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SWITCHES.forget(event.player.getUniqueID());
    }
}
