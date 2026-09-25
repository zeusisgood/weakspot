package com.example.weakspot.server;

import com.example.weakspot.Reflect;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.config.WeakSpotConfig;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * エンチャントの弱点のヒット通知の検証と効果（論理サーバー。1.7.0）。エンチャント台の画面を開いて物を置いている間に
 * 受け付け、3 つの候補を引き直す。バニラの候補は、プレイヤーの「エンチャントの種」（非公開の xpSeed。エンチャント
 * するたびにバニラが引き直す）と本棚の数で決まるので、種を新しい乱数にして、開いている画面の候補を計算し直す
 * （バニラの送信でクライアントの画面にも届く）。何も減らさない。種は保存されるので、画面を閉じても引き直したまま。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class EnchantHits {

    private static final int INTERVAL_JITTER_TICKS = 2;

    private static final Map<UUID, Long> LAST_HIT = new HashMap<>();
    /** EntityPlayer の非公開の xpSeed（SRG field_175152_f）。読めなければ、エンチャントの弱点を出さない。 */
    private static Field xpSeed;
    private static boolean resolved;

    private EnchantHits() {
    }

    /** 種を読み書きできるか（できなければ SyncedSettings で enchantWeakSpotEnabled を false にして送る）。 */
    public static synchronized boolean isAvailable() {
        if (!resolved) {
            resolved = true;
            xpSeed = Reflect.field(EntityPlayer.class, "the enchanting weak spot", "xpSeed", "field_175152_f");
        }
        return xpSeed != null;
    }

    public static void onHit(EntityPlayerMP player, int streak) {
        if (!ServerSwitches.isEnabled(player, HitKind.ENCHANT) || player.isSpectator()
                || !WeakSpotConfig.enchantWeakSpotEnabled || !isAvailable()
                || !(player.openContainer instanceof ContainerEnchantment)) {
            return;
        }
        ContainerEnchantment container = (ContainerEnchantment) player.openContainer;
        if (container.tableInventory.getStackInSlot(0).isEmpty()) {
            return;
        }
        long now = player.world.getTotalWorldTime();
        Long last = LAST_HIT.get(player.getUniqueID());
        int minInterval = Math.max(0, WeakSpotConfig.enchantMinHitIntervalTicks - INTERVAL_JITTER_TICKS);
        if (last != null && now - last < minInterval) {
            return;
        }
        if (!reroll(player, container)) {
            return;
        }
        LAST_HIT.put(player.getUniqueID(), now);
        ServerStats.countStreak(player);
        ServerStats.recordKindHit(player, HitKind.ENCHANT);
        ServerBoostTracker.notifyNearbyPlayers(player, new BlockPos(player), streak);
    }

    /** 種を新しい乱数にして、候補を計算し直す。書けなければ false。 */
    private static boolean reroll(EntityPlayerMP player, ContainerEnchantment container) {
        int seed = player.getRNG().nextInt();
        try {
            xpSeed.setInt(player, seed);
        } catch (IllegalAccessException | RuntimeException e) {
            return false;
        }
        container.xpSeed = seed;
        container.onCraftMatrixChanged(container.tableInventory);
        return true;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_HIT.remove(event.player.getUniqueID());
    }
}
