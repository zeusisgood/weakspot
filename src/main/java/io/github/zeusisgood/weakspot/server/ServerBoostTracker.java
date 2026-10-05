package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.Reflect;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.BoostMath;
import io.github.zeusisgood.weakspot.common.ComboFactor;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import io.github.zeusisgood.weakspot.network.OtherHitMessage;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * サーバー側（論理サーバー）の破壊ブースト。
 *
 * バニラのサーバーは「現在の破壊速度 × 経過tick」で破壊完了を判定するため、時間枠の間だけ
 * 倍率を掛けても判定の瞬間に枠外なら効かない。そこで、ヒットごとの追加進捗を貯めておき、
 * そのブロックの破壊速度に BoostMath#serverSpeedFactor を掛けて積算と同じ結果にする。
 *
 * 掘る前のヒット（1.11.2）: 掘っていないときに届いたヒットは「予約」としてプレイヤーごとに 1 つだけ持ち、
 * 同じブロックを掘り始めた瞬間（LeftClickBlock）に普通のヒットとして確定する。予約の時点ではコンボ・統計を数えない
 * （掘らずに当て続けてコンボを稼げないように）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class ServerBoostTracker {


    private static final Map<UUID, Mining> MINING = new HashMap<>();
    /** 掘る前のヒットの予約（1.11.2）。 */
    private static final Map<UUID, Reserved> RESERVED = new HashMap<>();
    /**
     * PlayerInteractionManager の非公開の isDestroyingBlock（1.11.2）。長押しをやめた（ABORT）には Forge のイベントがないので、
     * 今掘っているかはこれで見る。読めなければ、掘っているブロックと違う位置のヒットだけを予約にする。
     */
    private static final Field DESTROYING = Reflect.field(PlayerInteractionManager.class,
            "pre-dig mining hits on a re-clicked block", "isDestroyingBlock", "field_73088_d");

    private ServerBoostTracker() {
    }

    /** プレイヤーが今破壊しているブロックと、そこで得た追加進捗。 */
    private static final class Mining {
        final BlockPos pos;
        final long startTick;
        double extraTicks;
        long lastHitTick = Long.MIN_VALUE / 2;
        /** 弱点が出るブロック（壊せて、一瞬では壊れない）か。統計の「壊したブロック数」に数えるかに使う。 */
        boolean eligible;
        /**
         * このブロックで受け付けたヒットの数。壊したときに耐久回復の精算に渡す（未確定のヒット）。
         * 長押しをやめたり別のブロックに移ったりすると、次の LeftClickBlock で Mining ごと作り直されるので捨てられる。
         */
        int hits;

        Mining(BlockPos pos, long startTick) {
            this.pos = pos;
            this.startTick = startTick;
        }
    }

    /** 掘る前のヒット（1.11.2）。 */
    private static final class Reserved {
        final BlockPos pos;
        final long tick;
        final int streak;

        Reserved(BlockPos pos, long tick, int streak) {
            this.pos = pos;
            this.tick = tick;
            this.streak = streak;
        }
    }

    /** バニラの破壊開始 (PlayerInteractionManager#onBlockClicked) で呼ばれる。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote || event.isCanceled()) {
            return;
        }
        // 同じブロックを叩き直した場合も、バニラは経過tickを0から数え直すので追加進捗もリセットする。
        // ヒット間隔の制限だけは引き継ぐ。
        Mining previous = MINING.get(player.getUniqueID());
        Mining mining = new Mining(event.getPos(), player.world.getTotalWorldTime());
        if (previous != null) {
            mining.lastHitTick = previous.lastHitTick;
        }
        MINING.put(player.getUniqueID(), mining);
        // 新しい Mining は追加進捗が0なので、ここで測る破壊速度に自分のブーストは入らない
        IBlockState state = player.world.getBlockState(event.getPos());
        mining.eligible = !player.capabilities.isCreativeMode
                && !state.getBlock().isAir(state, player.world, event.getPos())
                && state.getBlockHardness(player.world, event.getPos()) >= 0
                && state.getPlayerRelativeBlockHardness(player, player.world, event.getPos()) < 1.0F;
        // 掘る前のヒットを確定する（1.11.2）。1 tick で壊れるブロックも、バニラはこのイベントのあとに壊すので間に合う。
        // 当てたブロックは、1 tick で壊れても統計・耐久回復に数える
        Reserved reserved = RESERVED.remove(player.getUniqueID());
        if (reserved != null && reserved.pos.equals(event.getPos())
                && mining.startTick - reserved.tick <= BoostMath.PRE_DIG_TICKS && player instanceof EntityPlayerMP
                && HitGate.allowed((EntityPlayerMP) player, HitKind.MINING)
                && apply((EntityPlayerMP) player, mining, reserved.pos, reserved.streak, reserved.tick)) {
            mining.eligible = !player.capabilities.isCreativeMode
                    && !state.getBlock().isAir(state, player.world, event.getPos())
                    && state.getBlockHardness(player.world, event.getPos()) >= 0;
        }
    }

    /**
     * 統計の「壊したブロック数」と、耐久回復の精算。他の Mod に取り消された破壊は数えない。
     * このイベントはツールの耐久が減る前（PlayerInteractionManager#tryHarvestBlock の先頭）に来るので、
     * 耐久が残り1のツールでも、回復してから壊れる。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        EntityPlayer player = event.getPlayer();
        if (player == null || player.world.isRemote || event.isCanceled()) {
            return;
        }
        Mining mining = MINING.get(player.getUniqueID());
        if (mining == null || !mining.eligible || !mining.pos.equals(event.getPos())) {
            return;
        }
        mining.eligible = false;
        int hits = mining.hits;
        ServerStats.record(player, stats -> stats.recordBlockBroken(hits));
        if (player instanceof EntityPlayerMP) {
            MiningRewards.onBlockBroken((EntityPlayerMP) player, hits);
        }
    }

    /** 他のプレイヤーのヒット音が届く距離（ブロック）。 */
    private static final double OTHERS_SOUND_RANGE = 16;

    /** クライアントからのヒット通知（サーバースレッドで実行される）。 */
    public static void onHit(EntityPlayerMP player, BlockPos pos, int streak) {
        // クリエイティブはブロックが一瞬で壊れるので、採掘の弱点は出ない
        if (player.capabilities.isCreativeMode || !HitGate.allowed(player, HitKind.MINING)) {
            return;
        }
        if (player.world.isAirBlock(pos) || player.world.getBlockState(pos).getBlockHardness(player.world, pos) < 0) {
            return;
        }
        long now = player.world.getTotalWorldTime();
        Mining mining = MINING.get(player.getUniqueID());
        if (mining == null || !mining.pos.equals(pos) || !isDestroying(player)) {
            // 掘っていないときのヒットは、掘る前のヒットとして予約する（1.11.2。新しい予約は古い予約を上書き）
            RESERVED.put(player.getUniqueID(), new Reserved(pos, now, streak));
            return;
        }
        apply(player, mining, pos, streak, now);
    }

    /** ヒットを受け付ける（間隔が足りなければ false）。tick は受け付けたことにする時刻（予約なら予約した時刻）。 */
    private static boolean apply(EntityPlayerMP player, Mining mining, BlockPos pos, int streak, long tick) {
        if (!HitGate.intervalOk(tick, mining.lastHitTick, HitKind.MINING)) {
            return false;
        }
        mining.lastHitTick = tick;
        // 1 回のヒットで進む量に、コンボの掛け数を掛ける（1.8.7）
        int combo = HitGate.accept(player, HitKind.MINING, pos, streak);
        double factor = SyncedSettings.server().miningComboBonus ? ComboFactor.factor(combo) : 1;
        double extra = BoostMath.extraTicksPerHit(WeakSpotConfig.server.mining.boostMultiplier, WeakSpotConfig.server.mining.boostDurationTicks,
                factor);
        mining.extraTicks += extra;
        mining.hits++;
        ServerStats.record(player, stats -> stats.recordHit(extra));
        MiningRewards.onMiningHit(player);
        return true;
    }

    /** 今ブロックを掘っている（長押し中）か。読めなければ true（今までどおり、掘っているブロックへのヒットとして扱う）。 */
    private static boolean isDestroying(EntityPlayerMP player) {
        if (DESTROYING == null) {
            return true;
        }
        try {
            return DESTROYING.getBoolean(player.interactionManager);
        } catch (IllegalAccessException e) {
            return true;
        }
    }

    /**
     * 受け付けたヒットだけを、近くの他のプレイヤーに知らせる（ヒット音を鳴らすため。すべての種類）。
     * pos は鳴らす位置（ブロック。動物・近接は当てた生き物、釣りは浮き、弓は引いているプレイヤーの位置）。
     */
    static void notifyNearbyPlayers(EntityPlayerMP hitter, BlockPos pos, int streak) {
        notifyNearbyPlayers(hitter, pos, streak, false);
    }

    /** miss: 的当て（1.11.0）の ✕ に当てた（近くの人には低い音）。 */
    static void notifyNearbyPlayers(EntityPlayerMP hitter, BlockPos pos, int streak, boolean miss) {
        OtherHitMessage message = new OtherHitMessage(pos, Math.max(1, streak), miss);
        double rangeSq = OTHERS_SOUND_RANGE * OTHERS_SOUND_RANGE;
        for (EntityPlayer other : hitter.world.playerEntities) {
            if (other != hitter && other instanceof EntityPlayerMP
                    && other.getDistanceSqToCenter(pos) <= rangeSq) {
                WeakSpotMod.network.sendTo(message, (EntityPlayerMP) other);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote || !ServerSwitches.isEnabled(player, HitKind.MINING)) {
            return;
        }
        Mining mining = MINING.get(player.getUniqueID());
        if (mining == null || mining.extraTicks <= 0 || !mining.pos.equals(event.getPos())) {
            return;
        }
        long elapsed = player.world.getTotalWorldTime() - mining.startTick;
        event.setNewSpeed((float) (event.getNewSpeed() * BoostMath.serverSpeedFactor(mining.extraTicks, elapsed)));
    }

    /** ログアウトの後片付け（HitGate から呼ぶ）。 */
    static void forget(EntityPlayer player, HitGate.Leave leave) {
        if (leave != HitGate.Leave.RESPAWN) {
            MINING.remove(player.getUniqueID());
            RESERVED.remove(player.getUniqueID());
        }
    }

}
