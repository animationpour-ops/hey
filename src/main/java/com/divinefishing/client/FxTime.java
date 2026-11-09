package com.divinefishing.client;

import com.divinefishing.net.VfxPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

/** Arret du temps : dome dore, cadran au sol, poussiere figee dans l'air, teinte d'ecran. */
public final class FxTime {
    private static final int GOLD = 0xFFD86B;
    private static final int PALE = 0xFFF3C4;

    public static final class TimeStop extends Fx {
        public TimeStop(VfxPacket p) {
            super(p);
        }

        private float radius() {
            return param > 1.0F ? param : 40.0F;
        }

        /** 0 -> 1 en entrant, 1 -> 0 en sortant. */
        private float presence(float t) {
            return Rnd.smooth(Rnd.ramp(t, 0.0F, 12.0F)) * (1.0F - Rnd.smooth(Rnd.ramp(t, duration - 16.0F, duration)));
        }

        @Override
        public void render(VfxDraw d, float t, float partial) {
            Vec3 c = pos(partial);
            float presence = presence(t);
            float R = radius();

            // --- onde de choc d'entree : un dome fait de cercles de latitude qui s'etend
            float wave = easeOut(t / 24.0F);
            float waveAlpha = (1.0F - Rnd.ramp(t, 6.0F, 40.0F)) * 0.85F;
            // --- onde de sortie : le dome se contracte
            float back = Rnd.ramp(t, duration - 14.0F, duration);

            d.type(VfxRenderTypes.LINE_ADD);
            if (waveAlpha > 0.01F) {
                double rad = R * wave;
                for (int k = -3; k <= 3; k++) {
                    double h = rad * k / 3.6D;
                    double rr = Math.sqrt(Math.max(0.0D, rad * rad - h * h));
                    d.ring(c.add(0, h + 1.0D, 0), AXIS_X, AXIS_Z, rr, 1.0D + rad * 0.02D, GOLD, waveAlpha * (1.0F - Math.abs(k) * 0.12F), 64);
                }
                d.ring(c.add(0, 1.0D, 0), AXIS_X, AXIS_Y, rad, 1.2D, PALE, waveAlpha * 0.6F, 64);
                d.ring(c.add(0, 1.0D, 0), AXIS_Z, AXIS_Y, rad, 1.2D, PALE, waveAlpha * 0.6F, 64);
            }
            if (back > 0.0F && back < 1.0F) {
                double rad = R * (1.0F - easeIn(back));
                d.ring(c.add(0, 1.0D, 0), AXIS_X, AXIS_Z, rad, 1.4D, GOLD, 0.8F * (1.0F - back), 64);
                d.ring(c.add(0, 1.0D, 0), AXIS_X, AXIS_Y, rad, 1.4D, PALE, 0.5F * (1.0F - back), 64);
            }

            // --- cadran au sol : cercle, 12 graduations, deux aiguilles qui tournent
            Vec3 floor = c.add(0, 0.07D, 0);
            float spin = t * 0.045F;
            d.ring(floor, AXIS_X, AXIS_Z, 5.2D, 0.35D, GOLD, 0.9F * presence, 72);
            d.ring(floor, AXIS_X, AXIS_Z, 4.4D, 0.18D, PALE, 0.55F * presence, 72);
            d.ring(floor, AXIS_X, AXIS_Z, 1.1D, 0.30D, GOLD, 0.7F * presence, 40);
            for (int i = 0; i < 12; i++) {
                double ang = i * Math.PI / 6.0D + spin * 0.2D;
                Vec3 dir = new Vec3(Math.cos(ang), 0, Math.sin(ang));
                boolean major = i % 3 == 0;
                d.ribbon(floor.add(dir.scale(major ? 3.7D : 4.1D)), floor.add(dir.scale(4.45D)),
                        major ? 0.32F : 0.18F, major ? 0.32F : 0.18F, PALE, 0.85F * presence, 0.85F * presence);
            }
            double minuteAng = -t * 0.28D;
            double hourAng = -t * 0.28D / 12.0D + 1.0D;
            Vec3 minute = new Vec3(Math.cos(minuteAng), 0, Math.sin(minuteAng));
            Vec3 hour = new Vec3(Math.cos(hourAng), 0, Math.sin(hourAng));
            d.ribbon(floor, floor.add(minute.scale(3.9D)), 0.34F, 0.12F, 0xFFFFFF, presence, 0.9F * presence);
            d.ribbon(floor, floor.add(hour.scale(2.7D)), 0.46F, 0.18F, GOLD, presence, 0.9F * presence);

            d.type(VfxRenderTypes.GLOW_ADD);
            d.glow(floor.add(0, 0.2D, 0), 3.0F, GOLD, 0.5F * presence);
            float flare = 1.0F - Rnd.ramp(t, 0.0F, 14.0F);
            d.glow(c.add(0, 1.0D, 0), 22.0F * flare, PALE, 0.8F * flare);

            // --- poussiere doree figee : elle scintille mais ne bouge plus, comme le reste du monde
            d.type(VfxRenderTypes.STAR_ADD);
            float seconds = t / 20.0F;
            for (int i = 0; i < 90; i++) {
                Vec3 dir = sphere(seed, i);
                double dist = 3.0D + Math.cbrt(Rnd.f(seed, i + 900)) * Math.min(R * 0.55D, 22.0D);
                Vec3 p = c.add(0, 1.0D, 0).add(dir.scale(dist));
                float tw = 0.5F + 0.5F * (float) Math.sin(seconds * (1.5F + Rnd.f(seed, i + 950) * 2.5F) + i);
                float appear = Rnd.ramp(t, 4.0F + Rnd.f(seed, i + 980) * 14.0F, 20.0F + Rnd.f(seed, i + 980) * 14.0F);
                d.glow(p, 0.35F + 0.25F * tw, i * 0.7F, i % 3 == 0 ? 0xFFFFFF : GOLD, tw * appear * presence);
            }
        }

        @Override
        public float shake(Vec3 cam, float t) {
            return 0.35F * (1.0F - Rnd.ramp(t, 0.0F, 14.0F)) * falloff(cam, origin, 40.0F);
        }

        @Override
        public float flash(Vec3 cam, float t) {
            return 0.75F * (1.0F - Rnd.ramp(t, 0.0F, 7.0F)) * falloff(cam, origin, 40.0F);
        }

        @Override
        public int flashColor() {
            return 0xFFF3C4;
        }

        @Override
        public void overlay(GuiGraphics g, int w, int h, float t, Vec3 cam) {
            double dist = cam.distanceTo(pos(0.0F));
            if (dist > radius()) {
                return;
            }
            float inside = presence(t);
            float pulse = 0.85F + 0.15F * (float) Math.sin(t * 0.18F);
            int tint = (int) (0.17F * inside * pulse * 255.0F);
            if (tint > 0) {
                g.fill(0, 0, w, h, (tint << 24) | 0x8A7A55);
            }
            // vignette sombre sur les bords
            float edge = 0.55F * inside;
            int th = Math.max(2, Math.min(w, h) / 28);
            for (int i = 0; i < 6; i++) {
                int a = (int) (edge * (1.0F - i / 6.0F) * 0.6F * 255.0F);
                if (a <= 0) {
                    continue;
                }
                int in = i * th;
                int col = (a << 24) | 0x1A1405;
                g.fill(in, in, w - in, in + th, col);
                g.fill(in, h - in - th, w - in, h - in, col);
                g.fill(in, in + th, in + th, h - in - th, col);
                g.fill(w - in - th, in + th, w - in, h - in - th, col);
            }
        }
    }

    private FxTime() {}
}
