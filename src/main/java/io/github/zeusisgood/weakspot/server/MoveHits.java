package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.FallDamage;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.ComboFactor;
import io.github.zeusisgood.weakspot.common.FallMath;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.TimedBoostMath;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * はしご・エリトラ・走り・泳ぎ（1.10.0）・落下（1.11.0）の弱点のヒット通知の検証と効果（論理サーバー。1.7.0）。
 * 落下は、サーバーの落ちた距離（fallDistance）を減らす。
 * はしご・エリトラ・泳ぎの速さは、プレイヤーの動きを決めるクライアントが足すので、サーバーは検証とコンボ・統計・ヒット音だけ。
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
        long swimTick = Long.MIN_VALUE / 2;
        long fallTick = Long.MIN_VALUE / 2;
        /** この落下で、死ぬ見込みのときに落下の弱点に当てたか（進捗「九死に一生」。1.11.0）。 */
        boolean closeCall;
        /** 死ぬ見込みのあと着地した tick（そのあと生きていたら進捗を与える。負ならなし）。 */
        long landedTick = -1;
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
        boolean swim = player.isInWater() && !player.isRiding();
        boolean fall = falling(player);
        if (state == null) {
            if (!ladder && !elytra && !sprint && !swim && !fall) {
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
        if (swim) {
            state.swimTick = now;
        }
        if (fall) {
            state.fallTick = now;
        }
        if (state.closeCall && (player.isInWater() || player.isRiding() || player.isElytraFlying())) {
            state.closeCall = false;
        } else if (state.closeCall && player.onGround) {
            // 着地した。すぐには与えず、しばらく生きていたら九死に一生（同じ tick の死亡の扱いの差を避ける）
            state.closeCall = false;
            state.landedTick = now;
        }
        if (state.landedTick >= 0 && now - state.landedTick >= CLOSE_CALL_CONFIRM_TICKS) {
            state.landedTick = -1;
            if (player.isEntityAlive() && player.getHealth() > 0 && player instanceof EntityPlayerMP) {
                WeakSpotAdvancements.grantCloseCall((EntityPlayerMP) player);
            }
        }
        if (state.sprintRemaining > 0 && --state.sprintRemaining <= 0) {
            SPRINT_SPEED.clear(player);
        }
    }

    /** 着地のあと、九死に一生を与えるまで生きているのを確かめる tick。 */
    private static final int CLOSE_CALL_CONFIRM_TICKS = 10;

    /**
     * 落ちているか（1.11.0。落下の弱点を受け付ける条件）。下がっている最中か、落下距離が 0 でない（当てて負の「貯め」に
     * なっているときも含む）。サーバーの位置はクライアントより遅れるので、直前 RECENT_TICKS のどこかでそうなら受け付ける。
     */
    private static boolean falling(EntityPlayer player) {
        return !player.onGround && (player.fallDistance != 0 || player.posY < player.lastTickPosY)
                && !player.isElytraFlying() && !player.isInWater() && !player.isInLava() && !player.isRiding()
                && !player.isOnLadder() && !player.capabilities.isFlying;
    }

    /** 死んだら、九死に一生の確かめをやめる。 */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        State state = STATES.get(event.getEntityLiving().getUniqueID());
        if (state != null) {
            state.closeCall = false;
            state.landedTick = -1;
        }
    }

    /** クライアントからのヒット通知（サーバースレッド）。kind は LADDER / ELYTRA / SPRINT / SWIM / FALL。 */
    public static void onHit(EntityPlayerMP player, HitKind kind, int streak) {
        if (!HitGate.allowed(player, kind)) {
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
        if (last != null && !HitGate.intervalOk(now, last, kind)) {
            return;
        }
        state.lastHit.put(kind, now);
        int combo = HitGate.accept(player, kind, new BlockPos(player), streak);
        if (kind == HitKind.SPRINT) {
            SPRINT_SPEED.set(player, TimedBoostMath.multiplier(WeakSpotConfig.server.sprint.sprintBoostMultiplier,
                    WeakSpotConfig.server.sprint.sprintBoostMaxMultiplier, combo));
            state.sprintRemaining = WeakSpotConfig.server.sprint.sprintBoostDurationTicks;
        } else if (kind == HitKind.FALL) {
            if (FallDamage.landingOutlook(player) == FallMath.Outlook.LETHAL) {
                state.closeCall = true;
            }
            // 落ちた距離を減らす（着地のダメージは、サーバーがこの値から決める。1.11.0）。落ち始めに当てた分は
            // 負の「貯め」として着地まで残す（0 で止めると、早く当てた分が無駄になり、クライアントの見込みともずれる）
            player.fallDistance = (float) Math.max(FallDamage.MIN_FALL, player.fallDistance
                    - WeakSpotConfig.server.fall.fallReduceBlocks * ComboFactor.factor(combo));
        }
    }

    private static long lastSeen(State state, HitKind kind) {
        switch (kind) {
            case LADDER:
                return state.ladderTick;
            case ELYTRA:
                return state.elytraTick;
            case SWIM:
                return state.swimTick;
            case FALL:
                return state.fallTick;
            default:
                return state.sprintTick;
        }
    }

    /** ログアウトの後片付け（HitGate から呼ぶ）。 */
    static void forget(EntityPlayer player, HitGate.Leave leave) {
        if (leave == HitGate.Leave.LOGOUT) {
            STATES.remove(player.getUniqueID());
            SPRINT_SPEED.clear(player);
        } else if (leave == HitGate.Leave.RESPAWN) {
            State state = STATES.get(player.getUniqueID());
            if (state != null) {
                state.sprintRemaining = 0;
                state.closeCall = false;
                state.landedTick = -1;
            }
            SPRINT_SPEED.clear(player);
        }
    }

}
