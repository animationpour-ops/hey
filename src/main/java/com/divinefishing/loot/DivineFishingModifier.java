package com.divinefishing.loot;

import com.divinefishing.net.VfxPacket;
import com.divinefishing.net.VfxType;
import com.divinefishing.registry.ModItems;
import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

/** Ajoute un item divin au butin de la peche, avec un faisceau de lumiere a la prise. */
public class DivineFishingModifier extends LootModifier {
    public static final Supplier<Codec<DivineFishingModifier>> CODEC = Suppliers.memoize(
            () -> RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, DivineFishingModifier::new)));

    /** Chance de base avec une canne normale. */
    private static final float BASE_CHANCE = 0.03F;
    /** Chance avec la canne divine. */
    private static final float ROD_CHANCE = 0.45F;
    /** Bonus par niveau de Luck of the Sea. */
    private static final float LUCK_BONUS = 0.02F;

    private record Drop(Supplier<Item> item, int weight, int color) {}

    private static Drop[] drops() {
        return new Drop[] {
                new Drop(ModItems.JUDGMENT_BLADE::get, 20, 0x9AD0FF),
                new Drop(ModItems.VOID_ORB::get, 14, 0x8A4DFF),
                new Drop(ModItems.CHRONO_HOURGLASS::get, 12, 0xFFD86B),
                new Drop(ModItems.METEOR_STAFF::get, 14, 0xFF8A2A),
                new Drop(ModItems.PHOENIX_HEART::get, 16, 0xFF5A1F),
                new Drop(ModItems.ANGEL_FEATHER::get, 24, 0xFFF2B0),
        };
    }

    public DivineFishingModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @NotNull
    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        ItemStack tool = context.getParamOrNull(LootContextParams.TOOL);
        boolean divineRod = tool != null && tool.is(ModItems.DIVINE_ROD.get());
        int luck = tool == null ? 0 : EnchantmentHelper.getFishingLuckBonus(tool);

        float chance = (divineRod ? ROD_CHANCE : BASE_CHANCE) + luck * LUCK_BONUS;
        if (context.getRandom().nextFloat() >= chance) {
            return loot;
        }

        Drop[] drops = drops();
        int total = 0;
        for (Drop d : drops) {
            total += d.weight();
        }
        int roll = context.getRandom().nextInt(total);
        Drop picked = drops[0];
        for (Drop d : drops) {
            roll -= d.weight();
            if (roll < 0) {
                picked = d;
                break;
            }
        }

        ItemStack stack = new ItemStack(picked.item().get());
        loot.add(stack);
        announce(context, stack, picked.color());
        return loot;
    }

    private static void announce(LootContext context, ItemStack stack, int color) {
        Entity hook = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        ServerLevel level = context.getLevel();
        Vec3 pos = hook != null ? hook.position() : context.getParamOrNull(LootContextParams.ORIGIN);
        if (pos == null) {
            return;
        }
        VfxPacket.of(VfxType.CATCH, pos).duration(70).color(color).send(level);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 1.2F, 1.5F);
        if (hook instanceof FishingHook fh && fh.getPlayerOwner() instanceof ServerPlayer player) {
            player.displayClientMessage(
                    Component.translatable("message.divinefishing.catch", stack.getHoverName()), true);
        }
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
