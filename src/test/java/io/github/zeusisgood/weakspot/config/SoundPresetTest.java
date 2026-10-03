package io.github.zeusisgood.weakspot.config;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.github.zeusisgood.weakspot.common.HitScale;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class SoundPresetTest {

    /** 「標準」は設定の初期値と同じ鳴らし方。最初から入っているものは、どれも鳴らし方が違う。 */
    @Test
    public void standardMatchesDefaultsAndBuiltInsDiffer() {
        assertTrue(SoundPreset.BUILT_IN.get(0).sameSound(SoundPreset.of(new ClientConfig().sound)));
        assertEquals(6, SoundPreset.BUILT_IN.size());
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < SoundPreset.BUILT_IN.size(); i++) {
            keys.add(SoundPreset.BUILT_IN.get(i).key);
            for (int j = i + 1; j < SoundPreset.BUILT_IN.size(); j++) {
                assertFalse(SoundPreset.BUILT_IN.get(i).sameSound(SoundPreset.BUILT_IN.get(j)));
            }
        }
        assertEquals(6, keys.size());
    }

    /** 保存した枠は読み戻せる。ほかの枠はそのまま、番号の順に並ぶ。 */
    @Test
    public void saveAndReadSlots() {
        SoundPreset sparkle = SoundPreset.BUILT_IN.get(1);
        String[] lines = SoundPreset.withMySlot(new String[0], 2, sparkle);
        assertArrayEquals(new String[] {"2=CHIME,PENTATONIC,UP_DOWN,2,true"}, lines);
        assertTrue(sparkle.sameSound(SoundPreset.mySlot(lines, 2)));
        assertNull(SoundPreset.mySlot(lines, 1));
        SoundPreset night = SoundPreset.BUILT_IN.get(3);
        lines = SoundPreset.withMySlot(lines, 1, night);
        assertEquals(2, lines.length);
        assertTrue(lines[0].startsWith("1="));
        assertTrue(night.sameSound(SoundPreset.mySlot(lines, 1)));
        assertTrue(sparkle.sameSound(SoundPreset.mySlot(lines, 2)));
        // 上書き
        lines = SoundPreset.withMySlot(lines, 2, night);
        assertTrue(night.sameSound(SoundPreset.mySlot(lines, 2)));
    }

    /** 読めない行は空き。小文字・空白は許す。 */
    @Test
    public void unreadableLinesAreEmpty() {
        String[] lines = {"1=NOPE,MAJOR,UP,1,true", "2=chime, pentatonic, up_down, 2, false", "3=PLING,MAJOR,UP,1,maybe"};
        assertNull(SoundPreset.mySlot(lines, 1));
        SoundPreset two = SoundPreset.mySlot(lines, 2);
        assertEquals(HitSound.CHIME, two.sound);
        assertEquals(HitScale.Direction.UP_DOWN, two.direction);
        assertFalse(two.chord);
        assertNull(SoundPreset.mySlot(lines, 3));
        assertNull(SoundPreset.mySlot(null, 1));
        // 読めない行は、保存し直すときに消える
        assertEquals(1, SoundPreset.withMySlot(lines, 1, null).length);
    }

    /** 適用しても、音量とほかの人の音は変えない。 */
    @Test
    public void applyKeepsVolumesAndOthers() {
        ClientConfig.Sound sound = new ClientConfig().sound;
        sound.myHitVolume = 0.7;
        sound.othersHitSound = HitSound.BASS;
        SoundPreset.BUILT_IN.get(4).applyTo(sound);
        assertEquals(HitSound.BELL, sound.myHitSound);
        assertFalse(sound.hitChordEnabled);
        assertEquals(0.7, sound.myHitVolume, 0);
        assertEquals(HitSound.BASS, sound.othersHitSound);
    }
}
