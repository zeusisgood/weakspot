package io.github.zeusisgood.weakspot;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** アイテム・効果と、プレイヤーの保存データ（1.12.2 の PlayerPersisted の weakspot と同じ中身）。 */
public final class Registration {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WeakSpotMod.MODID);
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT,
            WeakSpotMod.MODID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, WeakSpotMod.MODID);

    public static final DeferredItem<ItemTarget> TARGET = ITEMS.register("target", ItemTarget::new);
    public static final DeferredItem<ItemShard> SHARD = ITEMS.register("shard", ItemShard::new);
    public static final DeferredItem<ItemComboHourglass> COMBO_HOURGLASS = ITEMS.register("combo_hourglass",
            ItemComboHourglass::new);
    public static final DeferredItem<ItemCharm> CHARM = ITEMS.register("charm", ItemCharm::new);
    public static final DeferredItem<ItemMonocle> MONOCLE = ITEMS.register("monocle", ItemMonocle::new);

    public static final DeferredHolder<MobEffect, ComboGraceEffect> COMBO_GRACE = EFFECTS.register("combo_grace",
            ComboGraceEffect::new);

    /** 統計・節目・的当ての記録など。死んでも残す（1.12.2 の PlayerPersisted と同じ）。 */
    public static final Supplier<AttachmentType<CompoundTag>> PLAYER_DATA = ATTACHMENTS.register("player_data",
            () -> AttachmentType.builder(() -> new CompoundTag()).serialize(CompoundTag.CODEC).copyOnDeath().build());

    private Registration() {
    }

    static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        EFFECTS.register(modBus);
        ATTACHMENTS.register(modBus);
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
