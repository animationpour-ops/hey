package com.divinefishing.registry;

import com.divinefishing.DivineFishing;
import com.divinefishing.loot.DivineFishingModifier;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModLootModifiers {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, DivineFishing.MODID);

    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> DIVINE_FISHING =
            SERIALIZERS.register("divine_fishing", DivineFishingModifier.CODEC);

    private ModLootModifiers() {}
}
