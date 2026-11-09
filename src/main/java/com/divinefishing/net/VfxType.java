package com.divinefishing.net;

/** Identifiants des effets visuels envoyes du serveur vers les clients. */
public enum VfxType {
    /** Pilier de lumiere quand un item divin est peche. */
    CATCH,
    /** Foudre divine (eclair + onde de choc). */
    SMITE,
    /** Arc electrique entre deux points. */
    ARC,
    /** Petit coup de foudre sur un coup d'epee. */
    MINI_STRIKE,
    /** Trou noir. */
    BLACK_HOLE,
    /** Effondrement du trou noir. */
    COLLAPSE,
    /** Arret du temps. */
    TIME_STOP,
    /** Meteore (vol + impact). */
    METEOR,
    /** Renaissance du phenix. */
    PHOENIX,
    /** Ailes d'ange. */
    ANGEL,
    /** Supprime les effets d'un type donne attaches a une entite. */
    CANCEL;

    public static VfxType byId(int id) {
        VfxType[] all = values();
        return id >= 0 && id < all.length ? all[id] : CANCEL;
    }
}
