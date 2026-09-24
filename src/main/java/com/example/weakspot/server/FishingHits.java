package com.example.weakspot.server;

import com.example.weakspot.Reflect;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.FishingMath;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.network.FishingStateMessage;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 釣りの弱点（論理サーバー）。浮き（EntityFishHook）の待ち時間のタイマーはサーバーだけが持ち、非公開なので、
 * 開発環境の MCP 名と実際の環境の SRG 名を順に試して読み書きする（Reflect）。読めなければ釣りの弱点は出ない。
 *
 * バニラの catchingFish は、待ち時間（ticksCaughtDelay）→ 魚が寄ってくる段階（ticksCatchableDelay）→ 食いついた段階
 * （ticksCatchable）の順に進む。待ち時間の段階は「ticksCaughtDelay が正で、あとの2つが 0」のとき。
 * 待ち時間が始まったときの長さは、進み具合のバーのために、毎 tick 見て浮きごとに覚える（メモリだけ）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class FishingHits {

    /** 待ち時間の残り。 */
    private static final Field WAIT = Reflect.field(EntityFishHook.class, "the fishing weak spot",
            "ticksCaughtDelay", "field_146040_ay");
    /** 魚が寄ってくる段階の残り。 */
    private static final Field APPROACH = Reflect.field(EntityFishHook.class, "the fishing weak spot",
            "ticksCatchableDelay", "field_146038_az");
    /** 食いついた段階の残り。 */
    private static final Field BITE = Reflect.field(EntityFishHook.class, "the fishing weak spot",
            "ticksCatchable", "field_146045_ax");
    /** 浮きの状態（BOBBING = 水に浮いている）。 */
    private static final Field STATE = Reflect.field(EntityFishHook.class, "the fishing weak spot",
            "currentState", "field_190627_av");

    /** 通知の間隔はネットワークの揺らぎで縮むので、この tick 数だけ甘く見る。 */
    private static final int INTERVAL_JITTER_TICKS = 2;
    /** 釣りのヒットを受け付けた直後の、左クリックを止める時間（tick）。 */
    private static final int SUPPRESS_TICKS = 3;

    private static final Map<UUID, Track> TRACKS = new HashMap<>();

    private FishingHits() {
    }

    /** プレイヤーの浮きごとの、待ち時間の始まりの長さと、最後のヒット。 */
    private static final class Track {
        EntityFishHook hook;
        int initialWait;
        int lastWait;
        long lastHitTick = Long.MIN_VALUE / 2;
        long lastAcceptedTick = Long.MIN_VALUE / 2;
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

    /** 浮きが水に浮いていて、魚が寄ってくるのを待っている段階（弱点が出る段階）か。 */
    static boolean isWaiting(EntityFishHook hook) {
        if (!available() || hook.isDead || !hook.isInWater()) {
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

    /** そのプレイヤーの、弱点を出せる浮き（自分の浮きで、釣り竿を持っている）。なければ null。 */
    private static EntityFishHook hookOf(EntityPlayer player) {
        EntityFishHook hook = player.fishEntity;
        if (hook == null || hook.isDead || hook.getAngler() != player) {
            return null;
        }
        boolean holdsRod = player.getHeldItemMainhand().getItem() instanceof ItemFishingRod
                || player.getHeldItemOffhand().getItem() instanceof ItemFishingRod;
        return holdsRod ? hook : null;
    }

    /** 毎 tick、待ち時間の段階の浮きの、待ち時間の始まりの長さを覚える（新しい待ち時間が始まると、長さが増える）。 */
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

    /** クライアントからの状態の問い合わせ（サーバースレッド）。待ち時間の段階か、と進み具合を返す。 */
    public static void onQuery(EntityPlayerMP player) {
        EntityFishHook hook = hookOf(player);
        boolean waiting = hook != null && isWaiting(hook) && enabled(SyncedSettings.fromConfig());
        float progress = 0;
        if (waiting) {
            Track track = TRACKS.get(player.getUniqueID());
            if (track != null && track.hook == hook) {
                progress = (float) FishingMath.progress(track.initialWait, read(WAIT, hook));
            }
        }
        WeakSpotMod.network.sendTo(new FishingStateMessage(waiting, progress), player);
    }

    private static boolean enabled(SyncedSettings settings) {
        return settings.fishingWeakSpotEnabled && settings.fishingHits > 0 && available();
    }

    /** クライアントからのヒット通知（サーバースレッド）。照準の角度は確かめない（クライアントを信用する）。 */
    public static void onHit(EntityPlayerMP player, int streak) {
        if (!ServerSwitches.isEnabled(player) || player.capabilities.isCreativeMode || player.isSpectator()) {
            return;
        }
        SyncedSettings settings = SyncedSettings.fromConfig();
        EntityFishHook hook = hookOf(player);
        if (hook == null || !enabled(settings) || !isWaiting(hook)) {
            return;
        }
        Track track = TRACKS.computeIfAbsent(player.getUniqueID(), id -> new Track());
        long now = player.world.getTotalWorldTime();
        int minInterval = Math.max(0, WeakSpotConfig.fishingMinHitIntervalTicks - INTERVAL_JITTER_TICKS);
        if (now - track.lastHitTick < minInterval) {
            return;
        }
        track.lastHitTick = now;
        track.lastAcceptedTick = now;
        try {
            int wait = read(WAIT, hook);
            WAIT.setInt(hook, FishingMath.waitAfterHit(wait, FishingMath.ticksPerHit(settings.fishingHits)));
        } catch (IllegalAccessException e) {
            return;
        }
        ServerStats.record(player, stats -> stats.recordFishingHit());
        ServerStats.countStreak(player);
        ServerBoostTracker.notifyNearbyPlayers(player, new BlockPos(hook), streak);
    }

    /**
     * 釣りの弱点を叩いた直後の、左クリックのバニラの動作（ブロックを掘る、動物を攻撃する）を止める。
     * クライアントは、弱点に重なっている左クリックを最初から送らない（MouseEvent で止める）ので、これは念のため。
     * 弱点をオフにしているプレイヤーは、ヒットが受け付けられないので、止まらない。
     */
    private static boolean justHit(EntityPlayer player) {
        Track track = TRACKS.get(player.getUniqueID());
        return track != null && !player.world.isRemote
                && player.world.getTotalWorldTime() - track.lastAcceptedTick <= SUPPRESS_TICKS;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (justHit(event.getEntityPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (justHit(event.getEntityPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        TRACKS.remove(event.player.getUniqueID());
    }
}
