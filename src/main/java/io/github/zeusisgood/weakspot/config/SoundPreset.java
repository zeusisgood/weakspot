package io.github.zeusisgood.weakspot.config;

import io.github.zeusisgood.weakspot.common.HitScale;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 音のプリセット（1.11.1）: 自分のヒット音の鳴らし方（楽器・和音・音階の動き・種類・音域）。音量とほかの人の音は入れない。
 * 最初から入っているもの（BUILT_IN。名前は翻訳キー weakspot.sound.preset.<key>）と、各自が保存するマイプリセット 1〜3
 * （設定 client.sound.soundPresets に「番号=楽器,音階の種類,動き,音域,和音」で 1 行ずつ）。
 */
public final class SoundPreset {

    /** マイプリセットの枠の数。 */
    public static final int MY_SLOTS = 3;

    /** 最初から入っているプリセット（並びは「サウンド」タブの ◀ ▶ の順）。 */
    public static final List<SoundPreset> BUILT_IN = Collections.unmodifiableList(Arrays.asList(
            new SoundPreset("standard", HitSound.PLING, true, HitScale.Direction.UP, HitScale.Type.MAJOR, 1),
            new SoundPreset("sparkle", HitSound.CHIME, true, HitScale.Direction.UP_DOWN, HitScale.Type.PENTATONIC,
                    2),
            new SoundPreset("xylophone", HitSound.XYLOPHONE, true, HitScale.Direction.UP_DOWN, HitScale.Type.MAJOR,
                    2),
            new SoundPreset("night", HitSound.HARP, true, HitScale.Direction.UP, HitScale.Type.MINOR, 1),
            new SoundPreset("bells", HitSound.BELL, false, HitScale.Direction.UP, HitScale.Type.PENTATONIC, 1),
            new SoundPreset("quiet", HitSound.FLUTE, false, HitScale.Direction.UP_DOWN, HitScale.Type.PENTATONIC,
                    1)));

    /** 最初から入っているものの名前のキー（マイプリセットは null）。 */
    public final String key;
    public final HitSound sound;
    public final boolean chord;
    public final HitScale.Direction direction;
    public final HitScale.Type type;
    public final int octaves;

    public SoundPreset(String key, HitSound sound, boolean chord, HitScale.Direction direction, HitScale.Type type,
                       int octaves) {
        this.key = key;
        this.sound = sound;
        this.chord = chord;
        this.direction = direction;
        this.type = type;
        this.octaves = Math.max(HitScale.MIN_OCTAVES, Math.min(HitScale.MAX_OCTAVES, octaves));
    }

    /** 今の設定の鳴らし方（名前なし）。 */
    public static SoundPreset of(ClientConfig.Sound sound) {
        return new SoundPreset(null, sound.myHitSound, sound.hitChordEnabled, sound.hitScaleDirection,
                sound.hitScaleType, sound.hitScaleOctaves);
    }

    /** 設定に書き込む（音量とほかの人の音は変えない）。 */
    public void applyTo(ClientConfig.Sound sound) {
        sound.myHitSound = this.sound;
        sound.hitChordEnabled = chord;
        sound.hitScaleDirection = direction;
        sound.hitScaleType = type;
        sound.hitScaleOctaves = octaves;
    }

    /** 鳴らし方が同じか（名前は見ない）。 */
    public boolean sameSound(SoundPreset other) {
        return other != null && sound == other.sound && chord == other.chord && direction == other.direction
                && type == other.type && octaves == other.octaves;
    }

    /** 設定の 1 行の右側「楽器,音階の種類,動き,音域,和音」。 */
    String format() {
        return sound.name() + "," + type.name() + "," + direction.name() + "," + octaves + "," + chord;
    }

    /** 設定の行の右側を読む。読めなければ null。 */
    static SoundPreset parse(String value) {
        String[] parts = value.split(",");
        if (parts.length != 5) {
            return null;
        }
        try {
            HitSound sound = HitSound.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
            HitScale.Type type = HitScale.Type.valueOf(parts[1].trim().toUpperCase(Locale.ROOT));
            HitScale.Direction direction = HitScale.Direction.valueOf(parts[2].trim().toUpperCase(Locale.ROOT));
            int octaves = Integer.parseInt(parts[3].trim());
            String chord = parts[4].trim().toLowerCase(Locale.ROOT);
            if (!chord.equals("true") && !chord.equals("false")) {
                return null;
            }
            return new SoundPreset(null, sound, chord.equals("true"), direction, type, octaves);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** マイプリセットの枠 slot（1 始まり）の中身。空き・読めないときは null。 */
    public static SoundPreset mySlot(String[] lines, int slot) {
        if (lines == null) {
            return null;
        }
        String prefix = slot + "=";
        for (String line : lines) {
            if (line != null && line.trim().startsWith(prefix)) {
                return parse(line.trim().substring(prefix.length()));
            }
        }
        return null;
    }

    /** マイプリセットの枠 slot に preset を書いた、新しい行の配列（ほかの枠の行はそのまま。番号の順に並べる）。 */
    public static String[] withMySlot(String[] lines, int slot, SoundPreset preset) {
        String[] out = new String[MY_SLOTS];
        int count = 0;
        for (int i = 1; i <= MY_SLOTS; i++) {
            SoundPreset p = i == slot ? preset : mySlot(lines, i);
            if (p != null) {
                out[count++] = i + "=" + p.format();
            }
        }
        return Arrays.copyOf(out, count);
    }
}
