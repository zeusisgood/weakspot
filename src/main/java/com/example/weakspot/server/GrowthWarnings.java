package com.example.weakspot.server;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.GrowthStuck;
import com.example.weakspot.config.WeakSpotConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.BlockStem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * 成長の弱点に当てても育たないときに、チャットで知らせる（1.4.3）。
 * 暗い（作物・茎・苗木で、すぐ上の明るさが 9 未満）ときは最初のヒットで、原因が分からないときは
 * growthStuckHits 回続けて状態が変わらなかったときに知らせる。判定と回数の制限は GrowthStuck。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class GrowthWarnings {

    /** バニラの作物・茎・苗木が育つのに必要な、すぐ上の明るさ。 */
    static final int MIN_LIGHT = 9;

    private static final Map<UUID, GrowthStuck> STUCK = new HashMap<>();

    private GrowthWarnings() {
    }

    /** 成長のヒットの効果をかけたあとに呼ぶ。changed は効果の前後で状態が変わったか。 */
    static void onGrowthHit(EntityPlayerMP player, World world, BlockPos pos, boolean changed) {
        if (!WeakSpotConfig.growthWarnings) {
            return;
        }
        int light = needsLight(world.getBlockState(pos).getBlock()) ? world.getLightFromNeighbors(pos.up()) : -1;
        boolean dark = light >= 0 && light < MIN_LIGHT;
        GrowthStuck.Warning warning = STUCK.computeIfAbsent(player.getUniqueID(), id -> new GrowthStuck())
                .onHit(pos.toLong(), changed, dark, WeakSpotConfig.growthStuckHits, world.getTotalWorldTime());
        TextComponentTranslation message;
        switch (warning) {
            case DARK:
                message = new TextComponentTranslation("weakspot.growth.dark", light);
                break;
            case STUCK:
                message = new TextComponentTranslation("weakspot.growth.stuck", WeakSpotConfig.growthStuckHits);
                break;
            default:
                return;
        }
        message.getStyle().setColor(TextFormatting.YELLOW);
        player.sendMessage(message);
    }

    /** バニラの成長の判定で、すぐ上の明るさを見るブロック。 */
    private static boolean needsLight(Block block) {
        return block instanceof BlockCrops || block instanceof BlockStem || block instanceof BlockSapling;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STUCK.remove(event.player.getUniqueID());
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        STUCK.remove(event.player.getUniqueID());
    }
}
