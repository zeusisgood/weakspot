package com.example.weakspot.server;

import com.example.weakspot.common.HitKind;
import com.example.weakspot.Reflect;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.MachineProgress;
import com.example.weakspot.network.StateMessage;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 機械の進み具合のバーの問い合わせに答える（1.6.0）。かまど・醸造台は IInventory#getField、スポナーは非公開の
 * spawnDelay（読めなければスポナーだけ答えない）。値の計算は MachineProgress。
 */
public final class MachineStates {

    /** MobSpawnerBaseLogic#spawnDelay。 */
    private static final Field SPAWN_DELAY = Reflect.field(MobSpawnerBaseLogic.class, "spawner progress bar",
            "spawnDelay", "field_98286_b");
    /** ディメンションごと・位置ごとの、スポナーの待ち時間の始まりの値。 */
    private static final Map<Integer, Map<BlockPos, MachineProgress.SpawnerDelay>> SPAWNERS = new HashMap<>();

    private MachineStates() {
    }

    /** バーを出す機械か（両側。クライアントは問い合わせるかどうかに使う）。 */
    public static boolean hasBar(TileEntity te) {
        return te instanceof TileEntityFurnace || te instanceof TileEntityBrewingStand
                || te instanceof TileEntityMobSpawner;
    }

    public static void onQuery(EntityPlayerMP player, BlockPos pos) {
        if (!ServerSwitches.isEnabled(player, HitKind.MACHINE)) {
            return;
        }
        World world = player.world;
        if (!world.isBlockLoaded(pos) || !withinReach(player, pos)) {
            return;
        }
        TileEntity te = world.getTileEntity(pos);
        float progress;
        float fuel = -1;
        if (te instanceof TileEntityFurnace) {
            TileEntityFurnace furnace = (TileEntityFurnace) te;
            progress = (float) MachineProgress.ratio(furnace.getField(2), furnace.getField(3));
            fuel = (float) MachineProgress.ratio(furnace.getField(0), furnace.getField(1));
        } else if (te instanceof TileEntityBrewingStand) {
            TileEntityBrewingStand stand = (TileEntityBrewingStand) te;
            progress = (float) MachineProgress.brewing(stand.getField(0));
            fuel = (float) MachineProgress.ratio(stand.getField(1), MachineProgress.BREW_FUEL_MAX);
        } else if (te instanceof TileEntityMobSpawner && SPAWN_DELAY != null) {
            int delay;
            try {
                delay = SPAWN_DELAY.getInt(((TileEntityMobSpawner) te).getSpawnerBaseLogic());
            } catch (IllegalAccessException e) {
                return;
            }
            progress = (float) SPAWNERS.computeIfAbsent(world.provider.getDimension(), d -> new HashMap<>())
                    .computeIfAbsent(pos.toImmutable(), p -> new MachineProgress.SpawnerDelay()).update(delay);
        } else {
            return;
        }
        WeakSpotMod.network.sendTo(StateMessage.machine(pos, progress, fuel), player);
    }

    /** バニラが右クリックを受け付ける距離と同じ（RightClickHits と同じ）。 */
    private static boolean withinReach(EntityPlayerMP player, BlockPos pos) {
        double reach = player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue() + 3;
        return player.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < reach * reach;
    }

    /** サーバーが止まったら捨てる。 */
    public static void clear() {
        SPAWNERS.clear();
    }
}
