package com.divinefishing.client;

import com.divinefishing.net.VfxPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Effets de feu : meteore (chute + impact) et renaissance du phenix. */
public final class FxFire {

    public static final class Meteor extends Fx {
        private static final int FLIGHT = VfxPacket.METEOR_FLIGHT;

        public Meteor(VfxPacket p) {
            super(p);
        }

        private Vec3 headAt(float t) {
            float u = (float) Math.pow(Rnd.clamp01(t / FLIGHT), 1.5D);
            return origin.add(target.subtract(origin).scale(u));
        }

        @Override
        public void render(VfxDraw d, float t, float partial) {
            float s = param;
            if (t < FLIGHT) {
                flight(d, t, s);
            } else {
                impact(d, t - FLIGHT, s);
            }
        }

        private void flight(VfxDraw d, float t, float s) {
            Vec3 head = headAt(t);
            Vec3 dir = target.subtract(origin).normalize();
            double trail = 34.0D * s;

            // cercle d'avertissement au sol, de plus en plus vif
            d.type(VfxRenderTypes.LINE_ADD);
            float warn = Rnd.ramp(t, 0.0F, FLIGHT) * (0.5F + 0.5F * (float) Math.sin(t * 0.8F));
            d.ring(target.add(0, 0.12D, 0), AXIS_X, AXIS_Z, 9.0D * s, 0.5D, 0xFF3A1A, 0.55F * warn, 56);
            d.ring(target.add(0, 0.12D, 0), AXIS_X, AXIS_Z, 9.0D * s * (1.0F - Rnd.ramp(t, 0.0F, FLIGHT)), 0.35D,
                    0xFFB050, 0.7F * warn, 48);

            // traine : deux couches de rubans
            int seg = 14;
            for (int layer = 0; layer < 2; layer++) {
                for (int i = 0; i < seg; i++) {
                    float k0 = i / (float) seg;
                    float k1 = (i + 1) / (float) seg;
                    Vec3 a = head.subtract(dir.scale(k0 * trail)).add(wobble(i, t, s));
                    Vec3 b = head.subtract(dir.scale(k1 * trail)).add(wobble(i + 1, t, s));
                    float w0 = (3.4F * s * (1.0F - k0) + 0.25F) * (layer == 0 ? 2.4F : 1.0F);
                    float w1 = (3.4F * s * (1.0F - k1) + 0.25F) * (layer == 0 ? 2.4F : 1.0F);
                    int rgb = layer == 0 ? 0xFF4A10 : (k0 < 0.3F ? 0xFFE0A0 : 0xFF8A2A);
                    float mul = layer == 0 ? 0.35F : 0.95F;
                    d.ribbon(a, b, w0, w1, rgb, (1.0F - k0) * mul, (1.0F - k1) * mul);
                }
            }

            d.type(VfxRenderTypes.DISC_DARK);
            d.glow(head, 3.0F * s, 0x1E1410, 1.0F);

            d.type(VfxRenderTypes.GLOW_ADD);
            d.glow(head, 9.0F * s, 0xFF6A1A, 0.55F);
            d.glow(head, 5.0F * s, 0xFFC06A, 0.85F);
            d.glow(head, 2.2F * s, 0xFFFFFF, 1.0F);

            d.type(VfxRenderTypes.STAR_ADD);
            for (int i = 0; i < 36; i++) {
                float lag = Rnd.f(seed, i) * 0.9F;
                Vec3 ep = head.subtract(dir.scale(lag * trail))
                        .add(sphere(seed, i + 50).scale(Rnd.f(seed, i + 90) * 1.8D * s * (lag + 0.3F)));
                d.glow(ep, 0.45F * s * (1.0F - lag * 0.5F), t * 0.3F + i, i % 2 == 0 ? 0xFFB050 : 0xFF6A1A,
                        (1.0F - lag) * 0.9F);
            }
        }

        private Vec3 wobble(int i, float t, float s) {
            double a = Math.sin(t * 0.5D + i * 1.3D) * 0.35D * s * i * 0.12D;
            double b = Math.cos(t * 0.43D + i * 0.9D) * 0.35D * s * i * 0.12D;
            return new Vec3(a, b, a * 0.5D - b * 0.5D);
        }

        private Vec3 debris(int i, float ts, float s) {
            Vec3 dir = sphere(seed, i);
            dir = new Vec3(dir.x, Math.abs(dir.y) * 0.9D + 0.25D, dir.z).normalize();
            double speed = (6.0D + Rnd.f(seed, i + 500) * 18.0D) * Math.sqrt(s);
            return target.add(dir.scale(speed * ts)).add(0, -5.0D * ts * ts, 0);
        }

