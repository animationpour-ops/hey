package com.divinefishing.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Item divin generique : clic droit = capacite, recharge, toujours enchante visuellement. */
public class DivineItem extends Item {

    @FunctionalInterface
    public interface Activator {
        boolean activate(ServerLevel level, ServerPlayer player, ItemStack stack, InteractionHand hand);
    }

    private final int cooldownTicks;
    private final Activator activator;

    public DivineItem(Properties properties, int cooldownTicks, Activator activator) {
        super(properties);
        this.cooldownTicks = cooldownTicks;
        this.activator = activator;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return use(this, activator, cooldownTicks, level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        appendTooltip(this, cooldownTicks, tip);
    }

    /** Logique partagee avec la lame (qui est une epee). */
    public static InteractionResultHolder<ItemStack> use(Item item, Activator activator, int cooldownTicks,
                                                         Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(item)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide && player instanceof ServerPlayer sp && level instanceof ServerLevel sl) {
            if (!activator.activate(sl, sp, stack, hand)) {
                return InteractionResultHolder.fail(stack);
            }
            player.getCooldowns().addCooldown(item, cooldownTicks);
        }
        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static void appendTooltip(Item item, int cooldownTicks, List<Component> tip) {
        tip.add(Component.translatable(item.getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        if (cooldownTicks > 20) {
            tip.add(Component.translatable("tooltip.divinefishing.cooldown", cooldownTicks / 20)
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
