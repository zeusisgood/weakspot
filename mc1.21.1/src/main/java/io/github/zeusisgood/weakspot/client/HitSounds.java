package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.HitPitch;
import io.github.zeusisgood.weakspot.common.HitScale;
import io.github.zeusisgood.weakspot.common.HitSound;
import io.github.zeusisgood.weakspot.config.ClientConfig;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * どれも「プレイヤー」のカテゴリで鳴らすので、バニラの「プレイヤー」音量が掛かる。
 * 駆け上がりが鳴っている間は、自分の通常のヒット音を小さくする（埋もれないように）。
 */
@EventBusSubscriber(modid = WeakSpotMod.MODID, value = Dist.CLIENT)
final class HitSounds {

    /** delay が 0 になった tick に鳴らす。一時停止中も進める（統計画面の試聴のため）。 */
    private static final List<Scheduled> QUEUE = new ArrayList<>();
    /** 旋律に対する倍率。 */
    private static final float CHORD_VOLUME = 0.6F;
    private static final int RESOLVE_DELAY_TICKS = 2;
    /** 自分のヒット音の音量に掛ける。 */
    private static final double BELL_VOLUME = 0.5;
    /** 自分のヒット音の音量に対する倍率。上限 1.0。 */
    private static final float ACCENT_VOLUME = 1.5F;
    private static final float DUCKED_VOLUME = 0.3F;
    private static final int DUCK_TAIL_TICKS = 2;
    /** 自分のヒット音の音量に対する倍率と、2 音の間の tick。 */
    private static final float BREAK_VOLUME = 0.5F;
    private static final int BREAK_GAP_TICKS = 2;

    /** 予約の音と同じく一時停止中も進む。 */
    private static long soundTick;
    private static long duckUntil;

    private HitSounds() {
    }

    private static final class Scheduled {
        int delay;
        final Runnable sound;

        Scheduled(int delay, Runnable sound) {
            this.delay = delay;
            this.sound = sound;
        }
    }

    /** streak は 1 始まり。 */
    static void playHit(int streak) {
        double volume = WeakSpotConfig.client.sound.myHitVolume * (soundTick <= duckUntil ? DUCKED_VOLUME : 1);
        boolean chords = WeakSpotConfig.client.sound.hitChordEnabled;
        HitScale scale = scale();
        float[] pitches = scale.forHit(streak, chords);
        // 重ねる音が多いほど 1 音を小さくする（9 の和音などで大きくなりすぎないように）
        double chordVolume = volume * CHORD_VOLUME * Math.min(1, 2.0 / Math.max(1, pitches.length - 1));
        for (int i = 0; i < pitches.length; i++) {
            playFlatPitch(WeakSpotConfig.client.sound.myHitSound, i == 0 ? volume : chordVolume, pitches[i]);
        }
        float resolve = scale.resolveFor(streak, chords);
        if (resolve > 0) {
            schedule(RESOLVE_DELAY_TICKS,
                    () -> playFlatPitch(WeakSpotConfig.client.sound.myHitSound, chordVolume, resolve));
        }
        if (HitPitch.withBell(streak, chords)) {
            playFlatPitch(HitSound.BELL, volume * BELL_VOLUME, pitches[0]);
        }
    }

    /** 試聴と、統計画面の音の確かめ。 */
    static void playOwn(int streak) {
        playFlat(WeakSpotConfig.client.sound.myHitSound, WeakSpotConfig.client.sound.myHitVolume, streak);
    }

    static void playAccent(float pitch) {
        playFlatPitch(HitSound.BELL, Math.min(1.0, WeakSpotConfig.client.sound.myHitVolume * ACCENT_VOLUME), pitch);
    }

    static void accentRun(float[] pitches, int ticksPerNote, int startDelay) {
        for (int i = 0; i < pitches.length; i++) {
            float pitch = pitches[i];
            schedule(startDelay + i * ticksPerNote, () -> playAccent(pitch));
        }
        long end = soundTick + startDelay + (long) (pitches.length - 1) * ticksPerNote + DUCK_TAIL_TICKS;
        duckUntil = Math.max(duckUntil, end);
    }

    static void accentScale(int ticksPerNote, int startDelay) {
        accentRun(scale().oneOctave(), ticksPerNote, startDelay);
    }

    /** 設定が変わったときだけ作り直す。 */
    private static HitScale cachedScale = HitScale.DEFAULT;

