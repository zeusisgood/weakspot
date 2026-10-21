package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.FallDamage;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.ComboFactor;
import io.github.zeusisgood.weakspot.common.FallMath;
import io.github.zeusisgood.weakspot.common.HitBudget;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.TimedBoostMath;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * はしご・エリトラ・泳ぎの速さは、プレイヤーの動きを決めるクライアントが足すので、サーバーは検証とコンボ・統計・ヒット音だけ。
 * 走りは、移動速度に一時的な修正（保存しない）をかける（自分のクライアントにも届いて効く）。
 * サーバーの位置はクライアントより遅れて届くので、登っている・飛んでいる・走っていることは、直前 RECENT_TICKS の
 * どこかでそうだったかで確かめる。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class MoveHits {

    private static final int RECENT_TICKS = 10;
    /** 合計に (1 + 値) を掛ける。クライアントの視野の抑えも、この ID で見分ける。 */
    public static final UUID SPRINT_MODIFIER = UUID.fromString("5d3c2b1a-7e6f-4a8b-9c0d-1e2f3a4b5c6d");
    private static final SpeedModifier SPRINT_SPEED = new SpeedModifier(SPRINT_MODIFIER, "weakspot sprint boost");

    private static final class State {
        long ladderTick = Long.MIN_VALUE / 2;
        long elytraTick = Long.MIN_VALUE / 2;
        long sprintTick = Long.MIN_VALUE / 2;
        long swimTick = Long.MIN_VALUE / 2;
        long fallTick = Long.MIN_VALUE / 2;
        /** 死ぬ見込みのときに落下の弱点に当てたか（進捗「九死に一生」）。 */
        boolean closeCall;
        /** 負ならなし。 */
        long landedTick = -1;
        final Map<HitKind, HitBudget> budgets = new HashMap<>();
        /** 0 以下なら、かけていない。 */
        int sprintRemaining;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private MoveHits() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide) {
            return;
        }
        long now = player.level().getGameTime();
        State state = STATES.get(player.getUUID());
        boolean ladder = player.onClimbable() && !player.isPassenger();
        boolean elytra = player.isFallFlying();
        boolean sprint = player.isSprinting() && !player.isPassenger() && !elytra;
        boolean swim = player.isInWater() && !player.isPassenger();
        boolean fall = falling(player);
        if (state == null) {
            if (!ladder && !elytra && !sprint && !swim && !fall) {
                return;
            }
            state = new State();
            STATES.put(player.getUUID(), state);
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
        if (state.closeCall && (player.isInWater() || player.isPassenger() || player.isFallFlying())) {
            state.closeCall = false;
        } else if (state.closeCall && player.onGround()) {
            // すぐには与えず、しばらく生きていたら九死に一生（同じ tick の死亡の扱いの差を避ける）
            state.closeCall = false;
            state.landedTick = now;
        }
        if (state.landedTick >= 0 && now - state.landedTick >= CLOSE_CALL_CONFIRM_TICKS) {
            state.landedTick = -1;
            if (player.isAlive() && player.getHealth() > 0 && player instanceof ServerPlayer serverPlayer) {
                WeakSpotAdvancements.grantCloseCall(serverPlayer);
            }
        }
        if (state.sprintRemaining > 0 && --state.sprintRemaining <= 0) {
            SPRINT_SPEED.clear(player);
        }
    }

    private static final int CLOSE_CALL_CONFIRM_TICKS = 10;

    /** 当てて負の「貯め」になっているときも含む。 */
    private static boolean falling(Player player) {
        return !player.onGround() && (player.fallDistance != 0 || player.getY() < player.yOld)
                && !player.isFallFlying() && !player.isInWater() && !player.isInLava() && !player.isPassenger()
                && !player.onClimbable() && !player.getAbilities().flying;
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        State state = STATES.get(event.getEntity().getUUID());
        if (state != null) {
            state.closeCall = false;
            state.landedTick = -1;
        }
    }

    public static void onHit(ServerPlayer player, HitKind kind, int streak) {
        if (!HitGate.allowed(player, kind)) {
            return;
        }
        State state = STATES.get(player.getUUID());
        long now = player.level().getGameTime();
        if (state == null || now - lastSeen(state, kind) > RECENT_TICKS) {
            return;
        }
        if (kind != HitKind.ELYTRA && player.isPassenger()) {
            return;
        }
        if (!HitGate.take(state.budgets.computeIfAbsent(kind, k -> new HitBudget()), now, kind)) {
            return;
        }
        int combo = HitGate.accept(player, kind, player.blockPosition(), streak);
        if (kind == HitKind.SPRINT) {
            SPRINT_SPEED.set(player, TimedBoostMath.multiplier(WeakSpotConfig.server.sprint.sprintBoostMultiplier,
                    WeakSpotConfig.server.sprint.sprintBoostMaxMultiplier, combo));
            state.sprintRemaining = WeakSpotConfig.server.sprint.sprintBoostDurationTicks;
        } else if (kind == HitKind.FALL) {
            if (FallDamage.landingOutlook(player) == FallMath.Outlook.LETHAL) {
                state.closeCall = true;
            }
            // 着地のダメージは、サーバーがこの値から決める。落ち始めに当てた分は負の「貯め」として着地まで残す
            // （0 で止めると、早く当てた分が無駄になり、クライアントの見込みともずれる）
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

    static void forget(Player player, HitGate.Leave leave) {
        if (leave == HitGate.Leave.LOGOUT) {
            STATES.remove(player.getUUID());
            SPRINT_SPEED.clear(player);
        } else if (leave == HitGate.Leave.RESPAWN) {
            State state = STATES.get(player.getUUID());
            if (state != null) {
                state.sprintRemaining = 0;
                state.closeCall = false;
                state.landedTick = -1;
            }
            SPRINT_SPEED.clear(player);
        }
    }

}
