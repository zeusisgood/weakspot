package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.BoostMath;
import com.example.weakspot.common.ComboFactor;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.config.SyncedSettings;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 自分の採掘のブースト（クライアント。1.8.9 で ClientWeakSpotHandler から分けた）。PlayerControllerMP は tick ごとに
 * 進捗を積むので、ヒットの次の tick から boostDurationTicks の間だけ、破壊速度に倍率（コンボの掛け数つき）を掛ける。
 * サーバーは瞬間判定なので別の計算（server/ServerBoostTracker・BoostMath）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
final class MiningBoost {

    private static BlockPos boostPos;
    private static long boostHitTick = Long.MIN_VALUE / 2;
    /** 最後の採掘ヒットのコンボの掛け数（1.8.7。サーバーが古ければ 1）。 */
    private static double boostFactor = 1;
    /** 瞬間破壊の判定中は、自分のブーストを掛けない。 */
    private static boolean suppressBoost;

    private MiningBoost() {
    }

    /** 採掘の弱点に当てた（streak はヒット後の連続ヒット数）。 */
    static void onHit(BlockPos pos, int streak) {
        boostHitTick = ClientWeakSpotHandler.clientTick;
        boostPos = pos;
        boostFactor = comboFactor(streak);
    }

    /** 一時オフ・ワールドを出たとき。 */
    static void clear() {
        boostPos = null;
        boostHitTick = Long.MIN_VALUE / 2;
        boostFactor = 1;
    }

    /** 壊せないブロック（硬度が負）と、今の破壊速度で1tick以内に壊れるブロックは対象外。 */
    static boolean isEligible(World world, EntityPlayerSP player, BlockPos pos, IBlockState state) {
        if (state.getBlock().isAir(state, world, pos) || state.getBlockHardness(world, pos) < 0) {
            return false;
        }
        suppressBoost = true;
        try {
            return state.getPlayerRelativeBlockHardness(player, world, pos) < 1.0F;
        } finally {
            suppressBoost = false;
        }
    }

    /** ヒット後の次の tick から boostDurationTicks 回分の進捗計算に倍率を掛ける。 */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!event.getEntityPlayer().world.isRemote || suppressBoost || !KindSwitches.isEnabled(HitKind.MINING)) {
            return;
        }
        if (event.getEntityPlayer() != Minecraft.getMinecraft().player || !event.getPos().equals(boostPos)) {
            return;
        }
        SyncedSettings settings = ClientSettings.get();
        long sinceHit = ClientWeakSpotHandler.clientTick - boostHitTick;
        if (sinceHit > 0 && sinceHit <= settings.boostDurationTicks) {
            event.setNewSpeed((float) (event.getNewSpeed()
                    * BoostMath.clientMultiplier(settings.boostMultiplier, boostFactor)));
        }
    }

    /** 採掘のコンボの掛け数（1.8.7。1.9.0 からサーバーの設定 miningComboBonus でオフにできる）。 */
    static double comboFactor(int combo) {
        return ClientSettings.get().miningComboBonus ? ComboFactor.factor(combo) : 1;
    }
}
