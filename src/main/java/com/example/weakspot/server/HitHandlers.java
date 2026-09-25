package com.example.weakspot.server;

import com.example.weakspot.common.HitKind;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;

/**
 * ヒット通知（HitMessage）の種類ごとの処理の表（1.8.6。それまでは if の連なり）。種類を足すときは、ここに 1 行足す。
 * 登録のない種類は無視する。どれもサーバースレッドで呼ぶ。
 */
public final class HitHandlers {

    /** 1 つの種類の処理。pos はブロックの種類（採掘・成長・機械・収穫）、entityId は動物・乗り物で使う。 */
    public interface Handler {
        void onHit(EntityPlayerMP player, HitKind kind, BlockPos pos, int entityId, int streak);
    }

    private static final Map<HitKind, Handler> HANDLERS = new EnumMap<>(HitKind.class);

    static {
        HANDLERS.put(HitKind.MINING, (player, kind, pos, entityId, streak) -> ServerBoostTracker.onHit(player, pos, streak));
        Handler rightClick = (player, kind, pos, entityId, streak) -> RightClickHits.onHit(player, kind, pos, streak);
        HANDLERS.put(HitKind.GROWTH, rightClick);
        HANDLERS.put(HitKind.MACHINE, rightClick);
        HANDLERS.put(HitKind.HARVEST, rightClick);
        HANDLERS.put(HitKind.ANIMAL, (player, kind, pos, entityId, streak) -> AnimalHits.onHit(player, entityId, streak));
        HANDLERS.put(HitKind.FISHING, (player, kind, pos, entityId, streak) -> FishingHits.onHit(player, streak));
        HANDLERS.put(HitKind.BOW, (player, kind, pos, entityId, streak) -> BowHits.onHit(player, streak));
        HANDLERS.put(HitKind.MELEE, (player, kind, pos, entityId, streak) -> MeleeHits.onHit(player, streak));
        HANDLERS.put(HitKind.VEHICLE, (player, kind, pos, entityId, streak) -> VehicleHits.onHit(player, entityId, streak));
        HANDLERS.put(HitKind.EAT, (player, kind, pos, entityId, streak) -> EatHits.onHit(player, streak));
        HANDLERS.put(HitKind.SLEEP, (player, kind, pos, entityId, streak) -> SleepHits.onHit(player, streak));
        Handler move = (player, kind, pos, entityId, streak) -> MoveHits.onHit(player, kind, streak);
        HANDLERS.put(HitKind.LADDER, move);
        HANDLERS.put(HitKind.ELYTRA, move);
        HANDLERS.put(HitKind.SPRINT, move);
        HANDLERS.put(HitKind.ENCHANT, (player, kind, pos, entityId, streak) -> EnchantHits.onHit(player, streak));
        HANDLERS.put(HitKind.THROW, (player, kind, pos, entityId, streak) -> ThrowHits.onHit(player, streak));
        HANDLERS.put(HitKind.PORTAL, (player, kind, pos, entityId, streak) -> PortalHits.onHit(player, streak));
    }

    private HitHandlers() {
    }

    /** その種類に処理が登録されているか（単体テスト用）。 */
    public static boolean has(HitKind kind) {
        return HANDLERS.containsKey(kind);
    }

    public static void onHit(EntityPlayerMP player, HitKind kind, BlockPos pos, int entityId, int streak) {
        Handler handler = HANDLERS.get(kind);
        if (handler != null) {
            handler.onHit(player, kind, pos, entityId, streak);
        }
    }
}
