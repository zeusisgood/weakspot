package io.github.zeusisgood.weakspot;

import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** アイテムと、プレイヤーの保存データ（1.12.2 の PlayerPersisted の weakspot と同じ中身）。 */
public final class Registration {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WeakSpotMod.MODID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, WeakSpotMod.MODID);

    public static final DeferredItem<ItemTarget> TARGET = ITEMS.register("target", ItemTarget::new);

    /** 統計・節目・的当ての記録など。死んでも残す（1.12.2 の PlayerPersisted と同じ）。 */
    public static final Supplier<AttachmentType<CompoundTag>> PLAYER_DATA = ATTACHMENTS.register("player_data",
            () -> AttachmentType.builder(() -> new CompoundTag()).serialize(CompoundTag.CODEC).copyOnDeath().build());

    private Registration() {
    }

    static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ATTACHMENTS.register(modBus);
        modBus.addListener(Registration::onCreativeTabs);
    }

    private static void onCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(TARGET.get());
        }
    }
}
