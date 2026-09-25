package com.example.weakspot.server;

import com.example.weakspot.PlayerRules;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitKind;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * ヒット通知の共通の前置き（1.8.6。論理サーバー）: 受け付けるプレイヤーか、前のヒットからの間隔、ログアウトの後片付け。
 * 種類ごとの条件（弓を引いているか、など）は、各 *Hits に残す。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class HitGate {

    /** 通知の間隔はネットワークの揺らぎで縮むので、この tick 数だけ甘く見る。 */
    static final int INTERVAL_JITTER_TICKS = 2;

    /** 種類ごと・プレイヤーごとの最後に受け付けたヒットの tick（ready / mark を使う種類だけ）。 */
    private static final Map<HitKind, Map<UUID, Long>> LAST_HIT = new EnumMap<>(HitKind.class);

    private HitGate() {
    }

    /** その種類をオンにしていて（一時オフ・種類ごとのオフ）、観戦モードでない。1.8.6 からクリエイティブも受け付ける。 */
    static boolean allowed(EntityPlayerMP player, HitKind kind) {
        return ServerSwitches.isEnabled(player, kind) && PlayerRules.canUse(player);
    }

    /** 前に受け付けたヒットから、設定の間隔（− 2 tick）がたっているか（記録はしない。受け付けたら mark）。 */
    static boolean ready(EntityPlayerMP player, HitKind kind, int minIntervalTicks) {
        Map<UUID, Long> last = LAST_HIT.get(kind);
        Long previous = last == null ? null : last.get(player.getUniqueID());
        return previous == null || intervalOk(player.world.getTotalWorldTime(), previous, minIntervalTicks);
    }

    /** このヒットを受け付けた（次の ready の起点）。 */
    static void mark(EntityPlayerMP player, HitKind kind) {
        LAST_HIT.computeIfAbsent(kind, k -> new HashMap<>()).put(player.getUniqueID(), player.world.getTotalWorldTime());
    }

    /** 自分で最後の tick を持つ種類（採掘・右クリック・釣り・移動）向けの判定。 */
    static boolean intervalOk(long now, long last, int minIntervalTicks) {
        return now - last >= Math.max(0, minIntervalTicks - INTERVAL_JITTER_TICKS);
    }

    /** ログアウトの後片付け。プレイヤーごとの記憶を持つ *Hits は、ここに足す（付け忘れを防ぐため 1 か所にまとめる）。 */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        EntityPlayer player = event.player;
        for (Map<UUID, Long> last : LAST_HIT.values()) {
            last.remove(player.getUniqueID());
        }
        ServerBoostTracker.forget(player);
        RightClickHits.forget(player);
        FishingHits.forget(player);
        MoveHits.forget(player);
        MeleeHits.forget(player);
        ThrowHits.forget(player);
        if (player instanceof EntityPlayerMP) {
            VillagerBreedHints.onLogout((EntityPlayerMP) player);
        }
    }
}
