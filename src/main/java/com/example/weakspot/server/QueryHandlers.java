package com.example.weakspot.server;

import com.example.weakspot.common.HitKind;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;

/**
 * QueryMessage の種類ごとの処理の表（1.9.0。HitHandlers と同じ形。論理サーバー）。登録のない種類は無視する。
 * 状態を問い合わせる種類を足すときは、ここに足し、返事は StateMessage で送る（パケットは足さない）。
 */
public final class QueryHandlers {

    interface Handler {
        void onQuery(EntityPlayerMP player, long target);
    }

    private static final Map<HitKind, Handler> HANDLERS = new EnumMap<>(HitKind.class);

    static {
        HANDLERS.put(HitKind.ANIMAL, (player, target) -> AnimalHits.onQuery(player, (int) target));
        HANDLERS.put(HitKind.FISHING, (player, target) -> FishingHits.onQuery(player));
        HANDLERS.put(HitKind.MACHINE, (player, target) -> MachineStates.onQuery(player, BlockPos.fromLong(target)));
    }

    private QueryHandlers() {
    }

    /** サーバーのスレッドで呼ぶ。 */
    public static void onQuery(EntityPlayerMP player, HitKind kind, long target) {
        Handler handler = HANDLERS.get(kind);
        if (handler != null) {
            handler.onQuery(player, target);
        }
    }

    /** 登録のある種類（テスト用）。 */
    public static boolean handles(HitKind kind) {
        return HANDLERS.containsKey(kind);
    }
}
