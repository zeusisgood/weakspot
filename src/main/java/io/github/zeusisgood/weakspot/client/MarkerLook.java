package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.MarkerColor;
import io.github.zeusisgood.weakspot.common.MarkerShape;
import io.github.zeusisgood.weakspot.common.TargetRules;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;

/**
 * 自分の弱点の、種類ごとの色と形（1.7.0。統計画面の「弱点マーカー」タブ、設定 myMarkerColors / myMarkerShapes）。
 * 書いていない種類は、色は初期値（種類ごとの今までの色）、形は円。
 */
public final class MarkerLook {

    /** 「弱点マーカー」タブの ▶ で順に切り替える 12 色。 */
    static final int[] PRESETS = {0xFF5926, 0xFF3DCB, 0xFF8C42, 0xFFE14D, 0x7CFC00, 0x2ED3B7, 0x55CCFF, 0x7FB2FF,
            0xB070FF, 0xC8A060, 0xF0F0F0, 0xFF5A5F};

    /** 的当てのご褒美の特別な色（1.11.0。設定には「種類=silver」「種類=rainbow」と書く）。 */
    static final String SILVER = "silver";
    static final String RAINBOW = "rainbow";
    private static final int SILVER_RGB = 0xC0C0C0;

    private static String[] colorKeys;
    private static final Map<HitKind, Integer> COLORS = new EnumMap<>(HitKind.class);
    private static final Map<HitKind, String> SPECIALS = new EnumMap<>(HitKind.class);
    private static String[] shapeKeys;
    private static final Map<HitKind, MarkerShape> SHAPES = new EnumMap<>(HitKind.class);

    private MarkerLook() {
    }

    /**
     * 書き換えた色（0xRRGGBB）。書いていなければ null。特別な色（銀のきらめき・虹）は、今の時刻の色（解放していなければ
     * 書いていないのと同じ）。
     */
    static Integer customColor(HitKind kind) {
        refresh();
        String special = SPECIALS.get(kind);
        if (special != null && specialUnlocked(special)) {
            return SILVER.equals(special) ? silverRgb() : rainbowRgb();
        }
        return COLORS.get(kind);
    }

    /** 特別な色の名前（書いていなければ null）。 */
    static String special(HitKind kind) {
        refresh();
        return SPECIALS.get(kind);
    }

    /** 特別な色を使えるか（銀は的当ての銀、虹は虹の段階）。 */
    static boolean specialUnlocked(String special) {
        return TargetRecords.unlocked(SILVER.equals(special) ? TargetRules.Tier.SILVER : TargetRules.Tier.RAINBOW);
    }

    /** 銀のきらめき（銀から白へ、ゆっくり明るさが揺れる）。 */
    static int silverRgb() {
        double t = (Minecraft.getSystemTime() % 900) / 900.0;
        float k = (float) (0.35 * (0.5 + 0.5 * Math.sin(t * 2 * Math.PI)));
        float[] c = MarkerColor.towardWhite(SILVER_RGB, k);
        return (int) (c[0] * 255) << 16 | (int) (c[1] * 255) << 8 | (int) (c[2] * 255);
    }

    /** 虹（色が巡る。2 秒で一周）。 */
    static int rainbowRgb() {
        float hue = (Minecraft.getSystemTime() % 2000) / 2000F;
        return Color.HSBtoRGB(hue, 0.6F, 1.0F) & 0xFFFFFF;
    }

    /** その種類の色。書いていなければ初期値の色（HitKind.defaultColor）。 */
    static int color(HitKind kind) {
        return color(kind, kind.defaultColor());
    }

    /** その種類の色。書いていなければ defaultRgb。 */
    static int color(HitKind kind, int defaultRgb) {
        Integer custom = customColor(kind);
        return custom != null ? custom : defaultRgb;
    }

    /**
     * 円・輪・中心の色 {disk, ring, center}。色を書き換えていなければ、渡した初期値の色のまま（今までの見た目）。
     */
    static float[][] palette(HitKind kind, float[] disk, float[] ring, float[] center) {
        Integer custom = customColor(kind);
        if (custom == null && kind == HitKind.HARVEST) {
            // 収穫の弱点は、作物と重ならないマゼンタが初期値（ブロックの弱点で、ほかは橙赤）
            custom = kind.defaultColor();
        }
        if (custom == null) {
            return new float[][] {disk, ring, center};
        }
        return new float[][] {MarkerColor.towardWhite(custom, 0), MarkerColor.towardWhite(custom, 0.5F),
                MarkerColor.towardWhite(custom, 0.75F)};
    }

