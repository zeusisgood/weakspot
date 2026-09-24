package com.example.weakspot.common;

/**
 * IC2 のゴムの木（ic2:rubber_wood）の樹液の穴の判定（SPEC_v1.3.7）。Minecraft と IC2 に依存しない。
 * プロパティ state の値の名前が dry_north / dry_south / dry_west / dry_east なら、その向きに乾いた穴がある
 * （randomTick で 1/7 の確率で wet_* に戻る）。
 */
public final class ResinHole {

    /** ゴムの木の登録名。 */
    public static final String BLOCK = "ic2:rubber_wood";
    /** 穴の状態を持つプロパティの名前。 */
    public static final String PROPERTY = "state";
    private static final String DRY_PREFIX = "dry_";

    private ResinHole() {
    }

    /** 乾いた穴の向きの名前（"north" など）。乾いた穴でなければ null。 */
    public static String dryFacing(String stateValue) {
        if (stateValue == null || !stateValue.startsWith(DRY_PREFIX) || stateValue.length() == DRY_PREFIX.length()) {
            return null;
        }
        return stateValue.substring(DRY_PREFIX.length());
    }
}
