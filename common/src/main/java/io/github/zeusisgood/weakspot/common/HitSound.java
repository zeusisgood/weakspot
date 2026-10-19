package io.github.zeusisgood.weakspot.common;

/** ノートブロックの楽器。音の対応はクライアント側（HitSounds）。 */
public enum HitSound {
    XYLOPHONE,
    CHIME,
    BELL,
    FLUTE,
    GUITAR,
    HARP,
    BASS,
    HAT,
    SNARE,
    BASEDRUM,
    PLING;

    public HitSound next() {
        HitSound[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
