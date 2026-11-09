package com.divinefishing.server;

import com.divinefishing.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "divinefishing")
public final class ServerEvents {
    /** Recharge du coeur de phenix quand il sauve la vie du joueur (5 minutes). */
    private static final int PHOENIX_SAVE_COOLDOWN = 20 * 60 * 5;

    /** Totem d'immortalite : le coeur de phenix annule une mort tant qu'il est dans l'inventaire. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        Item heart = ModItems.PHOENIX_HEART.get();
        if (player.getCooldowns().isOnCooldown(heart) || !carrying(player, heart)) {
            return;
        }
        event.setCanceled(true);
        player.getCooldowns().addCooldown(heart, PHOENIX_SAVE_COOLDOWN);
        Abilities.rebirth(level, player);
    }

    private static boolean carrying(ServerPlayer player, Item item) {
        for (ItemStack s : player.getInventory().items) {
            if (s.is(item)) {
                return true;
            }
        }
        for (ItemStack s : player.getInventory().offhand) {
            if (s.is(item)) {
                return true;
            }
        }
        return false;
    }

    /** Duree du vol de la plume d'ange. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        long until = player.getPersistentData().getLong(Abilities.FLIGHT_TAG);
        if (until <= 0L) {
            return;
        }
        if (level.getGameTime() >= until) {
            Abilities.endFlight(level, player);
        } else if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
    }

    /** Securite : une entite restee figee dans un chunk decharge est liberee quand elle revient. */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        Entity e = event.getEntity();
        if (!event.getLevel().isClientSide && !TimeStopEffect.isActivelyFrozen(e)) {
            TimeStopEffect.release(e);
        }
    }

    private ServerEvents() {}
}
