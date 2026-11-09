package com.divinefishing.server;

import com.divinefishing.net.VfxPacket;
import com.divinefishing.net.VfxType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Arret du temps : tout ce qui est autour du lanceur est fige, sauf lui et ses projectiles. */
public class TimeStopEffect implements ServerEffects.Timed {
    public static final int DURATION = 160;

    private static final String TAG_FROZEN = "divinefishing_frozen";
    private static final String TAG_NOAI = "divinefishing_prev_noai";
    private static final String TAG_NOGRAV = "divinefishing_prev_nograv";

    /** Entites actuellement figees par un effet en cours (pour rattraper les oublis apres un rechargement). */
    private static final Set<UUID> ACTIVE = new HashSet<>();

    private final ServerPlayer owner;
    private final int duration;
    private final double radius;
    private final Map<UUID, Vec3> frozen = new HashMap<>();
    private int age;

    public TimeStopEffect(ServerPlayer owner, int duration, double radius) {
        this.owner = owner;
        this.duration = duration;
        this.radius = radius;
    }

    public static boolean isActivelyFrozen(Entity e) {
        return ACTIVE.contains(e.getUUID());
    }

    private boolean freezable(Entity e) {
        if (e == owner || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand) {
            return false;
        }
        if (e instanceof Projectile p) {
            return p.getOwner() != owner;
        }
        return e instanceof LivingEntity || e instanceof FallingBlockEntity;
    }

    @Override
    public boolean tick(ServerLevel level) {
        if (age == 0) {
            VfxPacket.of(VfxType.TIME_STOP, owner.position()).follow(owner).duration(duration)
                    .param((float) radius).color(0xFFD86B).send(level);
        }
        if (age % 20 == 0) {
            level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.BELL_BLOCK,
                    SoundSource.PLAYERS, 1.2F, 0.5F + (age / 20) * 0.04F);
        }

        AABB box = owner.getBoundingBox().inflate(radius);
        for (Entity e : level.getEntities((Entity) null, box, this::freezable)) {
            if (!frozen.containsKey(e.getUUID())) {
                freeze(e);
            }
        }
        for (UUID id : frozen.keySet()) {
            Entity e = level.getEntity(id);
            if (e != null) {
                e.setDeltaMovement(Vec3.ZERO);
                if (e instanceof ServerPlayer) {
                    e.hurtMarked = true;
                }
            }
        }
        age++;
        return age >= duration || owner.isRemoved();
    }

    private void freeze(Entity e) {
        frozen.put(e.getUUID(), e.getDeltaMovement());
        ACTIVE.add(e.getUUID());
        CompoundTag data = e.getPersistentData();
        data.putBoolean(TAG_FROZEN, true);
        data.putBoolean(TAG_NOGRAV, e.isNoGravity());
        if (e instanceof Mob m) {
            data.putBoolean(TAG_NOAI, m.isNoAi());
            m.setNoAi(true);
        }
        e.setNoGravity(true);
        e.setDeltaMovement(Vec3.ZERO);
        if (e instanceof ServerPlayer sp) {
            int dur = duration + 10;
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 9, false, false));
            sp.addEffect(new MobEffectInstance(MobEffects.JUMP, dur, 128, false, false));
            sp.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, dur, 5, false, false));
            sp.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, 5, false, false));
        }
    }

    @Override
    public void end(ServerLevel level) {
        for (Map.Entry<UUID, Vec3> en : frozen.entrySet()) {
            Entity e = level.getEntity(en.getKey());
            ACTIVE.remove(en.getKey());
            if (e == null) {
                continue;
            }
            release(e);
            if (e instanceof ServerPlayer sp) {
                sp.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                sp.removeEffect(MobEffects.JUMP);
                sp.removeEffect(MobEffects.DIG_SLOWDOWN);
                sp.removeEffect(MobEffects.WEAKNESS);
            } else {
                e.setDeltaMovement(en.getValue());
            }
        }
        frozen.clear();
        if (!owner.isRemoved()) {
            level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.PLAYERS, 2.0F, 1.5F);
        }
    }

    /** Restaure l'IA et la gravite d'une entite figee (aussi utilise au rechargement des chunks). */
    public static void release(Entity e) {
        CompoundTag data = e.getPersistentData();
        if (!data.getBoolean(TAG_FROZEN)) {
            return;
        }
        if (e instanceof Mob m && data.contains(TAG_NOAI)) {
            m.setNoAi(data.getBoolean(TAG_NOAI));
        }
        e.setNoGravity(data.getBoolean(TAG_NOGRAV));
        data.remove(TAG_FROZEN);
        data.remove(TAG_NOAI);
        data.remove(TAG_NOGRAV);
    }
}
