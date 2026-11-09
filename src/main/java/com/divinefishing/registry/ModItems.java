package com.divinefishing.registry;

import com.divinefishing.DivineFishing;
import com.divinefishing.item.DivineItem;
import com.divinefishing.item.DivineRodItem;
import com.divinefishing.item.JudgmentBladeItem;
import com.divinefishing.server.Abilities;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DivineFishing.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DivineFishing.MODID);

    private static Item.Properties props() {
        return new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant();
    }

    public static final RegistryObject<Item> DIVINE_ROD = ITEMS.register("divine_rod",
            () -> new DivineRodItem(props()));

    public static final RegistryObject<Item> JUDGMENT_BLADE = ITEMS.register("judgment_blade",
            () -> new JudgmentBladeItem(props()));

    public static final RegistryObject<Item> VOID_ORB = ITEMS.register("void_orb",
            () -> new DivineItem(props(), 20 * 25, Abilities::blackHole));

    public static final RegistryObject<Item> CHRONO_HOURGLASS = ITEMS.register("chrono_hourglass",
            () -> new DivineItem(props(), 20 * 45, Abilities::timeStop));

    public static final RegistryObject<Item> METEOR_STAFF = ITEMS.register("meteor_staff",
            () -> new DivineItem(props(), 20 * 30, Abilities::meteorStorm));

    public static final RegistryObject<Item> PHOENIX_HEART = ITEMS.register("phoenix_heart",
            () -> new DivineItem(props(), 20 * 60 * 5, Abilities::phoenix));

    public static final RegistryObject<Item> ANGEL_FEATHER = ITEMS.register("angel_feather",
            () -> new DivineItem(props(), 20, Abilities::angelFlight));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.divinefishing"))
                    .icon(() -> new ItemStack(DIVINE_ROD.get()))
                    .displayItems((params, out) -> ITEMS.getEntries().forEach(e -> out.accept(e.get())))
                    .build());

    private ModItems() {}
}
