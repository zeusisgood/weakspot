package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitPitch;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/** 自分のヒット音と、音階を順に鳴らす演出（節目）。 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
final class HitSounds {

    /** 音階の1オクターブの音の数（HitPitch の長音階）。 */
    static final int SCALE_LENGTH = 8;

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
        Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(
                SoundEvents.BLOCK_NOTE_PLING, HitPitch.forStreak(streak)));
    }

    /** 自分のヒット音で、音階を最低音から最高音まで ticksPerNote ごとに鳴らす。 */
    static void playOwnScale(int ticksPerNote, int startDelay) {
        for (int i = 0; i < SCALE_LENGTH; i++) {
            int streak = i + 1;
            schedule(startDelay + i * ticksPerNote, () -> playOwn(streak));
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
}
