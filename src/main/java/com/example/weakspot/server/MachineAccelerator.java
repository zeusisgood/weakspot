package com.example.weakspot.server;

import com.example.weakspot.RightClickTargets;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.MachineBoost;
import com.example.weakspot.common.MachineComboBoost;
import com.example.weakspot.common.MachineParticles;
import com.example.weakspot.config.WeakSpotConfig;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 機械の加速（Random Things の Time in a Bottle と同じ方式）。ヒットした機械の位置と残り時間をメモリにだけ記録し
 * （エンティティは使わず、セーブデータにも残さない）、そのワールドの tick の最後に update() を余分に呼ぶ。
 *
 * 機械 Mod の多くは update() が1tickに1回だけ呼ばれる前提で作られているので、余分に呼ぶと不具合や例外の原因になりうる。
 * 例外はあえて捕まえない（クラッシュレポートで機械を特定し、excludedBlocks に足してもらう）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class MachineAccelerator {

    /** ディメンションごとの、加速中の機械。 */
    private static final Map<Integer, Map<BlockPos, MachineBoost>> BOOSTS = new HashMap<>();
    /** 粒子を面から外側へ出す距離（ブロック）。 */
    private static final double PARTICLE_OUTSET = 0.05;
    /** 粒子を送るプレイヤーの距離（ブロック。バニラの粒子と同じ）。 */
    private static final double PARTICLE_RANGE = 32;

    private MachineAccelerator() {
    }

    /** サーバーが機械ヒットを受け付けたときに呼ぶ。combo はこのヒットを数えたあとの、そのプレイヤーの連続ヒット数。 */
    static void hit(World world, BlockPos pos, int combo) {
        double multiplier = MachineComboBoost.multiplier(WeakSpotConfig.machineBoostMultiplier,
                WeakSpotConfig.machineBoostMaxMultiplier, combo);
        BOOSTS.computeIfAbsent(world.provider.getDimension(), d -> new HashMap<>())
                .computeIfAbsent(pos, p -> new MachineBoost())
                .hit(multiplier, WeakSpotConfig.machineBoostDurationTicks);
    }

    /** バニラのタイルエンティティの更新が終わった後（ワールドの tick の最後）に、余分に update() を呼ぶ。 */
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
            return;
        }
        Map<BlockPos, MachineBoost> boosts = BOOSTS.get(event.world.provider.getDimension());
        if (boosts == null || boosts.isEmpty()) {
            return;
        }
        World world = event.world;
        for (Iterator<Map.Entry<BlockPos, MachineBoost>> it = boosts.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<BlockPos, MachineBoost> entry = it.next();
            BlockPos pos = entry.getKey();
            int particles = entry.getValue().nextParticles();
            int calls = entry.getValue().nextTickCalls();
            if (!entry.getValue().isActive()) {
                it.remove();
            }
            if (!world.isBlockLoaded(pos)) {
                continue;
            }
            if (WeakSpotConfig.machineBoostParticles && particles > 0) {
                spawnParticles(world, pos, particles, entry.getValue().multiplier());
            }
            if (RightClickTargets.isScheduledMachine(world.getBlockState(pos).getBlock())) {
                ScheduledTicks.boost(world, pos, calls, entry.getValue().multiplier());
                continue;
            }
            TileEntity te = world.getTileEntity(pos);
            for (int i = 0; i < calls && te instanceof ITickable && !te.isInvalid() && world.getTileEntity(pos) == te; i++) {
                ((ITickable) te).update();
            }
        }
    }

    /**
     * 加速中の機械のまわりに、速さに合わせた色の粉の粒子を出す（1.5.3）。数を 0 にすると、オフセットが色として届く
     * （バニラの粉の粒子。近くのプレイヤー全員に見える）。場所は、当たり判定の箱の底面以外の面のランダムな位置の少し外側。
     */
    private static void spawnParticles(World world, BlockPos pos, int count, double multiplier) {
        if (!(world instanceof WorldServer)) {
            return;
        }
        int rgb = MachineParticles.colorFor(
                MachineParticles.comboFactor(multiplier, WeakSpotConfig.machineBoostMultiplier));
        double r = (rgb >> 16 & 0xFF) / 255.0;
        double g = (rgb >> 8 & 0xFF) / 255.0;
        double b = (rgb & 0xFF) / 255.0;
        AxisAlignedBB box = world.getBlockState(pos).getBoundingBox(world, pos).offset(pos);
        Random rand = world.rand;
        for (int i = 0; i < count; i++) {
            double x = box.minX + rand.nextDouble() * (box.maxX - box.minX);
            double y = box.minY + rand.nextDouble() * (box.maxY - box.minY);
            double z = box.minZ + rand.nextDouble() * (box.maxZ - box.minZ);
            // 底面以外の 5 つの面のどれかに寄せて、少し外側に出す
            switch (rand.nextInt(5)) {
                case 0:
                    y = box.maxY + PARTICLE_OUTSET;
                    break;
                case 1:
                    x = box.minX - PARTICLE_OUTSET;
                    break;
                case 2:
                    x = box.maxX + PARTICLE_OUTSET;
                    break;
                case 3:
                    z = box.minZ - PARTICLE_OUTSET;
                    break;
                default:
                    z = box.maxZ + PARTICLE_OUTSET;
                    break;
            }
            // 各自の設定（machineParticlesVisible）で見ない人には送らない（1.6.0）
            for (EntityPlayer viewer : world.playerEntities) {
                if (viewer instanceof EntityPlayerMP && ServerSwitches.isParticlesVisible(viewer)
                        && viewer.getDistanceSq(x, y, z) <= PARTICLE_RANGE * PARTICLE_RANGE) {
                    ((WorldServer) world).spawnParticle((EntityPlayerMP) viewer, EnumParticleTypes.REDSTONE, false,
                            x, y, z, 0, r, g, b, 1.0);
                }
            }
        }
    }

    /** サーバーが止まったら記録を捨てる（シングルプレイで別のワールドに入り直したときに持ち越さない）。 */
    public static void clear() {
        BOOSTS.clear();
        ScheduledTicks.clear();
    }
}
