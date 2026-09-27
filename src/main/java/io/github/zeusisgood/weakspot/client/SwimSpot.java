package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.TimedBoostMath;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.MoverType;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;

/**
 * 泳ぎの弱点（自分だけ。1.10.0）。水の中で動いている間、照準のまわり（10〜20 度）に深い青の弱点を出し、照準を合わせるだけで
 * ヒットにする。当てると、視線の方向に小さく突進し、しばらく泳ぐ速さを上げる。1.12.2 の移動速度の値は水中の移動に効かない
 * ので、はしごと同じく、その tick の移動の (倍率 − 1) 倍をクライアントで足す（上下も含む）。サーバーは検証だけ（MoveHits）。
 */
final class SwimSpot extends AimSpotKind {

    /** 泳いでいるとみなす、1 tick の移動（ブロック。上下も含む）。 */
    private static final double MOVING_SPEED = 0.03;
    /** 突進の距離（ブロック）から足す速さへの換算（水中の減速で止まるまでに、およそ距離 ÷ 0.2 進む）。 */
    private static final double DASH_MOTION_PER_BLOCK = 0.2;
    private static final int BUBBLES = 12;

    private final TimedBoost boost = new TimedBoost();

    SwimSpot() {
        super(HitKind.SWIM, new HudSpot(HitKind.SWIM));
    }

    @Override
    void clear() {
        super.clear();
        boost.clear();
    }

    /** 泳いでいるか（弱点を出す条件と、速さを足す条件）。乗り物・はしご・エリトラ・溶岩は、それぞれの弱点に任せる。 */
    private static boolean swimming(EntityPlayerSP player) {
        if (!player.isInWater() || player.isInLava() || player.isRiding() || player.isOnLadder()
                || player.isElytraFlying()) {
            return false;
        }
        double dx = player.posX - player.prevPosX;
        double dy = player.posY - player.prevPosY;
        double dz = player.posZ - player.prevPosZ;
        return dx * dx + dy * dy + dz * dz >= MOVING_SPEED * MOVING_SPEED;
    }

    @Override
    boolean wanted(EntityPlayerSP player, SyncedSettings settings) {
        return swimming(player);
    }

    @Override
    int placement(EntityPlayerSP player) {
        return HudSpot.FREE;
    }

    @Override
    void onHit(Minecraft mc, EntityPlayerSP player, SyncedSettings settings, int streak) {
        boost.start(TimedBoostMath.multiplier(settings.swimBoostMultiplier, settings.swimBoostMaxMultiplier, streak),
                settings.swimBoostDurationTicks, ClientWeakSpotHandler.clientTick);
        // 突進は小さく固定（コンボで大きくしない）
        double power = settings.swimDashDistance * DASH_MOTION_PER_BLOCK;
        Vec3d look = player.getLookVec();
        player.motionX += look.x * power;
        player.motionY += look.y * power;
        player.motionZ += look.z * power;
        for (int i = 0; i < BUBBLES; i++) {
            mc.world.spawnParticle(EnumParticleTypes.WATER_BUBBLE, player.posX, player.posY + 0.5, player.posZ,
                    player.getRNG().nextGaussian() * 0.1, player.getRNG().nextGaussian() * 0.1,
                    player.getRNG().nextGaussian() * 0.1);
        }
        mc.world.playSound(player.posX, player.posY, player.posZ, SoundEvents.ENTITY_PLAYER_SPLASH,
                SoundCategory.PLAYERS, 0.4F, 1.4F, false);
    }

    /**
     * 加速中は、その tick に泳いだ分の (倍率 − 1) 倍を足す（上下も含む。1 tick に足す量に上限は付けない。ユーザーの方針）。
     * 乗り物に乗ったら、加速の残りを止める（乗っている間に体へ移動を足さないように）。
     */
    @Override
    void tickEnd(Minecraft mc) {
        EntityPlayerSP player = mc.player;
        if (player.isRiding()) {
            boost.clear();
            return;
        }
        if (!boost.isActive(ClientWeakSpotHandler.clientTick) || !KindSwitches.isEnabled(HitKind.SWIM)
                || !swimming(player)) {
            return;
        }
        double extra = TimedBoostMath.extra(boost.multiplier());
        double dx = (player.posX - player.prevPosX) * extra;
        double dy = (player.posY - player.prevPosY) * extra;
        double dz = (player.posZ - player.prevPosZ) * extra;
        if (dx * dx + dy * dy + dz * dz > 1e-8) {
            player.move(MoverType.SELF, dx, dy, dz);
        }
    }

    @Override
    boolean hasGauge(Minecraft mc, float partialTicks) {
        return WeakSpotConfig.swimBoostBarEnabled && boost.isActive(ClientWeakSpotHandler.clientTick)
                && mc.player.isInWater() && !mc.player.isRiding();
    }

    @Override
    void drawGauge(Minecraft mc, float partialTicks) {
        HudSpot.gauge(mc, boost.remaining(ClientWeakSpotHandler.clientTick, partialTicks),
                MarkerLook.color(HitKind.SWIM), true);
    }
}
