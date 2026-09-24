package com.example.weakspot.server;

import com.example.weakspot.MeleeTargets;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.config.WeakSpotConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * 近接の弱点（論理サーバー）。クライアントは、照準が弱点に重なった左クリックで、攻撃のパケットより先にヒット通知を送る
 * （どちらもサーバーのスレッドの予定に、届いた順に積まれる）。ここでヒットを確かめて「この敵への次の攻撃は
 * クリティカル」と覚え、続いて届いた攻撃で CriticalHitEvent を許可して、倍率をジャンプ攻撃と同じ 1.5 にする。
 *
 * バニラは CriticalHitEvent の前に攻撃のゲージを 0 に戻す（resetCooldown）ので、ゲージは AttackEntityEvent で確かめる。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class MeleeHits {

    /** ジャンプ攻撃のクリティカルと同じ倍率。 */
    private static final float CRIT_MULTIPLIER = 1.5F;
    /** ヒット通知のあと、攻撃を待つ時間（tick）。 */
    private static final int PENDING_TICKS = 5;
    /** バニラが攻撃を受け付ける距離（NetHandlerPlayServer#processUseEntity。見えていれば 6 ブロック）。 */
    private static final double REACH_SQ = 36.0;
    /** 通知の間隔と攻撃のゲージは、ネットワークの揺らぎでずれるので、この tick 数だけ甘く見る。 */
    private static final int JITTER_TICKS = 2;

    /** プレイヤー → クリティカルにする予定の攻撃（ヒット通知を受け付けたもの）。 */
    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    /** プレイヤー → 今まさにクリティカルにする攻撃の敵（AttackEntityEvent から CriticalHitEvent まで）。 */
    private static final Map<UUID, Entity> CRITS = new HashMap<>();
    private static final Map<UUID, Long> LAST_HIT = new HashMap<>();

    private MeleeHits() {
    }

    private static final class Pending {
        final int entityId;
        final long tick;

        Pending(int entityId, long tick) {
            this.entityId = entityId;
            this.tick = tick;
        }
    }

    /** クライアントからのヒット通知（サーバースレッド）。照準の位置は確かめない（クライアントを信用する）。 */
    public static void onHit(EntityPlayerMP player, int entityId) {
        if (!ServerSwitches.isEnabled(player) || player.capabilities.isCreativeMode || player.isSpectator()
                || !WeakSpotConfig.meleeWeakSpotEnabled) {
            return;
        }
        Entity entity = player.world.getEntityByID(entityId);
        if (entity == null || !MeleeTargets.isTarget(entity) || player.getDistanceSq(entity) >= REACH_SQ) {
            return;
        }
        long now = player.world.getTotalWorldTime();
        Long last = LAST_HIT.get(player.getUniqueID());
        int minInterval = Math.max(0, WeakSpotConfig.meleeMinHitIntervalTicks - JITTER_TICKS);
        if (last != null && now - last < minInterval) {
            return;
        }
        LAST_HIT.put(player.getUniqueID(), now);
        PENDING.put(player.getUniqueID(), new Pending(entityId, now));
        ServerStats.countStreak(player);
    }

    /** 攻撃の始まり（ゲージを 0 に戻す前）。キャンセルされた攻撃には来ない。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAttack(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) {
            return;
        }
        UUID id = player.getUniqueID();
        CRITS.remove(id);
        Pending pending = PENDING.remove(id);
        if (pending == null || pending.entityId != event.getTarget().getEntityId()
                || player.world.getTotalWorldTime() - pending.tick > PENDING_TICKS
                || !ServerSwitches.isEnabled(player) || !MeleeTargets.isTarget(event.getTarget())
                || !MeleeTargets.isCharged(player, 0.5F + JITTER_TICKS)) {
            return;
        }
        CRITS.put(id, event.getTarget());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onCriticalHit(CriticalHitEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) {
            return;
        }
        Entity target = CRITS.remove(player.getUniqueID());
        if (target == null || target != event.getTarget()) {
            return;
        }
        event.setResult(Event.Result.ALLOW);
        event.setDamageModifier(CRIT_MULTIPLIER);
        ServerStats.record(player, stats -> stats.recordCritHit());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.player.getUniqueID();
        PENDING.remove(id);
        CRITS.remove(id);
        LAST_HIT.remove(id);
    }
}
