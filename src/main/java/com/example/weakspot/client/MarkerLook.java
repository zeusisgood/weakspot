package com.example.weakspot.client;

import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.MarkerColor;
import com.example.weakspot.common.MarkerShape;
import com.example.weakspot.config.WeakSpotConfig;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 自分の弱点の、種類ごとの色と形（1.7.0。統計画面の「弱点」タブ、設定 myMarkerColors / myMarkerShapes）。
 * 書いていない種類は、色は初期値（種類ごとの今までの色）、形は円。
 */
public final class MarkerLook {

    /** 「弱点」タブの ▶ で順に切り替える 12 色。 */
    static final int[] PRESETS = {0xFF5926, 0xFF3DCB, 0xFF8C42, 0xFFE14D, 0x7CFC00, 0x2ED3B7, 0x55CCFF, 0x7FB2FF,
            0xB070FF, 0xC8A060, 0xF0F0F0, 0xFF5A5F};

    private static String[] colorKeys;
    private static final Map<HitKind, Integer> COLORS = new EnumMap<>(HitKind.class);
    private static String[] shapeKeys;
    private static final Map<HitKind, MarkerShape> SHAPES = new EnumMap<>(HitKind.class);

    private MarkerLook() {
    }

    /** 種類ごとの初期値の色（「弱点」タブの見本。ブロック・生き物・弓・釣りの弱点は橙赤、ほかは種類ごとの色）。 */
    static int defaultColor(HitKind kind) {
        switch (kind) {
            case HARVEST: return 0xFF3DCB;
            case VEHICLE: return 0x55CCFF;
            case EAT: return 0x7CFC00;
            case SLEEP: return 0xFFF1A8;
            case LADDER: return 0xC8A060;
            case ELYTRA: return 0x7FB2FF;
            case ENCHANT: return 0xB070FF;
            case THROW: return 0x2ED3B7;
            case SPRINT: return 0xFF5A5F;
            case MELEE: return 0xD0D8E0;
            case PORTAL: return 0xFFD23F;
            default: return 0xFF5926;
        }
    }

    /** 書き換えた色（0xRRGGBB）。書いていなければ null。 */
    static Integer customColor(HitKind kind) {
        refresh();
        return COLORS.get(kind);
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
            custom = defaultColor(kind);
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
        return shape != null ? shape : MarkerShape.CIRCLE;
    }

    /** 色を書き換えて保存する。rgb が null なら初期値に戻す。 */
    static void setColor(HitKind kind, Integer rgb) {
        refresh();
        if (rgb == null) {
            COLORS.remove(kind);
        } else {
            COLORS.put(kind, rgb & 0xFFFFFF);
        }
        List<String> lines = new ArrayList<>();
        for (Map.Entry<HitKind, Integer> e : COLORS.entrySet()) {
            lines.add(e.getKey().key() + "=" + String.format("#%06X", e.getValue()));
        }
        WeakSpotConfig.myMarkerColors = lines.toArray(new String[0]);
        colorKeys = WeakSpotConfig.myMarkerColors;
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
        WeakSpotConfig.myMarkerShapes = lines.toArray(new String[0]);
        shapeKeys = WeakSpotConfig.myMarkerShapes;
        WeakSpotConfig.save();
    }

    /** 設定の配列が置き換わっていたら（設定画面で変えた、など）読み直す。 */
    private static void refresh() {
        if (WeakSpotConfig.myMarkerColors != colorKeys) {
            colorKeys = WeakSpotConfig.myMarkerColors;
            COLORS.clear();
            for (String line : colorKeys) {
                String[] kv = split(line);
                HitKind kind = kv == null ? null : HitKind.byKey(kv[0]);
                int rgb = kv == null ? -1 : MarkerColor.parse(kv[1], -1);
                if (kind != null && rgb >= 0) {
                    COLORS.put(kind, rgb);
                }
            }
        }
        if (WeakSpotConfig.myMarkerShapes != shapeKeys) {
            shapeKeys = WeakSpotConfig.myMarkerShapes;
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
