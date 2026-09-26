package com.example.weakspot.server;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.VehicleBoostMath;
import com.example.weakspot.config.WeakSpotConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * はしご・エリトラ・走りの弱点のヒット通知の検証と効果（論理サーバー。1.7.0）。
 * はしご・エリトラの速さは、プレイヤーの動きを決めるクライアントが足すので、サーバーは検証とコンボ・統計・ヒット音だけ。
 * 走りは、移動速度に一時的な修正（保存しない）をかける（乗り物の馬と同じ。自分のクライアントにも届いて効く）。
 * サーバーの位置はクライアントより遅れて届くので、登っている・飛んでいる・走っていることは、直前 RECENT_TICKS の
 * どこかでそうだったかで確かめる。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class MoveHits {

    /** 登っている・飛んでいる・走っていたことを認める、直前の tick 数。 */
    private static final int RECENT_TICKS = 10;
    /** 走りの加速の修正（固定の UUID。合計に (1 + 値) を掛ける）。クライアントの視野の抑えも、この UUID で見分ける。 */
    public static final UUID SPRINT_MODIFIER = UUID.fromString("5d3c2b1a-7e6f-4a8b-9c0d-1e2f3a4b5c6d");
    private static final SpeedModifier SPRINT_SPEED = new SpeedModifier(SPRINT_MODIFIER, "weakspot sprint boost");

    /** プレイヤーごとの、最後にはしご・エリトラ・走りだった tick と、種類ごとの最後のヒット。 */
    private static final class State {
        long ladderTick = Long.MIN_VALUE / 2;
        long elytraTick = Long.MIN_VALUE / 2;
        long sprintTick = Long.MIN_VALUE / 2;
        final Map<HitKind, Long> lastHit = new HashMap<>();
        /** 走りの加速の残り tick（0 以下なら、かけていない）。 */
        int sprintRemaining;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private MoveHits() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        if (event.phase != TickEvent.Phase.END || player.world.isRemote) {
            return;
        }
        long now = player.world.getTotalWorldTime();
        State state = STATES.get(player.getUniqueID());
        boolean ladder = player.isOnLadder() && !player.isRiding();
        boolean elytra = player.isElytraFlying();
        boolean sprint = player.isSprinting() && !player.isRiding() && !elytra;
        if (state == null) {
            if (!ladder && !elytra && !sprint) {
                return;
            }
            state = new State();
            STATES.put(player.getUniqueID(), state);
        }
        if (ladder) {
            state.ladderTick = now;
        }
        if (elytra) {
            state.elytraTick = now;
        }
        if (sprint) {
            state.sprintTick = now;
        }
        if (state.sprintRemaining > 0 && --state.sprintRemaining <= 0) {
            SPRINT_SPEED.clear(player);
        }
    }

    /** クライアントからのヒット通知（サーバースレッド）。kind は LADDER / ELYTRA / SPRINT。 */
    public static void onHit(EntityPlayerMP player, HitKind kind, int streak) {
        if (!HitGate.allowed(player, kind)
                || !isEnabledOnServer(kind)) {
            return;
        }
        State state = STATES.get(player.getUniqueID());
        long now = player.world.getTotalWorldTime();
        if (state == null || now - lastSeen(state, kind) > RECENT_TICKS) {
            return;
        }
        if (kind != HitKind.ELYTRA && player.isRiding()) {
            return;
        }
        Long last = state.lastHit.get(kind);
        if (last != null && !HitGate.intervalOk(now, last, minHitInterval(kind))) {
            return;
        }
        state.lastHit.put(kind, now);
        int combo = HitGate.accept(player, kind, new BlockPos(player), streak);
        if (kind == HitKind.SPRINT) {
            SPRINT_SPEED.set(player, VehicleBoostMath.multiplier(WeakSpotConfig.sprintBoostMultiplier,
                    WeakSpotConfig.sprintBoostMaxMultiplier, combo));
            state.sprintRemaining = WeakSpotConfig.sprintBoostDurationTicks;
        }
    }

    private static boolean isEnabledOnServer(HitKind kind) {
        switch (kind) {
            case LADDER:
                return WeakSpotConfig.ladderWeakSpotEnabled;
            case ELYTRA:
                return WeakSpotConfig.elytraWeakSpotEnabled;
            case SPRINT:
                return WeakSpotConfig.sprintWeakSpotEnabled;
            default:
                return false;
        }
    }

    private static long lastSeen(State state, HitKind kind) {
        switch (kind) {
            case LADDER:
                return state.ladderTick;
            case ELYTRA:
                return state.elytraTick;
            default:
                return state.sprintTick;
        }
    }

    private static int minHitInterval(HitKind kind) {
        switch (kind) {
            case LADDER:
                return WeakSpotConfig.ladderMinHitIntervalTicks;
            case ELYTRA:
                return WeakSpotConfig.elytraMinHitIntervalTicks;
            default:
                return WeakSpotConfig.sprintMinHitIntervalTicks;
        }
    }

    /** ログアウトの後片付け（HitGate から呼ぶ）。 */
    static void forget(EntityPlayer player) {
        STATES.remove(player.getUniqueID());
        SPRINT_SPEED.clear(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        State state = STATES.get(event.player.getUniqueID());
        if (state != null) {
            state.sprintRemaining = 0;
        }
        SPRINT_SPEED.clear(event.player);
    }
}
