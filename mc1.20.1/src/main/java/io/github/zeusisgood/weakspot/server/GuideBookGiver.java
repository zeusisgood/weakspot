package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.GuideBook;
import io.github.zeusisgood.weakspot.ItemTarget;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 渡したことは、統計と同じ永続データの guideGiven に記録する（死亡・ディメンション移動で消えない。古い版は、このキーを無視する）。
 * 弱点の的は本とは別に targetsGiven に記録するので、前の版から遊んでいる人にも 1 回届く。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class GuideBookGiver {

    private static final String TAG_GIVEN = "guideGiven";
    private static final String TAG_TARGETS_GIVEN = "targetsGiven";
    /** ないのは、1.11.0 で遊んでいた人か、まだ的をもらっていない人。 */
    private static final String TAG_GIFT_VERSION = "targetGiftVersion";
    /** targetGiftVersion がない、的をもらったことのある人は、この版から更新したとみなす。 */
    private static final String FIRST_GIFT_FROM = "1.11.0";
    private static final int STARTER_TARGETS = 3;
    private static final ResourceLocation TARGET_RECIPE = ResourceLocation.fromNamespaceAndPath(WeakSpotMod.MODID, "target");

    private GuideBookGiver() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !WeakSpotConfig.server.general.giveGuideBook) {
            return;
        }
        CompoundTag data = ServerStats.data(player);
        if (!data.getBoolean(TAG_GIVEN)) {
            data.putBoolean(TAG_GIVEN, true);
            give(player, GuideBook.create());
        }
        if (!data.getBoolean(TAG_TARGETS_GIVEN)) {
            data.putBoolean(TAG_TARGETS_GIVEN, true);
            give(player, new ItemStack(ItemTarget.get(), STARTER_TARGETS));
            unlockTargetRecipe(player);
            sendWelcome(player, true);
            // 初めての人には、ようこその分だけ（アップデートの分を重ねて渡さない）
            data.putString(TAG_GIFT_VERSION, WeakSpotMod.VERSION);
        } else {
            giveUpdateGift(player, data);
        }
    }

    /** パッチも含めて版が変わるたびに 1 回（いくつ版を飛ばしても 1 回分）。0 なら配らず、版も記録しない。 */
    private static void giveUpdateGift(ServerPlayer player, CompoundTag data) {
        int count = WeakSpotConfig.server.general.updateGiftTargets;
        String last = data.contains(TAG_GIFT_VERSION) ? data.getString(TAG_GIFT_VERSION) : FIRST_GIFT_FROM;
        if (count <= 0 || WeakSpotMod.VERSION.equals(last)) {
            return;
        }
        data.putString(TAG_GIFT_VERSION, WeakSpotMod.VERSION);
        give(player, new ItemStack(ItemTarget.get(), count));
        player.sendSystemMessage(Component.translatable("weakspot.target.updateGift", WeakSpotMod.VERSION, count)
                .withStyle(ChatFormatting.GOLD));
    }

    /** targets = false は「統計をリセット」のとき（的の行を除く）。 */
    public static void sendWelcome(ServerPlayer player, boolean targets) {
        line(player, "weakspot.welcome.title", ChatFormatting.GOLD, false);
        line(player, "weakspot.welcome.about", ChatFormatting.WHITE, false);
        line(player, "weakspot.welcome.combo", ChatFormatting.WHITE, false);
        if (targets) {
            line(player, "weakspot.welcome.targets", ChatFormatting.WHITE, true, STARTER_TARGETS);
        }
        line(player, "weakspot.welcome.rewards", ChatFormatting.WHITE, true);
        line(player, "weakspot.welcome.keys", ChatFormatting.WHITE, true);
        line(player, "weakspot.welcome.guide", ChatFormatting.WHITE, true);
    }

    private static void line(ServerPlayer player, String key, ChatFormatting color, boolean bullet, Object... args) {
        MutableComponent body = Component.translatable(key, args).withStyle(color);
        player.sendSystemMessage(key.endsWith(".title") ? body
                : Component.literal(bullet ? " ・" : " ").withStyle(ChatFormatting.YELLOW).append(body));
    }

    /** いっぱいなら足元に落とす。 */
    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    static void unlockTargetRecipe(ServerPlayer player) {
        player.awardRecipesByKey(new ResourceLocation[] {TARGET_RECIPE});
    }
}