    static MarkerShape shape(HitKind kind) {
        refresh();
        MarkerShape shape = SHAPES.get(kind);
        if (shape == MarkerShape.STAR && !TargetRecords.unlocked(TargetRules.Tier.BRONZE)) {
            // 星は的当ての銅のご褒美（1.11.0）。解放していないワールドでは円
            return MarkerShape.CIRCLE;
        }
        return shape != null ? shape : MarkerShape.CIRCLE;
    }

    /** 色を書き換えて保存する。rgb が null なら初期値に戻す。 */
    static void setColor(HitKind kind, Integer rgb) {
        refresh();
        SPECIALS.remove(kind);
        if (rgb == null) {
            COLORS.remove(kind);
        } else {
            COLORS.put(kind, rgb & 0xFFFFFF);
        }
        saveColors();
    }

    /** 特別な色（SILVER / RAINBOW）にして保存する。 */
    static void setSpecial(HitKind kind, String special) {
        refresh();
        COLORS.remove(kind);
        SPECIALS.put(kind, special);
        saveColors();
    }

    private static void saveColors() {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<HitKind, Integer> e : COLORS.entrySet()) {
            lines.add(e.getKey().key() + "=" + String.format("#%06X", e.getValue()));
        }
        for (Map.Entry<HitKind, String> e : SPECIALS.entrySet()) {
            lines.add(e.getKey().key() + "=" + e.getValue());
        }
        WeakSpotConfig.client.markers.myMarkerColors = lines.toArray(new String[0]);
        colorKeys = WeakSpotConfig.client.markers.myMarkerColors;
        WeakSpotConfig.save();
    }

    static void setShape(HitKind kind, MarkerShape shape) {
        refresh();
        if (shape == null || shape == MarkerShape.CIRCLE) {
            SHAPES.remove(kind);
        } else {
            SHAPES.put(kind, shape);
        }
        List<String> lines = new ArrayList<>();
        for (Map.Entry<HitKind, MarkerShape> e : SHAPES.entrySet()) {
            lines.add(e.getKey().key() + "=" + e.getValue().name().toLowerCase(java.util.Locale.ROOT));
        }
        WeakSpotConfig.client.markers.myMarkerShapes = lines.toArray(new String[0]);
        shapeKeys = WeakSpotConfig.client.markers.myMarkerShapes;
        WeakSpotConfig.save();
    }

    /** 設定の配列が置き換わっていたら（設定画面で変えた、など）読み直す。 */
    private static void refresh() {
        if (WeakSpotConfig.client.markers.myMarkerColors != colorKeys) {
            colorKeys = WeakSpotConfig.client.markers.myMarkerColors;
            COLORS.clear();
            SPECIALS.clear();
            for (String line : colorKeys) {
                String[] kv = split(line);
                HitKind kind = kv == null ? null : HitKind.byKey(kv[0]);
                if (kind == null) {
                    continue;
                }
                String value = kv[1].toLowerCase(java.util.Locale.ROOT);
                if (SILVER.equals(value) || RAINBOW.equals(value)) {
                    SPECIALS.put(kind, value);
                    continue;
                }
                int rgb = MarkerColor.parse(kv[1], -1);
                if (rgb >= 0) {
                    COLORS.put(kind, rgb);
                }
            }
        }
        if (WeakSpotConfig.client.markers.myMarkerShapes != shapeKeys) {
            shapeKeys = WeakSpotConfig.client.markers.myMarkerShapes;
            SHAPES.clear();
            for (String line : shapeKeys) {
                String[] kv = split(line);
                HitKind kind = kv == null ? null : HitKind.byKey(kv[0]);
                if (kind != null) {
                    SHAPES.put(kind, MarkerShape.fromName(kv[1]));
                }
            }
        }
    }

    private static String[] split(String line) {
        int eq = line == null ? -1 : line.indexOf('=');
        return eq <= 0 ? null : new String[] {line.substring(0, eq).trim(), line.substring(eq + 1).trim()};
    }
}
