package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.common.GrowthStuck;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;

/**
 * 暗い（作物・茎・苗木で、育つのに要る明るさが 9 未満）ときは最初のヒットで知らせる。判定と回数の制限は GrowthStuck。
 * 明るさの測り方はバニラと同じ（作物・茎はそのブロック、苗木はすぐ上の周りの一番明るい所）。
 */
public final class GrowthWarnings {

    /** バニラの作物・茎・苗木が育つのに必要な明るさ。 */
    static final int MIN_LIGHT = 9;

    private static final Map<UUID, GrowthStuck> STUCK = new HashMap<>();

    private GrowthWarnings() {
    }

    /** changed は効果の前後で状態が変わったか。 */
    static void onGrowthHit(ServerPlayer player, Level world, BlockPos pos, boolean changed) {
        if (!WeakSpotConfig.server.growth.growthWarnings) {
            return;
        }
        int light = light(world, pos, world.getBlockState(pos).getBlock());
        boolean dark = light >= 0 && light < MIN_LIGHT;
        GrowthStuck.Warning warning = STUCK.computeIfAbsent(player.getUUID(), id -> new GrowthStuck())
                .onHit(pos.asLong(), changed, dark, WeakSpotConfig.server.growth.growthStuckHits, world.getGameTime());
        Component message;
        switch (warning) {
            case DARK:
                message = Component.translatable("weakspot.growth.dark", light);
                break;
            case STUCK:
                message = Component.translatable("weakspot.growth.stuck", WeakSpotConfig.server.growth.growthStuckHits);
                break;
            default:
                return;
        }
        player.sendSystemMessage(message.copy().withStyle(ChatFormatting.YELLOW));
    }

    /** 明るさで育つかが決まらないブロックは -1。 */
    private static int light(Level world, BlockPos pos, Block block) {
        if (block instanceof CropBlock || block instanceof StemBlock) {
            return world.getRawBrightness(pos, 0);
        }
        if (block instanceof SaplingBlock) {
            return world.getMaxLocalRawBrightness(pos.above());
        }
        return -1;
    }

    static void forget(Player player, HitGate.Leave leave) {
        if (leave != HitGate.Leave.RESPAWN) {
            STUCK.remove(player.getUUID());
        }
    }
}
