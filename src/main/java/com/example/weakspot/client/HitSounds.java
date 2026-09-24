package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitPitch;
import com.example.weakspot.config.HitSound;
import com.example.weakspot.config.WeakSpotConfig;
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
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
final class HitSounds {

    /** 予約した音。delay が 0 になった tick に鳴らす。一時停止中も進める（統計画面の試聴のため）。 */
    private static final List<Scheduled> QUEUE = new ArrayList<>();

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

    /** 自分のヒット音。streak は連続ヒット数（1 始まり）。 */
    static void playOwn(int streak) {
        playFlat(WeakSpotConfig.myHitSound, WeakSpotConfig.myHitVolume, streak);
    }

    /** 他のプレイヤーのヒット音。叩かれたブロックの位置から鳴らす。 */
    static void playOther(BlockPos pos, int streak) {
        float volume = (float) WeakSpotConfig.othersHitVolume;
        if (volume <= 0 || Minecraft.getMinecraft().world == null) {
            return;
        }
        Minecraft.getMinecraft().getSoundHandler().playSound(new PositionedSoundRecord(
                soundOf(WeakSpotConfig.othersHitSound), SoundCategory.PLAYERS, volume,
                HitPitch.forStreak(streak), pos));
    }

    /** 他のプレイヤーのヒット音の試聴。実際は距離で小さくなるが、試聴は距離なしで鳴らす。 */
    static void playOtherFlat(int streak) {
        playFlat(WeakSpotConfig.othersHitSound, WeakSpotConfig.othersHitVolume, streak);
    }

    private static void playFlat(HitSound sound, double volume, int streak) {
        if (volume <= 0) {
            return;
        }
        Minecraft.getMinecraft().getSoundHandler().playSound(new PositionedSoundRecord(
                soundOf(sound).getSoundName(), SoundCategory.PLAYERS, (float) volume, HitPitch.forStreak(streak),
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
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || QUEUE.isEmpty()) {
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
