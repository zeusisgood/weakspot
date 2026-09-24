package com.example.weakspot.config;

/** ヒット音に使うノートブロックの楽器（自分・他のプレイヤー共通）。音の対応はクライアント側（HitSounds）で行う。 */
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

    /** 統計画面の切り替えボタン用。最後の次は最初に戻る。 */
    public HitSound next() {
        HitSound[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
