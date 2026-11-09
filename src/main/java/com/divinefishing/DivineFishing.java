package com.divinefishing;

import com.divinefishing.net.ModNetwork;
import com.divinefishing.registry.ModItems;
import com.divinefishing.registry.ModLootModifiers;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(DivineFishing.MODID)
public class DivineFishing {
    public static final String MODID = "divinefishing";

    public DivineFishing() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(bus);
        ModItems.TABS.register(bus);
        ModLootModifiers.SERIALIZERS.register(bus);
        bus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
