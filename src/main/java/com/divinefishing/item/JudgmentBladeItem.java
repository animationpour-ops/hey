package com.divinefishing.item;

import com.divinefishing.server.Abilities;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Epee incassable : chaque coup foudroie la cible, le clic droit appelle le jugement. */
public class JudgmentBladeItem extends SwordItem {
    private static final int COOLDOWN = 20 * 15;

    public JudgmentBladeItem(Properties properties) {
        super(Tiers.NETHERITE, 14, -2.0F, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canBeDepleted() {
        return false;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target.level() instanceof ServerLevel level) {
            Abilities.miniStrike(level, attacker, target);
        }
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return DivineItem.use(this, Abilities::smite, COOLDOWN, level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        DivineItem.appendTooltip(this, COOLDOWN, tip);
    }
}
