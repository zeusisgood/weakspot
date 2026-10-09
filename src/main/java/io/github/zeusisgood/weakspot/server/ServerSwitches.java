package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.KindMask;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * プレイヤーごとの弱点のオン・オフ（クライアントの HOME キー）。ログインのたびにクライアントから届く。メモリだけ。
 * 届く前はオン。オフのプレイヤーについて、サーバーは右クリック・左クリックの抑止、ヒットの受け付け、ブースト、
 * マークの転送を止める。
 */
public final class ServerSwitches {

    /** オフにしたプレイヤー（オフだけを覚えるので、届く前のプレイヤーはオン）。 */
    private static final Set<UUID> OFF = new HashSet<>();
    /** 機械の粒子を見ないプレイヤー（1.6.0。クライアントの machineParticlesVisible。届く前は見る）。 */
    private static final Set<UUID> PARTICLES_OFF = new HashSet<>();

    /** 自分でオフにした種類（1.7.0。KindMask のビット。届く前は 0 = どれもオン）。 */
    private static final Map<UUID, Integer> DISABLED_KINDS = new HashMap<>();

    private ServerSwitches() {
    }

    public static boolean isEnabled(EntityPlayer player) {
        return !OFF.contains(player.getUniqueID());
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
        return !PARTICLES_OFF.contains(player.getUniqueID());
    }

    public static void setParticlesVisible(EntityPlayerMP player, boolean visible) {
        set(PARTICLES_OFF, player, visible);
    }

    /** クライアントからオン・オフが届いた（サーバースレッド）。オフにした瞬間に、出ていたマークを消す。 */
    public static void set(EntityPlayerMP player, boolean enabled) {
        set(OFF, player, enabled);
        if (!enabled) {
            MarkerRelay.onMarker(player, null);
        }
    }

    private static void set(Set<UUID> off, EntityPlayer player, boolean on) {
        if (on) {
            off.remove(player.getUniqueID());
        } else {
            off.add(player.getUniqueID());
        }
    }

    /** ログアウトの後片付け（HitGate から呼ぶ。1.8.9）。 */
    static void forgetOnLogout(EntityPlayer player) {
        OFF.remove(player.getUniqueID());
        PARTICLES_OFF.remove(player.getUniqueID());
        DISABLED_KINDS.remove(player.getUniqueID());
    }
}
