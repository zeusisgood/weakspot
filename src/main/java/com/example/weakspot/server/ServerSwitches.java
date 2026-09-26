package com.example.weakspot.server;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.KindMask;
import com.example.weakspot.common.PlayerSwitches;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Mod;

/**
 * プレイヤーごとの弱点のオン・オフ（クライアントの HOME キー）。ログインのたびにクライアントから届く。メモリだけ。
 * 届く前はオン。オフのプレイヤーについて、サーバーは右クリック・左クリックの抑止、ヒットの受け付け、ブースト、
 * マークの転送を止める。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class ServerSwitches {

    private static final PlayerSwitches<UUID> SWITCHES = new PlayerSwitches<>();
    /** 機械の粒子を見るか（1.6.0。クライアントの machineParticlesVisible。届く前は見る）。 */
    private static final PlayerSwitches<UUID> PARTICLES = new PlayerSwitches<>();

    /** 自分でオフにした種類（1.7.0。KindMask のビット。届く前は 0 = どれもオン）。 */
    private static final Map<UUID, Integer> DISABLED_KINDS = new HashMap<>();

    private ServerSwitches() {
    }

    public static boolean isEnabled(EntityPlayer player) {
        return SWITCHES.isEnabled(player.getUniqueID());
    }

    /** オンで、その種類も自分でオフにしていないか（1.7.0。統計画面の「弱点マーカー」タブ）。 */
    public static boolean isEnabled(EntityPlayer player, HitKind kind) {
        return isEnabled(player) && !KindMask.isDisabled(DISABLED_KINDS.getOrDefault(player.getUniqueID(), 0), kind);
    }

    public static void setDisabledKinds(EntityPlayerMP player, int mask) {
        if (mask == 0) {
            DISABLED_KINDS.remove(player.getUniqueID());
        } else {
            DISABLED_KINDS.put(player.getUniqueID(), mask);
        }
    }

    public static boolean isParticlesVisible(EntityPlayer player) {
        return PARTICLES.isEnabled(player.getUniqueID());
    }

    public static void setParticlesVisible(EntityPlayerMP player, boolean visible) {
        PARTICLES.set(player.getUniqueID(), visible);
    }

    /** クライアントからオン・オフが届いた（サーバースレッド）。オフにした瞬間に、出ていたマークを消す。 */
    public static void set(EntityPlayerMP player, boolean enabled) {
        SWITCHES.set(player.getUniqueID(), enabled);
        if (!enabled) {
            MarkerRelay.onMarker(player, null);
        }
    }

    /** ログアウトの後片付け（HitGate から呼ぶ。1.8.9）。 */
    static void forgetOnLogout(EntityPlayer player) {
        SWITCHES.forget(player.getUniqueID());
        PARTICLES.forget(player.getUniqueID());
        DISABLED_KINDS.remove(player.getUniqueID());
    }
}
