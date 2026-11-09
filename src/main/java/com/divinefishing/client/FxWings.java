package com.divinefishing.client;

import com.divinefishing.net.VfxPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Ailes de plumes lumineuses (ange et phenix) et effet de la plume d'ange. */
public final class FxWings {
    private static final Vec3 Y = new Vec3(0, 1, 0);

    /** Direction vers laquelle regarde le corps, en degres. */
    static float bodyYaw(Entity e, float partial) {
        if (e instanceof LivingEntity le) {
            return Mth.rotLerp(partial, le.yBodyRotO, le.yBodyRot);
        }
        return e != null ? e.getYRot() : 0.0F;
    }

    /**
     * Dessine une paire d'ailes.
     *
     * @param shoulder point entre les deux epaules
     * @param flap     angle de battement en radians (positif = ailes qui se levent)
     * @param spread   0 = ailes repliees, 1 = pleinement deployees
     */
    static void wings(VfxDraw d, Vec3 shoulder, float yawDeg, int feathers, float length, float flap, float spread,
                      int inner, int outer, float alpha, float time) {
        if (alpha <= 0.01F || spread <= 0.01F) {
            return;
        }
        double yaw = Math.toRadians(yawDeg);
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 leftDir = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        Vec3 back = forward.scale(-1.0D);

        Vec3[] tips = new Vec3[feathers * 2];
        d.type(VfxRenderTypes.LINE_ADD);
        int n = 0;
        for (int side = -1; side <= 1; side += 2) {
            Vec3 out = leftDir.scale(side);
            for (int k = 0; k < feathers; k++) {
                float f = k / (float) (feathers - 1);
                double phi = Math.toRadians(18.0D + 112.0D * f) * (0.3D + 0.7D * spread) + flap;
                double len = length * (0.5D + 0.5D * Math.sin((0.22D + 0.78D * f) * Math.PI * 0.92D))
                        * (0.35D + 0.65D * spread);
                Vec3 dir = out.scale(Math.sin(phi)).add(Y.scale(Math.cos(phi))).add(back.scale(0.30D + 0.12D * f))
                        .normalize();
                Vec3 base = shoulder.add(out.scale(0.16D)).add(0, 0.04D * k, 0);
                Vec3 mid = base.add(dir.scale(len * 0.55D)).add(0, len * 0.06D, 0);
                Vec3 tip = base.add(dir.scale(len)).add(0, -len * 0.04D * f, 0);
                float flutter = 0.92F + 0.08F * (float) Math.sin(time * 0.3F + k * 0.8F + side);
                d.ribbon(base, mid, 0.42F, 0.34F, inner, alpha * 0.95F * flutter, alpha * 0.9F * flutter);
                d.ribbon(mid, tip, 0.34F, 0.05F, outer, alpha * 0.9F * flutter, alpha * 0.15F);
                d.ribbon(base, tip, 0.12F, 0.02F, 0xFFFFFF, alpha * 0.55F, alpha * 0.1F);
                tips[n++] = tip;
            }
        }
        d.type(VfxRenderTypes.STAR_ADD);
        for (int i = 0; i < n; i++) {
            d.glow(tips[i], 0.55F, time * 0.05F + i, outer, alpha * 0.8F);
        }
        d.type(VfxRenderTypes.GLOW_ADD);
        d.glow(shoulder, 1.6F * spread, outer, alpha * 0.45F);
    }

    /** Ailes d'ange + halo + plumes qui tombent : dure tout le temps du vol. */
    public static final class Angel extends Fx {
        public Angel(VfxPacket p) {
            super(p);
        }

        @Override
        public void render(VfxDraw d, float t, float partial) {
            Entity e = followed();
            Vec3 p = pos(partial);
            float grow = Rnd.smooth(Rnd.ramp(t, 0.0F, 24.0F)) * (1.0F - Rnd.smooth(Rnd.ramp(t, duration - 20.0F, duration)));
            if (grow <= 0.01F) {
                return;
            }
            float yaw = bodyYaw(e, partial);
            boolean active = e != null && (e.getDeltaMovement().horizontalDistanceSqr() > 0.01D || !e.onGround());
            float flap = (float) Math.sin(t * 0.14F) * 0.10F + (active ? (float) Math.sin(t * 0.42F) * 0.13F : 0.0F);
            double height = e != null ? e.getBbHeight() : 1.8D;
            Vec3 shoulder = p.add(0, height * 0.75D, 0);

            d.type(VfxRenderTypes.GLOW_ADD);
            d.glow(p.add(0, height * 0.6D, 0), 5.5F, color, 0.13F * grow);

            wings(d, shoulder, yaw, 11, 3.1F, flap, grow, 0xFFFFFF, color, 0.85F * grow, t);

            Minecraft mc = Minecraft.getInstance();
            boolean hideHalo = e == mc.player && mc.options.getCameraType().isFirstPerson();
            if (!hideHalo) {
                Vec3 top = p.add(0, height + 0.32D + 0.03D * Math.sin(t * 0.12D), 0);
                d.type(VfxRenderTypes.LINE_ADD);
                d.ring(top, AXIS_X, AXIS_Z, 0.34D, 0.11D, 0xFFE9A0, 0.95F * grow, 32);
                d.ring(top, AXIS_X, AXIS_Z, 0.34D, 0.28D, color, 0.35F * grow, 32);
                d.type(VfxRenderTypes.GLOW_ADD);
                d.glow(top, 1.3F, color, 0.35F * grow);
            }

            // plumes lumineuses qui tombent doucement
            d.type(VfxRenderTypes.STAR_ADD);
            float s = t / 20.0F;
            double yawRad = Math.toRadians(yaw);
            Vec3 leftDir = new Vec3(Math.cos(yawRad), 0, Math.sin(yawRad));
            for (int i = 0; i < 16; i++) {
                float phase = fract(s / 3.2F + Rnd.f(seed, i));
                float side = i % 2 == 0 ? 1.0F : -1.0F;
                Vec3 start = shoulder.add(leftDir.scale(side * (0.8D + Rnd.f(seed, i + 50) * 2.2D)))
                        .add(0, Rnd.f(seed, i + 80) * 0.8D, 0);
                Vec3 fall = start.add(Math.sin(phase * 6.0F + i) * 0.4D, -phase * 3.4D, Math.cos(phase * 5.0F + i) * 0.4D);
                d.glow(fall, 0.3F, phase * 8.0F + i, 0xFFFFFF, (float) Math.sin(phase * Math.PI) * 0.85F * grow);
            }
        }
    }

    private FxWings() {}
}
