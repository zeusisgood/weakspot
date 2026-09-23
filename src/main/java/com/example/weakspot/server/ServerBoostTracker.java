package com.example.weakspot.server;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.BoostMath;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.network.OtherHitMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.player.PlayerEvent;
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
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class ServerBoostTracker {

    /** 通知の間隔はネットワークの揺らぎで縮むので、この tick 数だけ甘く見る。 */
    private static final int INTERVAL_JITTER_TICKS = 2;

    private static final Map<UUID, Mining> MINING = new HashMap<>();

    private ServerBoostTracker() {
    }

    /** プレイヤーが今破壊しているブロックと、そこで得た追加進捗。 */
    private static final class Mining {
        final BlockPos pos;
        final long startTick;
        double extraTicks;
        long lastHitTick = Long.MIN_VALUE / 2;

        Mining(BlockPos pos, long startTick) {
            this.pos = pos;
            this.startTick = startTick;
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
    }

    /** 他のプレイヤーのヒット音が届く距離（ブロック）。 */
    private static final double OTHERS_SOUND_RANGE = 16;

    /** クライアントからのヒット通知（サーバースレッドで実行される）。 */
    public static void onHit(EntityPlayerMP player, BlockPos pos, int streak) {
        if (player.capabilities.isCreativeMode || player.isSpectator()) {
            return;
        }
        Mining mining = MINING.get(player.getUniqueID());
        if (mining == null || !mining.pos.equals(pos)) {
            return;
        }
        if (player.world.getBlockState(pos).getBlockHardness(player.world, pos) < 0) {
            return;
        }
        long now = player.world.getTotalWorldTime();
        int minInterval = Math.max(0, WeakSpotConfig.minHitIntervalTicks - INTERVAL_JITTER_TICKS);
        if (now - mining.lastHitTick < minInterval) {
            return;
        }
        mining.lastHitTick = now;
        mining.extraTicks += BoostMath.extraTicksPerHit(
                WeakSpotConfig.boostMultiplier, WeakSpotConfig.boostDurationTicks);
        notifyNearbyPlayers(player, pos, streak);
    }

    /** 受け付けたヒットだけを、近くの他のプレイヤーに知らせる（ヒット音を鳴らすため）。 */
    private static void notifyNearbyPlayers(EntityPlayerMP hitter, BlockPos pos, int streak) {
        OtherHitMessage message = new OtherHitMessage(pos, Math.max(1, streak));
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
        if (player.world.isRemote) {
            return;
        }
        Mining mining = MINING.get(player.getUniqueID());
        if (mining == null || mining.extraTicks <= 0 || !mining.pos.equals(event.getPos())) {
            return;
        }
        long elapsed = player.world.getTotalWorldTime() - mining.startTick;
        event.setNewSpeed((float) (event.getNewSpeed() * BoostMath.serverSpeedFactor(mining.extraTicks, elapsed)));
    }

    @SubscribeEvent
    public static void onLogout(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        MINING.remove(event.player.getUniqueID());
    }

    @SubscribeEvent
    public static void onChangeDimension(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent event) {
        MINING.remove(event.player.getUniqueID());
    }
}
