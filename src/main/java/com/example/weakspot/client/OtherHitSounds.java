package com.example.weakspot.client;

import com.example.weakspot.common.HitPitch;
import com.example.weakspot.config.OtherHitSound;
import com.example.weakspot.config.WeakSpotConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;

/** 他のプレイヤーのヒット音。叩かれたブロックの位置から、距離で小さくなる立体音で鳴らす。 */
final class OtherHitSounds {

    private OtherHitSounds() {
    }

    static void play(BlockPos pos, int streak) {
        float volume = (float) WeakSpotConfig.othersHitVolume;
        if (volume <= 0 || Minecraft.getMinecraft().world == null) {
            return;
        }
        Minecraft.getMinecraft().getSoundHandler().playSound(new PositionedSoundRecord(
                soundOf(WeakSpotConfig.othersHitSound), SoundCategory.PLAYERS, volume,
                HitPitch.forStreak(streak), pos));
    }

    private static SoundEvent soundOf(OtherHitSound sound) {
        switch (sound) {
            case CHIME:
                return SoundEvents.BLOCK_NOTE_CHIME;
            case BELL:
                return SoundEvents.BLOCK_NOTE_BELL;
            case FLUTE:
                return SoundEvents.BLOCK_NOTE_FLUTE;
            case GUITAR:
                return SoundEvents.BLOCK_NOTE_GUITAR;
            case HARP:
                return SoundEvents.BLOCK_NOTE_HARP;
            case BASS:
                return SoundEvents.BLOCK_NOTE_BASS;
            case HAT:
                return SoundEvents.BLOCK_NOTE_HAT;
            case SNARE:
                return SoundEvents.BLOCK_NOTE_SNARE;
            case BASEDRUM:
                return SoundEvents.BLOCK_NOTE_BASEDRUM;
            case PLING:
                return SoundEvents.BLOCK_NOTE_PLING;
            case XYLOPHONE:
            default:
                return SoundEvents.BLOCK_NOTE_XYLOPHONE;
        }
    }
}
