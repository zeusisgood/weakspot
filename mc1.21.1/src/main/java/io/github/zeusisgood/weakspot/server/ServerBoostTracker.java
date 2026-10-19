package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.BoostMath;
import io.github.zeusisgood.weakspot.common.ComboFactor;
import io.github.zeusisgood.weakspot.common.HitBudget;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import io.github.zeusisgood.weakspot.network.OtherHitMessage;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * 論理サーバーの破壊ブースト。
 *
 * バニラのサーバーは「現在の破壊速度 × 経過tick」で破壊完了を判定するため、時間枠の間だけ
 * 倍率を掛けても判定の瞬間に枠外なら効かない。そこで、ヒットごとの追加進捗を貯めておき、
 * そのブロックの破壊速度に BoostMath#serverSpeedFactor を掛けて積算と同じ結果にする。
 *
 * 掘る前のヒットは「予約」としてプレイヤーごとに 1 つだけ持ち、同じブロックを掘り始めた瞬間に確定する。
 * 予約の時点ではコンボ・統計を数えない（掘らずに当て続けてコンボを稼げないように）。
 *
 * ゴーストブロックの対策: クリックのたびに、そのブロックと前に掘っていたブロックの状態を本人に送り直す
 * （バニラは掘り始め（START）には何も返さないので、クライアントだけで壊れたブロックが残る）。また、バニラが保留にした破壊
 * （進捗が足りないまま掘り終わりが届いた。1 枠だけ）にも、別のブロックへ移ったあとまでブーストを効かせる。
 * 長押しをやめたか・保留は、ServerPlayerGameMode のフィールドで見る（accesstransformer.cfg）。
 */
@EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class ServerBoostTracker {

    private static final Map<UUID, Mining> MINING = new HashMap<>();
    private static final Map<UUID, Reserved> RESERVED = new HashMap<>();
    /** ブロックが変わっても引き継ぐ。 */
    private static final Map<UUID, HitBudget> BUDGETS = new HashMap<>();
    /** バニラが保留にした破壊の、前に掘っていた Mining。 */
    private static final Map<UUID, Mining> DELAYED = new HashMap<>();
    /** この tick の終わりに、状態を本人に送り直すブロック。 */
    private static final Map<UUID, Set<BlockPos>> RESEND = new HashMap<>();

    private ServerBoostTracker() {
    }

    private static final class Mining {
        final BlockPos pos;
        final long startTick;
        double extraTicks;
        /** 弱点が出るブロック（壊せて、一瞬では壊れない）だけを、統計の「壊したブロック数」に数える。 */
        boolean eligible;
        /**
        * 未確定のヒット。壊したときに耐久回復の精算に渡す。長押しをやめたり別のブロックに移ったりすると、
        * 次の LeftClickBlock で Mining ごと作り直されるので捨てられる。
        */
        int hits;

        Mining(BlockPos pos, long startTick) {
            this.pos = pos;
            this.startTick = startTick;
        }
    }

    private record Reserved(BlockPos pos, long tick, int streak) {
    }

    /** サーバーでは掘り終わり・やめたときにも来るので、掘り始め（START）だけを見る。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.isCanceled()
                || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }
        BlockPos pos = event.getPos().immutable();
        // 同じブロックを叩き直した場合も、バニラは経過tickを0から数え直すので追加進捗もリセットする。
        // ヒットの間隔（BUDGETS）はプレイヤーごとなので引き継ぐ
        Mining previous = MINING.get(player.getUUID());
        Mining mining = new Mining(pos, player.level().getGameTime());
        MINING.put(player.getUUID(), mining);
        keepDelayed(player, previous, pos);
        queueResend(player, previous, pos);
        // 新しい Mining は追加進捗が0なので、ここで測る破壊速度に自分のブーストは入らない
        BlockState state = player.level().getBlockState(pos);
        mining.eligible = !player.isCreative() && !state.isAir() && state.getDestroySpeed(player.level(), pos) >= 0
                && state.getDestroyProgress(player, player.level(), pos) < 1.0F;
        // 1 tick で壊れるブロックも、バニラはこのイベントのあとに壊すので、確定が間に合う。
        // 当てたブロックは、1 tick で壊れても統計・耐久回復に数える
        Reserved reserved = RESERVED.remove(player.getUUID());
        if (reserved != null && reserved.pos.equals(pos)
                && mining.startTick - reserved.tick <= BoostMath.PRE_DIG_TICKS
                && HitGate.allowed(player, HitKind.MINING)
                && apply(player, mining, reserved.pos, reserved.streak, reserved.tick)) {
            mining.eligible = !player.isCreative() && !state.isAir()
                    && state.getDestroySpeed(player.level(), pos) >= 0;
        }
    }

    /**
    * 他の Mod に取り消された破壊は数えない。このイベントはツールの耐久が減る前（destroyBlock の先頭）に来るので、
    * 耐久が残り 1 のツールでも、回復してから壊れる。
    */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer) || event.isCanceled()) {
            return;
        }
        Mining mining = MINING.get(player.getUUID());
        if (mining == null || !mining.pos.equals(event.getPos())) {
            mining = DELAYED.get(player.getUUID());
            if (mining == null || !mining.pos.equals(event.getPos())) {
                return;
            }
            DELAYED.remove(player.getUUID());
        }
        if (!mining.eligible) {
            return;
        }
        mining.eligible = false;
        int hits = mining.hits;
        ServerStats.record(player, stats -> stats.recordBlockBroken(hits));
        MiningRewards.onBlockBroken(serverPlayer, hits);
    }

    /** ブロック。 */
    private static final double OTHERS_SOUND_RANGE = 16;

    public static void onHit(ServerPlayer player, BlockPos pos, int streak) {
        // クリエイティブはブロックが一瞬で壊れるので、採掘の弱点は出ない
        if (player.isCreative() || !HitGate.allowed(player, HitKind.MINING)) {
            return;
        }
        BlockState state = player.level().getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(player.level(), pos) < 0) {
            return;
        }
        long now = player.level().getGameTime();
        Mining mining = MINING.get(player.getUUID());
        if (mining == null || !mining.pos.equals(pos) || !player.gameMode.isDestroyingBlock) {
            // 新しい予約は古い予約を上書き
            RESERVED.put(player.getUUID(), new Reserved(pos, now, streak));
            return;
        }
        apply(player, mining, pos, streak, now);
    }

    /** 間隔が足りなければ false。tick は受け付けたことにする時刻（予約なら予約した時刻）。 */
    private static boolean apply(ServerPlayer player, Mining mining, BlockPos pos, int streak, long tick) {
        HitBudget budget = BUDGETS.computeIfAbsent(player.getUUID(), id -> new HitBudget());
        if (!HitGate.take(budget, tick, HitKind.MINING)) {
            return false;
        }
        int combo = HitGate.accept(player, HitKind.MINING, pos, streak);
        double factor = SyncedSettings.server().miningComboBonus ? ComboFactor.factor(combo) : 1;
        double extra = BoostMath.extraTicksPerHit(WeakSpotConfig.server.mining.boostMultiplier,
                WeakSpotConfig.server.mining.boostDurationTicks, factor);
        mining.extraTicks += extra;
        mining.hits++;
        ServerStats.record(player, stats -> stats.recordHit(extra));
        MiningRewards.onMiningHit(player);
        return true;
    }

    /** バニラの保留（1 枠）と同じ位置のものだけ残す。 */
    private static void keepDelayed(ServerPlayer player, Mining previous, BlockPos clicked) {
        UUID id = player.getUUID();
        Mining delayed = DELAYED.get(id);
        if (delayed != null && delayed.pos.equals(clicked)) {
            // 保留中のブロックを叩き直した: 新しい Mining で数え直す
            DELAYED.remove(id);
            delayed = null;
        }
        boolean candidate = previous != null && !previous.pos.equals(clicked) && previous.extraTicks > 0
                && !player.level().getBlockState(previous.pos).isAir();
        BlockPos pending = player.gameMode.hasDelayedDestroy ? player.gameMode.delayedDestroyPos : null;
        if (candidate && previous.pos.equals(pending)) {
            DELAYED.put(id, previous);
        } else if (delayed != null && !delayed.pos.equals(pending)) {
            DELAYED.remove(id);
        }
    }

    private static void queueResend(ServerPlayer player, Mining previous, BlockPos clicked) {
        if (player.isCreative() || !HitGate.allowed(player, HitKind.MINING)) {
            return;
        }
        Set<BlockPos> positions = RESEND.computeIfAbsent(player.getUUID(), id -> new LinkedHashSet<>());
        positions.add(clicked);
        if (previous != null) {
            positions.add(previous.pos);
        }
    }

    /** 同じ状態を送るだけなので、掘っている最中に届いても何も変わらない。 */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (RESEND.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        for (Map.Entry<UUID, Set<BlockPos>> entry : RESEND.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                continue;
            }
            for (BlockPos pos : entry.getValue()) {
                if (player.level().isLoaded(pos)) {
                    player.connection.send(new ClientboundBlockUpdatePacket(player.level(), pos));
                }
            }
        }
        RESEND.clear();
    }

    /** ヒット音を鳴らすため。pos は鳴らす位置（動物・近接は当てた生き物、釣りは浮き、弓は引いているプレイヤーの位置）。 */
    static void notifyNearbyPlayers(ServerPlayer hitter, BlockPos pos, int streak) {
        notifyNearbyPlayers(hitter, pos, streak, false);
    }

    /** miss: 的当ての ✕ に当てた（近くの人には低い音）。 */
    static void notifyNearbyPlayers(ServerPlayer hitter, BlockPos pos, int streak, boolean miss) {
        OtherHitMessage message = new OtherHitMessage(pos, Math.max(1, streak), miss);
        double rangeSq = OTHERS_SOUND_RANGE * OTHERS_SOUND_RANGE;
        for (Player other : hitter.level().players()) {
            if (other != hitter && other instanceof ServerPlayer otherPlayer
                    && other.distanceToSqr(Vec3.atCenterOf(pos)) <= rangeSq) {
                WeakSpotMod.network.sendTo(message, otherPlayer);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        BlockPos pos = event.getPosition().orElse(null);
        if (player.level().isClientSide || pos == null || !ServerSwitches.isEnabled(player, HitKind.MINING)) {
            return;
        }
        Mining mining = MINING.get(player.getUUID());
        if (mining == null || !mining.pos.equals(pos)) {
            // バニラの保留も、そのブロックを掘り始めた tick から数える
            mining = DELAYED.get(player.getUUID());
        }
        if (mining == null || mining.extraTicks <= 0 || !mining.pos.equals(pos)) {
            return;
        }
        long elapsed = player.level().getGameTime() - mining.startTick;
        event.setNewSpeed((float) (event.getNewSpeed() * BoostMath.serverSpeedFactor(mining.extraTicks, elapsed)));
    }

    /** コンボを揃えるとき、クライアントはもう数えている 1 つ。 */
    static boolean hasReservation(Player player) {
        Reserved reserved = RESERVED.get(player.getUUID());
        return reserved != null && player.level().getGameTime() - reserved.tick <= BoostMath.PRE_DIG_TICKS;
    }

    static void forget(Player player, HitGate.Leave leave) {
        if (leave != HitGate.Leave.RESPAWN) {
            MINING.remove(player.getUUID());
            RESERVED.remove(player.getUUID());
            DELAYED.remove(player.getUUID());
            RESEND.remove(player.getUUID());
        }
        if (leave == HitGate.Leave.LOGOUT) {
            BUDGETS.remove(player.getUUID());
        }
    }
}