        private void impact(VfxDraw d, float ti, float s) {
            Vec3 g = target;
            float ts = ti / 20.0F;

            d.type(VfxRenderTypes.GLOW_DARK);
            for (int i = 0; i < 22; i++) {
                double ang = Rnd.f(seed, i + 700) * Math.PI * 2.0D;
                double rad = Rnd.f(seed, i + 720) * 5.0D * s * Math.min(1.0F, ts * 2.5F);
                Vec3 p = g.add(Math.cos(ang) * rad, ts * Rnd.range(seed, i + 740, 2.0F, 5.0F) * s + 1.0D,
                        Math.sin(ang) * rad);
                float a = 0.5F * Rnd.ramp(ti, 0.0F, 4.0F) * (1.0F - Rnd.ramp(ti, 10.0F, 58.0F));
                d.glow(p, (4.0F + ts * 6.0F) * s, 0x2A2420, a);
            }

            d.type(VfxRenderTypes.GLOW_ADD);
            float fb = easeOut(ti / 5.0F) * (1.0F - Rnd.ramp(ti, 3.0F, 26.0F));
            Vec3 core = g.add(0, 3.0D * s * easeOut(ti / 18.0F), 0);
            d.glow(core, 26.0F * s * fb, 0xFF6A1A, 0.8F * fb);
            d.glow(core, 14.0F * s * fb, 0xFFD27A, 0.9F * fb);
            float white = 1.0F - Rnd.ramp(ti, 0.0F, 8.0F);
            d.glow(core, 8.0F * s * white, 0xFFFFFF, white);

            d.type(VfxRenderTypes.LINE_ADD);
            float ringAlpha = (1.0F - Rnd.ramp(ti, 6.0F, 34.0F)) * 0.9F;
            d.ring(g.add(0, 0.15D, 0), AXIS_X, AXIS_Z, 14.0D * s * easeOut(ti / 26.0F),
                    3.2D * s * (1.0F - ti / 40.0F) + 0.4D, 0xFFB050, ringAlpha, 64);
            float t2 = ti - 4.0F;
            if (t2 > 0.0F) {
                d.ring(g.add(0, 0.15D, 0), AXIS_X, AXIS_Z, 11.0D * s * easeOut(t2 / 26.0F),
                        2.4D * s * (1.0F - t2 / 40.0F) + 0.3D, 0xFF5A20, (1.0F - Rnd.ramp(t2, 6.0F, 34.0F)) * 0.8F, 64);
            }
            d.pillar(g, 38.0D * s, 6.0F * s * (1.0F - Rnd.ramp(ti, 0.0F, 22.0F)), 0xFF7A2A,
                    0.8F * (1.0F - Rnd.ramp(ti, 0.0F, 22.0F)), 0.0F);
            for (int i = 0; i < 90; i++) {
                float life = 0.7F + Rnd.f(seed, i + 600) * 1.1F;
                if (ts > life) {
                    continue;
                }
                float k = 1.0F - ts / life;
                d.ribbon(debris(i, Math.max(0.0F, ts - 0.06F), s), debris(i, ts, s), 0.0F, 0.22F * s, 0xFF9A3A, 0.0F, k);
            }

            d.type(VfxRenderTypes.STAR_ADD);
            for (int i = 0; i < 90; i++) {
                float life = 0.7F + Rnd.f(seed, i + 600) * 1.1F;
                if (ts > life) {
                    continue;
                }
                float k = 1.0F - ts / life;
                d.glow(debris(i, ts, s), 0.6F * s * k, ti * 0.2F + i, i % 3 == 0 ? 0xFFFFFF : 0xFFB050, k);
            }
        }

        @Override
        public float shake(Vec3 cam, float t) {
            if (t < FLIGHT) {
                return 0.15F * Rnd.ramp(t, FLIGHT - 14.0F, FLIGHT) * falloff(cam, target, 80.0F);
            }
            return 1.0F * param * (1.0F - Rnd.ramp(t - FLIGHT, 0.0F, 26.0F)) * falloff(cam, target, 100.0F);
        }

        @Override
        public float flash(Vec3 cam, float t) {
            return t < FLIGHT ? 0.0F
                    : 0.85F * param * (1.0F - Rnd.ramp(t - FLIGHT, 0.0F, 8.0F)) * falloff(cam, target, 50.0F);
        }

