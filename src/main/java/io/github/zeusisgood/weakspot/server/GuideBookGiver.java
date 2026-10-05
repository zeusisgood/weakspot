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
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * 初めてログインしたプレイヤーに、ガイドの本を1冊渡す（1.4.1。論理サーバー）。渡したことは、統計と同じ永続データの
 * guideGiven に記録する（死亡・ディメンション移動で消えない。古い版は、このキーを無視する）。持ち物がいっぱいなら足元に落とす。
 * （1.8.5〜1.8.9 にあった、古い形の本の差し替えは 1.9.0 でやめた。）
 * 1.11.0 から、弱点の的も 3 つ渡す（本とは別に targetsGiven に記録するので、前の版から遊んでいる人にも 1 回届く）。
 * 渡したら、ようこそのメッセージを出す（sendWelcome）。1.11.1 から、Mod を更新したときも的を渡す（giveUpdateGift。設定 updateGiftTargets）。
 *的のレシピは、渡したときと初めて弱点に当てたとき（WeakSpotAdvancements）に、レシピ本に出す。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class GuideBookGiver {

    private static final String TAG_GIVEN = "guideGiven";
    private static final String TAG_TARGETS_GIVEN = "targetsGiven";
    /** アップデートの的を前に渡したときの版（1.11.1）。ないのは、1.11.0 で遊んでいた人か、まだ的をもらっていない人。 */
    private static final String TAG_GIFT_VERSION = "targetGiftVersion";
    /** targetGiftVersion がない、的をもらったことのある人は、この版から更新したとみなす。 */
    private static final String FIRST_GIFT_FROM = "1.11.0";
    /** アップデートの知らせのキーを足した版。 */
    private static final String GIFT_SINCE = "1.11.1";
    /** 最初に渡す弱点の的の数。 */
    private static final int STARTER_TARGETS = 3;
    /** 弱点の的のレシピ（assets/weakspot/recipes/target.json）。 */
    private static final ResourceLocation TARGET_RECIPE = new ResourceLocation(WeakSpotMod.MODID, "target");

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

    /**
     * アップデートの的（1.11.1）: そのプレイヤーが前に受け取ったときから Mod の版が変わっていたら、updateGiftTargets 個渡す
     * （パッチも含めて版が変わるたびに 1 回。いくつ版を飛ばしても 1 回分）。0 なら配らず、版も記録しない。
     */
    private static void giveUpdateGift(EntityPlayerMP player, NBTTagCompound data) {
        int count = WeakSpotConfig.server.general.updateGiftTargets;
        String last = data.hasKey(TAG_GIFT_VERSION) ? data.getString(TAG_GIFT_VERSION) : FIRST_GIFT_FROM;
        if (count <= 0 || WeakSpotMod.VERSION.equals(last)) {
            return;
        }
        data.setString(TAG_GIFT_VERSION, WeakSpotMod.VERSION);
        give(player, new ItemStack(ItemTarget.INSTANCE, count));
        ITextComponent text = PlayerText.of(player, GIFT_SINCE, "weakspot.target.updateGift", WeakSpotMod.VERSION,
                count);
        text.getStyle().setColor(TextFormatting.GOLD);
        player.sendMessage(text);
    }

    /**
     * ようこそのメッセージ（1.11.0。何の Mod で何ができるかと、的当て・キー・本の案内）。見出しは金、本文は白、行頭の「・」は黄。
     * 的を渡したときと、「統計をリセット」（統計画面・/weakspot reset）のとき（targets = false。的の行を除く）に出す。
     */
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
        ITextComponent body = PlayerText.of(player, TargetRounds.SINCE, key, args);
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

    /** 持ち物に入れる。いっぱいなら足元に落とす。 */
    private static void give(EntityPlayer player, ItemStack stack) {
        if (!player.inventory.addItemStackToInventory(stack)) {
            player.dropItem(stack, false);
        }
    }

    /** 弱点の的のレシピを、レシピ本（作業台の緑の本）に出す。 */
    static void unlockTargetRecipe(EntityPlayerMP player) {
        player.unlockRecipes(new ResourceLocation[] {TARGET_RECIPE});
    }
}
