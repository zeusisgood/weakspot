package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.KindMask;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** プレイヤーごとの弱点のオン・オフ（クライアントの HOME キー）。ログインのたびにクライアントから届く。メモリだけで、届く前はオン。 */
public final class ServerSwitches {

    /** オフだけを覚えるので、届く前のプレイヤーはオン。 */
    private static final Set<UUID> OFF = new HashSet<>();
    /** クライアントの machineParticlesVisible。届く前は見る。 */
    private static final Set<UUID> PARTICLES_OFF = new HashSet<>();

    /** KindMask のビット。届く前は 0（どれもオン）。 */
    private static final Map<UUID, Integer> DISABLED_KINDS = new HashMap<>();

    private ServerSwitches() {
    }

    public static boolean isEnabled(Player player) {
        return !OFF.contains(player.getUUID());
    }

    public static boolean isEnabled(Player player, HitKind kind) {
        return isEnabled(player) && !KindMask.isDisabled(DISABLED_KINDS.getOrDefault(player.getUUID(), 0), kind);
    }

    public static void setDisabledKinds(ServerPlayer player, int mask) {
        if (mask == 0) {
            DISABLED_KINDS.remove(player.getUUID());
        } else {
            DISABLED_KINDS.put(player.getUUID(), mask);
        }
    }

    public static boolean isParticlesVisible(Player player) {
        return !PARTICLES_OFF.contains(player.getUUID());
    }

    public static void setParticlesVisible(ServerPlayer player, boolean visible) {
        set(PARTICLES_OFF, player, visible);
    }

    /** サーバースレッドで呼ぶ。オフにした瞬間に、出ていたマークを消す。 */
    public static void set(ServerPlayer player, boolean enabled) {
        set(OFF, player, enabled);
        if (!enabled) {
            MarkerRelay.onMarker(player, null);
        }
    }

    private static void set(Set<UUID> off, Player player, boolean on) {
        if (on) {
            off.remove(player.getUUID());
        } else {
            off.add(player.getUUID());
        }
    }

    static void forgetOnLogout(Player player) {
        OFF.remove(player.getUUID());
        PARTICLES_OFF.remove(player.getUUID());
        DISABLED_KINDS.remove(player.getUUID());
    }
}
