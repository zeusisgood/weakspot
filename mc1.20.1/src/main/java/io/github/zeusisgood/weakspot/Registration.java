package io.github.zeusisgood.weakspot;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** アイテムと効果。プレイヤーの保存データは、1.12.2 と同じく PlayerPersisted に置く（ServerStats）。 */
public final class Registration {

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, WeakSpotMod.MODID);
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,
            WeakSpotMod.MODID);

    public static final RegistryObject<ItemTarget> TARGET = ITEMS.register("target", ItemTarget::new);
    public static final RegistryObject<ItemShard> SHARD = ITEMS.register("shard", ItemShard::new);
    public static final RegistryObject<ItemComboHourglass> COMBO_HOURGLASS = ITEMS.register("combo_hourglass",
            ItemComboHourglass::new);
    public static final RegistryObject<ItemCharm> CHARM = ITEMS.register("charm", ItemCharm::new);
    public static final RegistryObject<ItemMonocle> MONOCLE = ITEMS.register("monocle", ItemMonocle::new);

    public static final RegistryObject<ComboGraceEffect> COMBO_GRACE = EFFECTS.register("combo_grace",
            ComboGraceEffect::new);

    private Registration() {
    }

    static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        EFFECTS.register(modBus);
        modBus.addListener(Registration::onCreativeTabs);
    }

    private static void onCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(TARGET.get());
            event.accept(SHARD.get());
            event.accept(COMBO_HOURGLASS.get());
            event.accept(CHARM.get());
            event.accept(MONOCLE.get());
        }
    }
}
