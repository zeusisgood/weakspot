package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.FishingWater;
import io.github.zeusisgood.weakspot.Reflect;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.FishingMath;
import io.github.zeusisgood.weakspot.common.HitBudget;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import io.github.zeusisgood.weakspot.network.StateMessage;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 浮きの待ち時間のタイマーはサーバーだけが持ち、非公開なので Reflect で読み書きする。読めなければ釣りの弱点は出ない。
 * バニラの catchingFish は、待ち時間（ticksCaughtDelay）→ 魚が寄ってくる段階（ticksCatchableDelay）→ 食いついた段階
 * （ticksCatchable）の順に進む。待ち時間が始まったときの長さは、進み具合のバーのために、浮きごとに覚える（メモリだけ）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class FishingHits {

    private static final Field WAIT = Reflect.field(EntityFishHook.class, "the fishing weak spot",
            "ticksCaughtDelay", "field_146040_ay");
    private static final Field APPROACH = Reflect.field(EntityFishHook.class, "the fishing weak spot",
            "ticksCatchableDelay", "field_146038_az");
    private static final Field BITE = Reflect.field(EntityFishHook.class, "the fishing weak spot",
            "ticksCatchable", "field_146045_ax");
    private static final Field STATE = Reflect.field(EntityFishHook.class, "the fishing weak spot",
            "currentState", "field_190627_av");

    private static final Map<UUID, Track> TRACKS = new HashMap<>();

    private FishingHits() {
    }

    private static final class Track {
        EntityFishHook hook;
        int initialWait;
        int lastWait;
        final HitBudget budget = new HitBudget();
    }

    private static boolean available() {
        return WAIT != null && APPROACH != null && BITE != null;
    }

    private static int read(Field field, EntityFishHook hook) {
        try {
            return field.getInt(hook);
        } catch (IllegalAccessException e) {
            return 0;
        }
    }

    /** 弱点が出る段階。 */
    static boolean isWaiting(EntityFishHook hook) {
        return waitingPhase(hook) && FishingWater.isFloating(hook);
    }

    /** 浮きの位置が水かは見ない。 */
    private static boolean waitingPhase(EntityFishHook hook) {
        if (!available() || hook.isDead) {
            return false;
        }
        if (STATE != null) {
            try {
                Object state = STATE.get(hook);
                if (!(state instanceof Enum) || !((Enum<?>) state).name().equals("BOBBING")) {
                    return false;
                }
            } catch (IllegalAccessException e) {
                return false;
            }
        }
        return read(WAIT, hook) > 0 && read(APPROACH, hook) == 0 && read(BITE, hook) == 0;
    }

    /** なければ null。 */
    private static EntityFishHook hookOf(EntityPlayer player) {
        EntityFishHook hook = player.fishEntity;
        if (hook == null || hook.isDead || hook.getAngler() != player) {
            return null;
        }
        boolean holdsRod = player.getHeldItemMainhand().getItem() instanceof ItemFishingRod
                || player.getHeldItemOffhand().getItem() instanceof ItemFishingRod;
        return holdsRod ? hook : null;
    }

    /** 新しい待ち時間が始まると、長さが増える。 */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote || !available()) {
            return;
        }
        EntityFishHook hook = event.player.fishEntity;
        if (hook == null || hook.isDead) {
            TRACKS.remove(event.player.getUniqueID());
            return;
        }
        Track track = TRACKS.computeIfAbsent(event.player.getUniqueID(), id -> new Track());
        if (track.hook != hook) {
            track.hook = hook;
            track.initialWait = 0;
            track.lastWait = 0;
        }
        if (isWaiting(hook)) {
            int wait = read(WAIT, hook);
            if (track.initialWait == 0 || wait > track.lastWait) {
                track.initialWait = wait;
            }
            track.lastWait = wait;
        } else {
            track.initialWait = 0;
            track.lastWait = 0;
        }
    }

    /** サーバースレッドで呼ぶ。 */
    public static void onQuery(EntityPlayerMP player) {
        EntityFishHook hook = hookOf(player);
        boolean waiting = hook != null && isWaiting(hook) && enabled(SyncedSettings.server());
        float progress = 0;
        if (waiting) {
            Track track = TRACKS.get(player.getUniqueID());
            if (track != null && track.hook == hook) {
                progress = (float) FishingMath.progress(track.initialWait, read(WAIT, hook));
            }
        }
        WeakSpotMod.network.sendTo(StateMessage.fishing(waiting, progress), player);
    }

    private static boolean enabled(SyncedSettings settings) {
        return settings.enabled(HitKind.FISHING) && settings.fishingHits > 0 && available();
    }

    /** 照準の角度は確かめない（クライアントを信用する）。 */
    public static void onHit(EntityPlayerMP player, int streak) {
        if (!HitGate.allowed(player, HitKind.FISHING)) {
            return;
        }
        SyncedSettings settings = SyncedSettings.server();
        EntityFishHook hook = hookOf(player);
        if (hook == null || !enabled(settings) || !isWaiting(hook)) {
            return;
        }
        Track track = TRACKS.computeIfAbsent(player.getUniqueID(), id -> new Track());
        long now = player.world.getTotalWorldTime();
        if (!HitGate.take(track.budget, now, HitKind.FISHING)) {
            return;
        }
        try {
            int wait = read(WAIT, hook);
            WAIT.setInt(hook, FishingMath.waitAfterHit(wait, FishingMath.ticksPerHit(settings.fishingHits)));
        } catch (IllegalAccessException e) {
            return;
        }
        HitGate.accept(player, HitKind.FISHING, new BlockPos(hook), streak);
    }

    static void forget(EntityPlayer player, HitGate.Leave leave) {
        if (leave == HitGate.Leave.LOGOUT) {
            TRACKS.remove(player.getUniqueID());
        }
    }
}
