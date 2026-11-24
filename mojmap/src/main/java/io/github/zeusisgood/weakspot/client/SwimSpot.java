package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.TimedBoost;
import io.github.zeusisgood.weakspot.common.TimedBoostMath;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/**
 * 移動速度の値は（水中の移動効率がなければ）水中の移動に効かないので、はしごと同じく、その tick の移動の (倍率 − 1) 倍を
 * クライアントで足す（上下も含む）。サーバーは検証だけ（MoveHits）。
 */
final class SwimSpot extends AimSpotKind {

    /** ブロック。上下も含む。 */
    private static final double MOVING_SPEED = 0.03;
    /** 水中の減速で止まるまでに、およそ距離 ÷ 0.2 進む。 */
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

    /**
     * 泳ぐ姿勢（isSwimming）も、水の中をただ動いているときも含む（泳ぐ姿勢は水の中でしかならない）。
     * 乗り物・はしご・エリトラ・溶岩は、それぞれの弱点に任せる。
     */
    private static boolean swimming(LocalPlayer player) {
        if (!(player.isInWater() || player.isSwimming()) || player.isInLava() || player.isPassenger()
                || player.onClimbable() || player.isFallFlying()) {
            return false;
        }
        double dx = player.getX() - player.xo;
        double dy = player.getY() - player.yo;
        double dz = player.getZ() - player.zo;
        return dx * dx + dy * dy + dz * dz >= MOVING_SPEED * MOVING_SPEED;
    }

    @Override
    boolean wanted(LocalPlayer player, SyncedSettings settings) {
        return swimming(player);
    }

    @Override
    int keepTicks() {
        return MOVING_KEEP_TICKS;
    }

    @Override
    int placement(LocalPlayer player) {
        return HudSpot.VERTICAL;
    }

    @Override
    void onHit(Minecraft mc, LocalPlayer player, SyncedSettings settings, int streak) {
        boost.start(TimedBoostMath.multiplier(settings.swimBoostMultiplier, settings.swimBoostMaxMultiplier, streak),
                settings.swimBoostDurationTicks, ClientWeakSpotHandler.clientTick);
        // 突進は小さく固定（コンボで大きくしない）
        double power = settings.swimDashDistance * DASH_MOTION_PER_BLOCK;
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(player.getDeltaMovement().add(look.x * power, look.y * power, look.z * power));
        for (int i = 0; i < BUBBLES; i++) {
            mc.level.addParticle(ParticleTypes.BUBBLE, player.getX(), player.getY() + 0.5, player.getZ(),
                    player.getRandom().nextGaussian() * 0.1, player.getRandom().nextGaussian() * 0.1,
                    player.getRandom().nextGaussian() * 0.1);
        }
        mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_SPLASH,
                SoundSource.PLAYERS, 0.4F, 1.4F, false);
    }

    /** 1 tick に足す量に上限は付けない（速さの上限は付けない方針）。乗り物に乗ったら、加速の残りを止める（乗っている間に体へ移動を足さないように）。 */
    @Override
    void tickEnd(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player.isPassenger()) {
            boost.clear();
            return;
        }
        if (!boost.isActive(ClientWeakSpotHandler.clientTick) || !KindSwitches.isEnabled(HitKind.SWIM)
                || !swimming(player)) {
            return;
        }
        double extra = TimedBoostMath.extra(boost.multiplier());
        double dx = (player.getX() - player.xo) * extra;
        double dy = (player.getY() - player.yo) * extra;
        double dz = (player.getZ() - player.zo) * extra;
        if (dx * dx + dy * dy + dz * dz > 1e-8) {
            player.move(MoverType.SELF, new Vec3(dx, dy, dz));
        }
    }

    @Override
    boolean hasGauge(Minecraft mc, float partialTicks) {
        return WeakSpotConfig.client.hud.swimBoostBarEnabled && boost.isActive(ClientWeakSpotHandler.clientTick)
                && mc.player.isInWater() && !mc.player.isPassenger();
    }

    @Override
    void drawGauge(Minecraft mc, float partialTicks) {
        HudSpot.gauge(mc, boost.remainingFraction(ClientWeakSpotHandler.clientTick, partialTicks),
                MarkerLook.color(HitKind.SWIM), true);
    }
}
