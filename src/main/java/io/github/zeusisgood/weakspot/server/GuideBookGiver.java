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
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * 初めてログインしたプレイヤーに、ガイドの本を1冊渡す（1.4.1。論理サーバー）。渡したことは、統計と同じ永続データの
 * guideGiven に記録する（死亡・ディメンション移動で消えない。古い版は、このキーを無視する）。持ち物がいっぱいなら足元に落とす。
 * （1.8.5〜1.8.9 にあった、古い形の本の差し替えは 1.9.0 でやめた。）
 * 1.11.0 から、弱点の的も 3 つ渡す（本とは別に targetsGiven に記録するので、前の版から遊んでいる人にも 1 回届く）。
 * 的のレシピは、渡したときと初めて弱点に当てたとき（WeakSpotAdvancements）に、レシピ本に出す。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class GuideBookGiver {

    private static final String TAG_GIVEN = "guideGiven";
    private static final String TAG_TARGETS_GIVEN = "targetsGiven";
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
            ITextComponent text = PlayerText.of(mp, TargetRounds.SINCE, "weakspot.target.given", STARTER_TARGETS);
            text.getStyle().setColor(TextFormatting.GOLD);
            mp.sendMessage(text);
        }
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
