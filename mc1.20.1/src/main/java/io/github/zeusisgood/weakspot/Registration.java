package io.github.zeusisgood.weakspot;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** アイテム。プレイヤーの保存データは、1.12.2 と同じく PlayerPersisted に置く（ServerStats）。 */
public final class Registration {

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, WeakSpotMod.MODID);

    public static final RegistryObject<ItemTarget> TARGET = ITEMS.register("target", ItemTarget::new);

    private Registration() {
    }

    static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(Registration::onCreativeTabs);
    }

    private static void onCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(TARGET.get());
        }
    }
}
