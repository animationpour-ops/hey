package com.divinefishing.client;

import com.divinefishing.DivineFishing;
import com.divinefishing.registry.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = DivineFishing.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Meme propriete "cast" que la canne a peche vanilla : le modele change quand la ligne est lancee.
        event.enqueueWork(() -> ItemProperties.register(ModItems.DIVINE_ROD.get(),
                new ResourceLocation("cast"), (stack, level, entity, seed) -> {
                    if (entity == null) {
                        return 0.0F;
                    }
                    boolean main = entity.getMainHandItem() == stack;
                    boolean off = entity.getOffhandItem() == stack;
                    if (entity.getMainHandItem().getItem() instanceof FishingRodItem) {
                        off = false;
                    }
                    return (main || off) && entity instanceof Player p && p.fishing != null ? 1.0F : 0.0F;
                }));
    }

    private ClientSetup() {}
}