        @Override
        public int flashColor() {
            return 0xFFC080;
        }
    }

    /** Renaissance du phenix : ailes de flammes, helices de feu, onde de choc et braises. */
    public static final class Phoenix extends Fx {
        public Phoenix(VfxPacket p) {
            super(p);
        }

        private float presence(float t) {
            return Rnd.smooth(Rnd.ramp(t, 0.0F, 10.0F)) * (1.0F - Rnd.smooth(Rnd.ramp(t, duration - 24.0F, duration)));
        }

        @Override
        public void render(VfxDraw d, float t, float partial) {
            Entity e = followed();
            Vec3 p = pos(partial);
            float grow = presence(t);
            float s = t / 20.0F;
            double height = e != null ? e.getBbHeight() : 1.8D;

            float flap = (float) Math.sin(t * 0.42F) * 0.38F * (1.0F - 0.65F * Rnd.ramp(t, 30.0F, 70.0F));
            FxWings.wings(d, p.add(0, height * 0.75D, 0), FxWings.bodyYaw(e, partial), 9, 4.0F, flap, grow,
                    0xFF4A1A, 0xFFC040, 0.9F * grow, t);

            d.type(VfxRenderTypes.LINE_ADD);
            for (int k = 0; k < 2; k++) {
                float tk = t - k * 4.0F;
                if (tk <= 0.0F) {
                    continue;
                }
                d.ring(p.add(0, 0.1D, 0), AXIS_X, AXIS_Z, 1.0D + 9.0D * easeOut(tk / 20.0F),
                        1.6D * (1.0F - tk / 34.0F) + 0.3D, k == 0 ? 0xFFB050 : 0xFF5A20,
                        (1.0F - Rnd.ramp(tk, 4.0F, 26.0F)) * 0.9F, 56);
            }

            // trois helices de flammes autour du joueur
            d.type(VfxRenderTypes.GLOW_ADD);
            for (int h = 0; h < 3; h++) {
                for (int j = 0; j < 26; j++) {
                    float phase = j / 26.0F;
                    double ang = h * 2.094D + phase * 7.0D + t * 0.25D;
                    double rad = 1.1D + 0.5D * phase;
                    Vec3 hp = p.add(Math.cos(ang) * rad, phase * (height + 2.4D), Math.sin(ang) * rad);
                    d.glow(hp, 0.9F * (1.0F - phase * 0.55F), phase < 0.4F ? 0xFFB050 : 0xFF5A20,
                            (1.0F - phase * 0.7F) * grow * 0.75F);
                }
            }
            float burst = 1.0F - Rnd.ramp(t, 0.0F, 20.0F);
            d.glow(p.add(0, height * 0.55D, 0), 7.0F * (burst + 0.25F), 0xFF8A2A, 0.55F * Math.max(burst, 0.3F * grow));
            d.glow(p.add(0, height * 0.55D, 0), 3.0F * (burst + 0.2F), 0xFFE0A0, 0.8F * burst);

            // braises montantes
            d.type(VfxRenderTypes.STAR_ADD);
            for (int i = 0; i < 70; i++) {
                float life = 1.2F + Rnd.f(seed, i) * 1.0F;
                float phase = fract(s / life + Rnd.f(seed, i + 100));
                double ang = Rnd.f(seed, i + 200) * Math.PI * 2.0D;
                double rad = 0.5D + Rnd.f(seed, i + 300) * 2.2D;
                Vec3 ep = p.add(Math.cos(ang) * rad + Math.sin(phase * 7.0F + i) * 0.3D,
                        phase * (3.0D + Rnd.f(seed, i + 400) * 5.0D),
                        Math.sin(ang) * rad + Math.cos(phase * 6.0F + i) * 0.3D);
                d.glow(ep, 0.38F * (1.0F - phase * 0.5F), s * 3.0F + i, i % 3 == 0 ? 0xFFE0A0 : 0xFF7A2A,
                        (float) Math.sin(phase * Math.PI) * grow);
            }
        }

        @Override
        public float shake(Vec3 cam, float t) {
            return 0.4F * (1.0F - Rnd.ramp(t, 0.0F, 15.0F)) * falloff(cam, pos(0.0F), 30.0F);
        }

        @Override
        public float flash(Vec3 cam, float t) {
            return 0.55F * (1.0F - Rnd.ramp(t, 0.0F, 8.0F)) * falloff(cam, pos(0.0F), 30.0F);
        }

        @Override
        public int flashColor() {
            return 0xFFB060;
        }
    }

    private FxFire() {}
}
