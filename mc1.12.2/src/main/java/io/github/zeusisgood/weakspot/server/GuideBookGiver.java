package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.GuideBook;
import io.github.zeusisgood.weakspot.ItemTarget;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

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
    private static final ResourceLocation TARGET_RECIPE = new ResourceLocation(WeakSpotMod.MODID, "target");
    private static final ResourceLocation[] SHARD_RECIPES = {
            new ResourceLocation(WeakSpotMod.MODID, "combo_hourglass"),
            new ResourceLocation(WeakSpotMod.MODID, "charm"),
            new ResourceLocation(WeakSpotMod.MODID, "monocle")};

    private GuideBookGiver() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        if (player.world.isRemote) {
            return;
        }
        if (!WeakSpotConfig.server.general.giveGuideBook) {
            return;
        }
        NBTTagCompound data = ServerStats.data(player);
        if (!data.getBoolean(TAG_GIVEN)) {
            data.setBoolean(TAG_GIVEN, true);
            give(player, GuideBook.create());
        }
        if (!data.getBoolean(TAG_TARGETS_GIVEN) && player instanceof EntityPlayerMP) {
            data.setBoolean(TAG_TARGETS_GIVEN, true);
            EntityPlayerMP mp = (EntityPlayerMP) player;
            give(player, new ItemStack(ItemTarget.INSTANCE, STARTER_TARGETS));
            unlockTargetRecipe(mp);
            sendWelcome(mp, true);
            // 初めての人には、ようこその分だけ（アップデートの分を重ねて渡さない）
            data.setString(TAG_GIFT_VERSION, WeakSpotMod.VERSION);
        } else if (player instanceof EntityPlayerMP) {
            giveUpdateGift((EntityPlayerMP) player, data);
        }
    }

    /** パッチも含めて版が変わるたびに 1 回（いくつ版を飛ばしても 1 回分）。0 なら配らず、版も記録しない。 */
    private static void giveUpdateGift(EntityPlayerMP player, NBTTagCompound data) {
        int count = WeakSpotConfig.server.general.updateGiftTargets;
        String last = data.hasKey(TAG_GIFT_VERSION) ? data.getString(TAG_GIFT_VERSION) : FIRST_GIFT_FROM;
        if (count <= 0 || WeakSpotMod.VERSION.equals(last)) {
            return;
        }
        data.setString(TAG_GIFT_VERSION, WeakSpotMod.VERSION);
        give(player, new ItemStack(ItemTarget.INSTANCE, count));
        ITextComponent text = new TextComponentTranslation("weakspot.target.updateGift", WeakSpotMod.VERSION,
                count);
        text.getStyle().setColor(TextFormatting.GOLD);
        player.sendMessage(text);
    }

    /** targets = false は「統計をリセット」のとき（的の行を除く）。 */
    public static void sendWelcome(EntityPlayerMP player, boolean targets) {
        line(player, "weakspot.welcome.title", TextFormatting.GOLD, false);
        line(player, "weakspot.welcome.about", TextFormatting.WHITE, false);
        line(player, "weakspot.welcome.combo", TextFormatting.WHITE, false);
        if (targets) {
            line(player, "weakspot.welcome.targets", TextFormatting.WHITE, true, STARTER_TARGETS);
        }
        line(player, "weakspot.welcome.rewards", TextFormatting.WHITE, true);
        line(player, "weakspot.welcome.keys", TextFormatting.WHITE, true);
        line(player, "weakspot.welcome.guide", TextFormatting.WHITE, true);
    }

    private static void line(EntityPlayerMP player, String key, TextFormatting color, boolean bullet, Object... args) {
        ITextComponent body = new TextComponentTranslation(key, args);
        body.getStyle().setColor(color);
        ITextComponent text = new TextComponentString(bullet ? " ・" : " ");
        text.getStyle().setColor(TextFormatting.YELLOW);
        if (key.endsWith(".title")) {
            text = body;
        } else {
            text.appendSibling(body);
        }
        player.sendMessage(text);
    }

    /** いっぱいなら足元に落とす。 */
    private static void give(EntityPlayer player, ItemStack stack) {
        if (!player.inventory.addItemStackToInventory(stack)) {
            player.dropItem(stack, false);
        }
    }

    static void unlockTargetRecipe(EntityPlayerMP player) {
        player.unlockRecipes(new ResourceLocation[] {TARGET_RECIPE});
    }

    static void unlockShardRecipes(EntityPlayerMP player) {
        player.unlockRecipes(SHARD_RECIPES);
    }
}
