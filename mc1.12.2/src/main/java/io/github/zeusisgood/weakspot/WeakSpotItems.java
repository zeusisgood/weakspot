package io.github.zeusisgood.weakspot;

import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** 欠片と、それで作るアイテム・効果。弱点の的は ItemTarget。 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
public final class WeakSpotItems {

    public static final ItemShard SHARD = new ItemShard();
    public static final ItemComboHourglass COMBO_HOURGLASS = new ItemComboHourglass();
    public static final ItemCharm CHARM = new ItemCharm();
    public static final ItemMonocle MONOCLE = new ItemMonocle();
    public static final PotionComboGrace COMBO_GRACE = new PotionComboGrace();

    public static final Item[] ALL = {SHARD, COMBO_HOURGLASS, CHARM, MONOCLE};

    private WeakSpotItems() {
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(ALL);
    }

    @SubscribeEvent
    public static void registerPotions(RegistryEvent.Register<Potion> event) {
        event.getRegistry().register(COMBO_GRACE);
    }
}
