package com.example.weakspot.server;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.MachineBoost;
import com.example.weakspot.common.MachineComboBoost;
import com.example.weakspot.config.WeakSpotConfig;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
            int calls = entry.getValue().nextTickCalls();
            if (!entry.getValue().isActive()) {
                it.remove();
            }
            if (!world.isBlockLoaded(pos)) {
                continue;
            }
            TileEntity te = world.getTileEntity(pos);
            for (int i = 0; i < calls && te instanceof ITickable && !te.isInvalid() && world.getTileEntity(pos) == te; i++) {
                ((ITickable) te).update();
            }
        }
    }

    /** サーバーが止まったら記録を捨てる（シングルプレイで別のワールドに入り直したときに持ち越さない）。 */
    public static void clear() {
        BOOSTS.clear();
    }
}
