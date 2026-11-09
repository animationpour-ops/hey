package com.divinefishing.server;

import com.divinefishing.net.VfxPacket;
import com.divinefishing.net.VfxType;
import com.divinefishing.util.TargetUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Les capacites cote serveur de tous les items divins. */
public final class Abilities {
    public static final String FLIGHT_TAG = "divinefishing_flight_until";
    public static final int FLIGHT_TICKS = 20 * 90;

    // ------------------------------------------------------------------ outils

    public static boolean validTarget(Entity owner, Entity e) {
        if (!(e instanceof LivingEntity le) || e == owner || !le.isAlive() || e.isSpectator()
                || e instanceof ArmorStand) {
            return false;
        }
        return !(e instanceof TamableAnimal pet && owner instanceof LivingEntity o && pet.isOwnedBy(o));
    }

    public static List<LivingEntity> targetsAround(ServerLevel level, Entity owner, Vec3 center, double radius) {
        AABB box = new AABB(center, center).inflate(radius);
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, x -> validTarget(owner, x))) {
            double r = radius + e.getBbWidth() * 0.5;
            if (e.position().add(0, e.getBbHeight() * 0.5, 0).distanceToSqr(center) <= r * r) {
                out.add(e);
            }
        }
        return out;
    }

    public static void hurt(ServerLevel level, LivingEntity target, Entity owner, float damage) {
        DamageSource src = owner instanceof LivingEntity
                ? level.damageSources().indirectMagic(owner, owner)
                : level.damageSources().magic();
        target.invulnerableTime = 0;
        target.hurt(src, damage);
    }

    public static void knock(Entity e, Vec3 from, double horizontal, double vertical) {
        Vec3 d = e.position().subtract(from);
        d = new Vec3(d.x, 0, d.z);
        if (d.lengthSqr() < 1.0E-4) {
            d = new Vec3(1, 0, 0);
        }
        d = d.normalize().scale(horizontal);
        e.setDeltaMovement(e.getDeltaMovement().add(d.x, vertical, d.z));
        e.hurtMarked = true;
    }

    // ------------------------------------------------------------ lame du jugement

    public static boolean smite(ServerLevel level, ServerPlayer player, ItemStack stack, InteractionHand hand) {
        Vec3 target = TargetUtil.targetPoint(player, 80.0D);
        for (int i = 0; i < 3; i++) {
            final boolean last = i == 2;
            ServerEffects.delay(level, i * 8 + 1, () -> strike(level, player, target, last));
        }
        return true;
    }

    private static void strike(ServerLevel level, ServerPlayer owner, Vec3 target, boolean last) {
        VfxPacket.of(VfxType.SMITE, target).duration(34).param(last ? 1.7F : 1.0F).color(0x9AD0FF).send(level);
        level.playSound(null, target.x, target.y, target.z, SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.WEATHER, 10.0F, 0.8F + level.random.nextFloat() * 0.3F);
        level.playSound(null, target.x, target.y, target.z, SoundEvents.LIGHTNING_BOLT_IMPACT,
                SoundSource.WEATHER, 5.0F, 0.9F);

        Set<LivingEntity> hit = new HashSet<>();
        for (LivingEntity e : targetsAround(level, owner, target, last ? 7.0D : 5.0D)) {
            hit.add(e);
            hurt(level, e, owner, last ? 40.0F : 22.0F);
            e.setSecondsOnFire(6);
            knock(e, target, last ? 1.6D : 0.9D, 0.7D);
        }
        if (last) {
            int chained = 0;
            for (LivingEntity e : targetsAround(level, owner, target, 18.0D)) {
                if (hit.contains(e) || chained >= 6) {
                    continue;
                }
                chained++;
                Vec3 end = e.position().add(0, e.getBbHeight() * 0.5, 0);
                VfxPacket.of(VfxType.ARC, target.add(0, 1.0, 0)).to(end).duration(16).color(0x9AD0FF).send(level);
                hurt(level, e, owner, 18.0F);
                e.setSecondsOnFire(4);
            }
        }
    }

    public static void miniStrike(ServerLevel level, LivingEntity attacker, LivingEntity target) {
        Vec3 pos = target.position();
        VfxPacket.of(VfxType.MINI_STRIKE, pos).duration(16).color(0x9AD0FF).send(level);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.LIGHTNING_BOLT_IMPACT,
                SoundSource.PLAYERS, 1.4F, 1.5F);
        hurt(level, target, attacker, 6.0F);
    }

    // ------------------------------------------------------------- orbe du neant

    public static boolean blackHole(ServerLevel level, ServerPlayer player, ItemStack stack, InteractionHand hand) {
        Vec3 target = TargetUtil.targetPoint(player, 45.0D);
        Vec3 center = target.add(0, 2.5, 0);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.END_PORTAL_SPAWN,
                SoundSource.PLAYERS, 1.5F, 0.6F);
        ServerEffects.add(level, new BlackHoleEffect(player, center, BlackHoleEffect.DURATION));
        return true;
    }

    // --------------------------------------------------------- sablier chronos

    public static boolean timeStop(ServerLevel level, ServerPlayer player, ItemStack stack, InteractionHand hand) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 3.0F, 0.5F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 3.0F, 0.5F);
        ServerEffects.add(level, new TimeStopEffect(player, TimeStopEffect.DURATION, 40.0D));
        return true;
    }

    // ----------------------------------------------------------- sceptre meteore

    public static boolean meteorStorm(ServerLevel level, ServerPlayer player, ItemStack stack, InteractionHand hand) {
        Vec3 target = TargetUtil.groundTarget(player, 110.0D);
        Vec3 look = player.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0, look.z);
        dir = dir.lengthSqr() < 1.0E-3 ? new Vec3(1, 0, 0) : dir.normalize();

        launchMeteor(level, player, target, dir, 1.0F, 0);
        for (int i = 0; i < 4; i++) {
            double ang = level.random.nextDouble() * Math.PI * 2.0;
            double rad = 6.0 + level.random.nextDouble() * 6.0;
            Vec3 around = target.add(Math.cos(ang) * rad, 0, Math.sin(ang) * rad);
            Vec3 ground = TargetUtil.groundBelow(level, around.add(0, 24, 0), player);
            launchMeteor(level, player, ground, dir, 0.5F, 6 + i * 6);
        }
        return true;
    }

    private static void launchMeteor(ServerLevel level, ServerPlayer owner, Vec3 target, Vec3 dir,
                                     float scale, int delay) {
        Vec3 start = target.add(dir.x * 50.0, 85.0, dir.z * 50.0);
        ServerEffects.delay(level, delay + 1, () -> {
            VfxPacket.of(VfxType.METEOR, start).to(target)
                    .duration(VfxPacket.METEOR_FLIGHT + 60).param(scale).color(0xFF8A2A).send(level);
            level.playSound(null, target.x, target.y, target.z, SoundEvents.FIRECHARGE_USE,
                    SoundSource.HOSTILE, 6.0F, 0.5F);
            ServerEffects.delay(level, VfxPacket.METEOR_FLIGHT, () -> meteorImpact(level, owner, target, scale));
        });
    }

    private static void meteorImpact(ServerLevel level, ServerPlayer owner, Vec3 target, float scale) {
        level.playSound(null, target.x, target.y, target.z, SoundEvents.GENERIC_EXPLODE,
                SoundSource.BLOCKS, 8.0F * scale + 2.0F, 0.55F);
        level.playSound(null, target.x, target.y, target.z, SoundEvents.DRAGON_FIREBALL_EXPLODE,
                SoundSource.BLOCKS, 6.0F * scale + 1.0F, 0.7F);

        double radius = 9.0D * scale;
        for (LivingEntity e : targetsAround(level, owner, target, radius)) {
            double dist = e.position().distanceTo(target);
            float dmg = (float) (55.0D * scale * (1.0D - 0.6D * Math.min(1.0D, dist / radius)));
            hurt(level, e, owner, dmg);
            e.setSecondsOnFire(8);
            knock(e, target, 2.0D * scale, 0.9D);
        }

        if (level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            carveCrater(level, target, Math.round(4.5F * scale));
        }
    }

    private static void carveCrater(ServerLevel level, Vec3 impact, int radius) {
        BlockPos center = BlockPos.containing(impact).below(Math.max(1, radius / 3));
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radius * radius) {
                        continue;
                    }
                    BlockPos p = center.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(p);
                    if (s.isAir() || s.getDestroySpeed(level, p) < 0 || level.getBlockEntity(p) != null) {
                        continue;
                    }
                    level.setBlock(p, air, 3);
                    if (dy <= -radius + 1 && level.random.nextFloat() < 0.25F
                            && level.getBlockState(p.below()).canOcclude()
                            && level.getBlockState(p.below()).getDestroySpeed(level, p.below()) >= 0) {
                        level.setBlock(p.below(), Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    // --------------------------------------------------------------- coeur de phenix

    public static boolean phoenix(ServerLevel level, ServerPlayer player, ItemStack stack, InteractionHand hand) {
        rebirth(level, player);
        return true;
    }

    /** Soin total + explosion de flammes. Sert aussi de totem d'immortalite. */
    public static void rebirth(ServerLevel level, ServerPlayer player) {
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
        player.clearFire();
        player.fallDistance = 0.0F;
        for (MobEffectInstance mi : new ArrayList<>(player.getActiveEffects())) {
            if (mi.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                player.removeEffect(mi.getEffect());
            }
        }
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 2));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 2));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200, 0));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 4));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 1));

        VfxPacket.of(VfxType.PHOENIX, player.position()).follow(player).duration(90).color(0xFF9A2E).send(level);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT,
                SoundSource.PLAYERS, 2.0F, 0.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRECHARGE_USE,
                SoundSource.PLAYERS, 2.0F, 0.8F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE,
                SoundSource.PLAYERS, 1.0F, 1.2F);

        for (LivingEntity e : targetsAround(level, player, player.position(), 7.0D)) {
            hurt(level, e, player, 14.0F);
            e.setSecondsOnFire(10);
            knock(e, player.position(), 1.5D, 0.6D);
        }
    }

    // ----------------------------------------------------------------- plume d'ange

    public static boolean angelFlight(ServerLevel level, ServerPlayer player, ItemStack stack, InteractionHand hand) {
        CompoundTag data = player.getPersistentData();
        long now = level.getGameTime();
        if (data.getLong(FLIGHT_TAG) > now) {
            endFlight(level, player);
            return true;
        }
        data.putLong(FLIGHT_TAG, now + FLIGHT_TICKS);
        player.getAbilities().mayfly = true;
        player.onUpdateAbilities();
        Vec3 dm = player.getDeltaMovement();
        player.setDeltaMovement(dm.x, Math.max(dm.y, 0.9D), dm.z);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        VfxPacket.of(VfxType.ANGEL, player.position()).follow(player).duration(FLIGHT_TICKS).color(0xFFF2B0).send(level);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_FLAP,
                SoundSource.PLAYERS, 1.5F, 1.4F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 0.8F, 1.7F);
        return true;
    }

    public static void endFlight(ServerLevel level, ServerPlayer player) {
        player.getPersistentData().putLong(FLIGHT_TAG, 0L);
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, false));
        }
        VfxPacket.of(VfxType.CANCEL, player.position()).follow(player).param(VfxType.ANGEL.ordinal()).send(level);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 0.8F, 1.6F);
    }

    private Abilities() {}
}
