package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.HitPitch;
import io.github.zeusisgood.weakspot.config.HitSound;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * ヒット音。楽器と音量は各自の設定（myHitSound / myHitVolume、othersHitSound / othersHitVolume）。
 * どれも「プレイヤー」のカテゴリで鳴らすので、バニラの「プレイヤー」音量が掛かる。
 * 自分の音と試聴は距離なし、他のプレイヤーの音は叩かれたブロックの位置から距離で小さくなる。
 * 1.9.5: 自分のヒット音はコンボで和音が厚くなる（HitPitch.forHit。hitChordEnabled）。コンボの段階と累計の節目の音は
 * ベルで大きめに鳴らし（accent）、駆け上がりが鳴っている間は自分の通常のヒット音を小さくする（埋もれないように）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
final class HitSounds {

    /** 予約した音。delay が 0 になった tick に鳴らす。一時停止中も進める（統計画面の試聴のため）。 */
    private static final List<Scheduled> QUEUE = new ArrayList<>();
    /** 重ねる音の音量（旋律に対する倍率）。 */
    private static final float CHORD_VOLUME = 0.6F;
    /** 段階・節目の音（ベル）の音量（自分のヒット音の音量に対する倍率。上限 1.0）。 */
    private static final float ACCENT_VOLUME = 1.5F;
    /** 駆け上がりの間の、自分の通常のヒット音の音量の倍率。 */
    private static final float DUCKED_VOLUME = 0.3F;
    /** 駆け上がりの最後の音のあと、ヒット音を小さくしたままにする tick。 */
    private static final int DUCK_TAIL_TICKS = 2;
    /** コンボが途切れた音の音量（自分のヒット音の音量に対する倍率）と、2 音の間の tick。 */
    private static final float BREAK_VOLUME = 0.5F;
    private static final int BREAK_GAP_TICKS = 2;

    /** この音の時計（予約の音と同じく一時停止中も進む）。 */
    private static long soundTick;
    /** この tick まで、自分の通常のヒット音を小さくする（駆け上がりの間）。 */
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

    /** 自分のヒット音。streak は連続ヒット数（1 始まり）。コンボで和音が厚くなり、駆け上がりの間は小さくなる。 */
    static void playHit(int streak) {
        double volume = WeakSpotConfig.client.sound.myHitVolume * (soundTick <= duckUntil ? DUCKED_VOLUME : 1);
        float[] pitches = HitPitch.forHit(streak, WeakSpotConfig.client.sound.hitChordEnabled);
        for (int i = 0; i < pitches.length; i++) {
            playFlatPitch(WeakSpotConfig.client.sound.myHitSound, i == 0 ? volume : volume * CHORD_VOLUME, pitches[i]);
        }
    }

    /** 自分のヒット音の楽器で、旋律の 1 音だけを鳴らす（試聴と、統計画面の音の確かめ）。 */
    static void playOwn(int streak) {
        playFlat(WeakSpotConfig.client.sound.myHitSound, WeakSpotConfig.client.sound.myHitVolume, streak);
    }

    /** 段階・節目の音（ベル、大きめ）。 */
    static void playAccent(float pitch) {
        playFlatPitch(HitSound.BELL, Math.min(1.0, WeakSpotConfig.client.sound.myHitVolume * ACCENT_VOLUME), pitch);
    }

    /**
     * 段階・節目の音を、startDelay tick 後から ticksPerNote ごとに鳴らす（駆け上がり・分散和音）。
     * 鳴っている間（最後の音の少しあとまで）は、自分の通常のヒット音を小さくする。
     */
    static void accentRun(float[] pitches, int ticksPerNote, int startDelay) {
        for (int i = 0; i < pitches.length; i++) {
            float pitch = pitches[i];
            schedule(startDelay + i * ticksPerNote, () -> playAccent(pitch));
        }
        long end = soundTick + startDelay + (long) (pitches.length - 1) * ticksPerNote + DUCK_TAIL_TICKS;
        duckUntil = Math.max(duckUntil, end);
    }

    /** 音階（下のドから上のドまで）を、段階・節目の音で鳴らす。 */
    static void accentScale(int ticksPerNote, int startDelay) {
        float[] pitches = new float[HitPitch.SCALE_LENGTH];
        for (int i = 0; i < pitches.length; i++) {
            pitches[i] = HitPitch.forStreak(i + 1);
        }
        accentRun(pitches, ticksPerNote, startDelay);
    }

    /** 自分のコンボが途切れた音（下がる 2 音。自分のヒット音の楽器で小さめ）。 */
    static void playBreak() {
        float[] notes = HitPitch.breakNotes();
        double volume = WeakSpotConfig.client.sound.myHitVolume * BREAK_VOLUME;
        for (int i = 0; i < notes.length; i++) {
            float pitch = notes[i];
            schedule(i * BREAK_GAP_TICKS, () -> playFlatPitch(WeakSpotConfig.client.sound.myHitSound, volume, pitch));
        }
    }

    /** 他のプレイヤーのヒット音。叩かれたブロックの位置から鳴らす。 */
    static void playOther(BlockPos pos, int streak) {
        float volume = (float) WeakSpotConfig.client.sound.othersHitVolume;
        if (volume <= 0 || Minecraft.getMinecraft().world == null) {
            return;
        }
        Minecraft.getMinecraft().getSoundHandler().playSound(new PositionedSoundRecord(
                soundOf(WeakSpotConfig.client.sound.othersHitSound), SoundCategory.PLAYERS, volume,
                HitPitch.forStreak(streak), pos));
    }

    /** 他のプレイヤーのヒット音の試聴。実際は距離で小さくなるが、試聴は距離なしで鳴らす。 */
    static void playOtherFlat(int streak) {
        playFlat(WeakSpotConfig.client.sound.othersHitSound, WeakSpotConfig.client.sound.othersHitVolume, streak);
    }

    private static void playFlat(HitSound sound, double volume, int streak) {
        playFlatPitch(sound, volume, HitPitch.forStreak(streak));
    }

    private static void playFlatPitch(HitSound sound, double volume, float pitch) {
        if (volume <= 0) {
            return;
        }
        Minecraft.getMinecraft().getSoundHandler().playSound(new PositionedSoundRecord(
                soundOf(sound).getSoundName(), SoundCategory.PLAYERS, (float) volume, pitch,
                false, 0, ISound.AttenuationType.NONE, 0, 0, 0));
    }

    /** 音階を最低音から最高音まで ticksPerNote ごとに鳴らす。note は連続ヒット数を受け取って1音鳴らす処理。 */
    static void playScale(IntConsumer note, int ticksPerNote, int startDelay) {
        for (int i = 0; i < HitPitch.SCALE_LENGTH; i++) {
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
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
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

    private static SoundEvent soundOf(HitSound sound) {
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
