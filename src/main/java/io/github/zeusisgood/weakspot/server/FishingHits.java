package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.Reflect;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.FishingMath;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import io.github.zeusisgood.weakspot.network.StateMessage;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
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

    private static final Map<UUID, Track> TRACKS = new HashMap<>();

    private FishingHits() {
    }

    /** プレイヤーの浮きごとの、待ち時間の始まりの長さと、最後のヒット。 */
    private static final class Track {
        EntityFishHook hook;
        int initialWait;
        int lastWait;
        long lastHitTick = Long.MIN_VALUE / 2;
        /** 浮いている状態なのに位置が水の外、が続いている tick 数（1.10.2）。 */
        int outOfWaterTicks;
        /** この浮きで、水にないことを知らせたか。 */
        boolean noticed;
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
        return waitingPhase(hook) && inWaterBlock(hook);
    }

    /**
     * 浮きのいる位置のブロックが水か（1.10.2）。バニラの EntityFishHook#onUpdate は、浮いている状態（BOBBING）でも、
     * この条件（水で、液体の高さ &gt; 0）のときしか待ち時間を減らさない（catchingFish）。水面ぎりぎりやブロックの縁に
     * 引っかかると、浮いている状態のまま位置が水の外になり、魚が来なくなる。
     */
    static boolean inWaterBlock(EntityFishHook hook) {
        BlockPos pos = new BlockPos(hook);
        IBlockState state = hook.world.getBlockState(pos);
        return state.getMaterial() == Material.WATER && BlockLiquid.getBlockLiquidHeight(state, hook.world, pos) > 0;
    }

    /** 浮いている状態で、魚を待つ段階の値になっているか（浮きの位置が水かは見ない）。 */
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
            track.outOfWaterTicks = 0;
            track.noticed = false;
        }
        if (event.player instanceof EntityPlayerMP) {
            noticeIfStuck((EntityPlayerMP) event.player, hook, track);
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

    /** 浮いている状態なのに位置が水の外、が続いたときに、この投げにつき 1 回だけ本人のチャットで知らせるまでの tick 数。 */
    private static final int STUCK_NOTICE_TICKS = 40;

    /** 翻訳キー weakspot.fishing.* を足した版。 */
    static final String NOTICE_SINCE = "1.10.2";

    /**
     * 浮いている状態で魚を待つ段階なのに、浮きの位置が水の外、が 2 秒続いたら、この投げにつき 1 回だけ知らせる（1.10.2）。
     * 弱点を出していない（釣り竿を持っていない・釣りの弱点がオフ・自分の弱点がオフ）ときは知らせない。
     */
    private static void noticeIfStuck(EntityPlayerMP player, EntityFishHook hook, Track track) {
        if (!waitingPhase(hook) || inWaterBlock(hook)) {
            track.outOfWaterTicks = 0;
            return;
        }
        if (track.noticed || ++track.outOfWaterTicks < STUCK_NOTICE_TICKS) {
            return;
        }
        track.noticed = true;
        if (hookOf(player) != hook || !enabled(SyncedSettings.server()) || !ServerSwitches.isEnabled(player, HitKind.FISHING)) {
            return;
        }
        ITextComponent text = PlayerText.of(player, NOTICE_SINCE, "weakspot.fishing.notInWater");
        text.getStyle().setColor(TextFormatting.YELLOW);
        player.sendMessage(text);
    }

    /** クライアントからの状態の問い合わせ（サーバースレッド）。待ち時間の段階か、と進み具合を返す。 */
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

    /** クライアントからのヒット通知（サーバースレッド）。照準の角度は確かめない（クライアントを信用する）。 */
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
        if (!HitGate.intervalOk(now, track.lastHitTick, HitKind.FISHING)) {
            return;
        }
        track.lastHitTick = now;
        try {
            int wait = read(WAIT, hook);
            WAIT.setInt(hook, FishingMath.waitAfterHit(wait, FishingMath.ticksPerHit(settings.fishingHits)));
        } catch (IllegalAccessException e) {
            return;
        }
        HitGate.accept(player, HitKind.FISHING, new BlockPos(hook), streak);
    }

    /** ログアウトの後片付け（HitGate から呼ぶ）。 */
    static void forget(EntityPlayer player, HitGate.Leave leave) {
        if (leave == HitGate.Leave.LOGOUT) {
            TRACKS.remove(player.getUniqueID());
        }
    }
}
