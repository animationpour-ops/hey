package com.divinefishing.server;

import com.divinefishing.net.VfxPacket;
import com.divinefishing.net.VfxType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Trou noir : aspire tout, ecrase au centre, puis s'effondre en une onde de choc. */
public class BlackHoleEffect implements ServerEffects.Timed {
    public static final int DURATION = 120;
    private static final double PULL_RADIUS = 20.0D;

    private final ServerPlayer owner;
    private final Vec3 center;
    private final int duration;
    private int age;
    private boolean finished;

    public BlackHoleEffect(ServerPlayer owner, Vec3 center, int duration) {
        this.owner = owner;
        this.center = center;
        this.duration = duration;
    }

    private boolean affects(Entity e) {
        if (e == owner || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand) {
            return false;
        }
        return !(e instanceof Player p && p.isCreative() && p.getAbilities().flying);
    }

    @Override
    public boolean tick(ServerLevel level) {
        if (age == 0) {
            VfxPacket.of(VfxType.BLACK_HOLE, center).duration(duration).color(0x8A4DFF).send(level);
        }
        if (age % 30 == 0) {
            level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_AMBIENT,
                    SoundSource.HOSTILE, 4.0F, 0.5F);
        }

        AABB box = new AABB(center, center).inflate(PULL_RADIUS);
        for (Entity e : level.getEntities((Entity) null, box, this::affects)) {
            Vec3 mid = e.position().add(0, e.getBbHeight() * 0.5, 0);
            Vec3 d = center.subtract(mid);
            double dist = d.length();
            if (dist > PULL_RADIUS) {
                continue;
            }
            if (dist < 0.8D) {
                if (e instanceof LivingEntity le && age % 5 == 0) {
                    Abilities.hurt(level, le, owner, 8.0F);
                }
                e.setDeltaMovement(e.getDeltaMovement().scale(0.3D));
                continue;
            }
            double strength = 0.25D + 1.15D * (1.0D - dist / PULL_RADIUS);
            Vec3 pull = d.normalize().scale(strength);
            Vec3 swirl = new Vec3(-d.z, 0.0D, d.x);
            swirl = swirl.lengthSqr() < 1.0E-6 ? Vec3.ZERO : swirl.normalize().scale(0.16D);
            e.setDeltaMovement(e.getDeltaMovement().scale(0.45D).add(pull.scale(0.7D)).add(swirl));
            e.hurtMarked = true;
            e.fallDistance = 0.0F;
            if (e instanceof LivingEntity le && dist < 5.0D && age % 10 == 0) {
                Abilities.hurt(level, le, owner, 4.0F + (float) (5.0D - dist));
            }
        }
        age++;
        finished = age >= duration;
        return finished;
    }

    @Override
    public void end(ServerLevel level) {
        if (!finished) {
            return;
        }
        VfxPacket.of(VfxType.COLLAPSE, center).duration(46).color(0x8A4DFF).send(level);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE,
                SoundSource.HOSTILE, 8.0F, 0.4F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.HOSTILE, 8.0F, 0.6F);

        for (LivingEntity e : Abilities.targetsAround(level, owner, center, 12.0D)) {
            Abilities.hurt(level, e, owner, 45.0F);
            Abilities.knock(e, center, 2.4D, 1.0D);
        }
        AABB box = new AABB(center, center).inflate(12.0D);
        for (Entity e : level.getEntities((Entity) null, box, x -> affects(x) && !(x instanceof LivingEntity))) {
            Abilities.knock(e, center, 1.6D, 0.6D);
        }
        if (level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            carve(level, 4);
        }
    }

    private void carve(ServerLevel level, int radius) {
        BlockPos c = BlockPos.containing(center);
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radius * radius) {
                        continue;
                    }
                    BlockPos p = c.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(p);
                    if (!s.isAir() && s.getDestroySpeed(level, p) >= 0 && level.getBlockEntity(p) == null) {
                        level.setBlock(p, air, 3);
                    }
                }
            }
        }
    }
}
