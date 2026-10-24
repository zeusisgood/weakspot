package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.ItemTarget;
import io.github.zeusisgood.weakspot.WeakSpotItems;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import net.minecraft.item.Item;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
public final class ItemModels {

    private ItemModels() {
    }

    @SubscribeEvent
    public static void onModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(ItemTarget.INSTANCE, 0,
                new ModelResourceLocation(ItemTarget.INSTANCE.getRegistryName(), "inventory"));
        for (Item item : WeakSpotItems.ALL) {
            ModelLoader.setCustomModelResourceLocation(item, 0,
                    new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }
    }
}
