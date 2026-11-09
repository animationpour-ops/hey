package com.divinefishing.client;

import com.divinefishing.net.VfxPacket;
import com.divinefishing.net.VfxType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;

/** Liste des effets actifs cote client. */
public final class VfxManager {
    public static final List<Fx> ACTIVE = new ArrayList<>();

    public static void receive(VfxPacket p) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        switch (p.type) {
            case CANCEL -> {
                VfxType wanted = VfxType.byId((int) p.param);
                for (Fx fx : ACTIVE) {
                    if (fx.entityId == p.entityId && fx.type == wanted) {
                        fx.age = fx.duration;
                    }
                }
            }
            case ANGEL -> {
                for (Fx fx : ACTIVE) {
                    if (fx.type == VfxType.ANGEL && fx.entityId == p.entityId) {
                        fx.age = fx.duration;
                    }
                }
                ACTIVE.add(new FxWings.Angel(p));
            }
            case CATCH -> ACTIVE.add(new FxLight.Catch(p));
            case SMITE -> ACTIVE.add(new FxLight.Smite(p));
            case ARC -> ACTIVE.add(new FxLight.Arc(p));
            case MINI_STRIKE -> ACTIVE.add(new FxLight.Mini(p));
            case BLACK_HOLE -> ACTIVE.add(new FxVoid.BlackHole(p));
            case COLLAPSE -> ACTIVE.add(new FxVoid.Collapse(p));
            case TIME_STOP -> ACTIVE.add(new FxTime.TimeStop(p));
            case METEOR -> ACTIVE.add(new FxFire.Meteor(p));
            case PHOENIX -> ACTIVE.add(new FxFire.Phoenix(p));
        }
    }

    public static void tick() {
        if (Minecraft.getInstance().level == null) {
            ACTIVE.clear();
            return;
        }
        if (Minecraft.getInstance().isPaused()) {
            return;
        }
        Iterator<Fx> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Fx fx = it.next();
            fx.age++;
            if (fx.finished()) {
                it.remove();
            }
        }
    }

    private VfxManager() {}
}