    static HitScale scale() {
        ClientConfig.Sound sound = WeakSpotConfig.client.sound;
        HitScale scale = cachedScale;
        if (scale.type != sound.hitScaleType || scale.direction != sound.hitScaleDirection
                || scale.octaves != sound.hitScaleOctaves) {
            scale = new HitScale(sound.hitScaleType, sound.hitScaleDirection, sound.hitScaleOctaves);
            cachedScale = scale;
        }
        return scale;
    }

    static void playBreak() {
        float[] notes = scale().breakNotes();
        double volume = WeakSpotConfig.client.sound.myHitVolume * BREAK_VOLUME;
        for (int i = 0; i < notes.length; i++) {
            float pitch = notes[i];
            schedule(i * BREAK_GAP_TICKS, () -> playFlatPitch(WeakSpotConfig.client.sound.myHitSound, volume, pitch));
        }
    }

    static void playOther(BlockPos pos, int streak) {
        float volume = (float) WeakSpotConfig.client.sound.othersHitVolume;
        if (volume <= 0 || Minecraft.getInstance().level == null) {
            return;
        }
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(
                soundOf(WeakSpotConfig.client.sound.othersHitSound).value(), SoundSource.PLAYERS, volume,
                scale().melody(streak), SoundInstance.createUnseededRandom(), pos));
    }

    /** 本人と同じ低い音。 */
    static void playOtherMiss(BlockPos pos) {
        float volume = (float) WeakSpotConfig.client.sound.othersHitVolume;
        if (volume <= 0 || Minecraft.getInstance().level == null) {
            return;
        }
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(
                SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, volume, TargetPlay.MISS_PITCH,
                SoundInstance.createUnseededRandom(), pos));
    }

    /** 実際は距離で小さくなるが、試聴は距離なしで鳴らす。 */
    static void playOtherFlat(int streak) {
        playFlat(WeakSpotConfig.client.sound.othersHitSound, WeakSpotConfig.client.sound.othersHitVolume, streak);
    }

    private static void playFlat(HitSound sound, double volume, int streak) {
        playFlatPitch(sound, volume, scale().melody(streak));
    }

    private static void playFlatPitch(HitSound sound, double volume, float pitch) {
        if (volume <= 0) {
            return;
        }
        // 聞き手からの相対位置 (0, 0, 0) に置くので、左右に振れない
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(
                soundOf(sound).value().getLocation(), SoundSource.PLAYERS, (float) volume, pitch,
                SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true));
    }

    /** note は連続ヒット数を受け取って 1 音鳴らす。 */
    static void playScale(IntConsumer note, int ticksPerNote, int startDelay) {
        playScale(note, ticksPerNote, startDelay, scale().cycleLength());
    }

    static void playScale(IntConsumer note, int ticksPerNote, int startDelay, int count) {
        for (int i = 0; i < count; i++) {
            int streak = i + 1;
            schedule(startDelay + i * ticksPerNote, () -> note.accept(streak));
        }
    }

    static void schedule(int delayTicks, Runnable sound) {
        if (delayTicks <= 0) {
            sound.run();
        } else {
            QUEUE.add(new Scheduled(delayTicks, sound));
        }
    }

    static void clear() {
        QUEUE.clear();
        duckUntil = 0;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        soundTick++;
        if (QUEUE.isEmpty()) {
            return;
        }
        List<Runnable> due = new ArrayList<>();
        for (Iterator<Scheduled> it = QUEUE.iterator(); it.hasNext(); ) {
            Scheduled s = it.next();
            if (--s.delay <= 0) {
                due.add(s.sound);
                it.remove();
            }
        }
        due.forEach(Runnable::run);
    }

    private static Holder<SoundEvent> soundOf(HitSound sound) {
        switch (sound) {
            case CHIME:
                return SoundEvents.NOTE_BLOCK_CHIME;
            case BELL:
                return SoundEvents.NOTE_BLOCK_BELL;
            case FLUTE:
                return SoundEvents.NOTE_BLOCK_FLUTE;
            case GUITAR:
                return SoundEvents.NOTE_BLOCK_GUITAR;
            case HARP:
                return SoundEvents.NOTE_BLOCK_HARP;
            case BASS:
                return SoundEvents.NOTE_BLOCK_BASS;
            case HAT:
                return SoundEvents.NOTE_BLOCK_HAT;
            case SNARE:
                return SoundEvents.NOTE_BLOCK_SNARE;
            case BASEDRUM:
                return SoundEvents.NOTE_BLOCK_BASEDRUM;
            case PLING:
                return SoundEvents.NOTE_BLOCK_PLING;
            case XYLOPHONE:
            default:
                return SoundEvents.NOTE_BLOCK_XYLOPHONE;
        }
    }
}
