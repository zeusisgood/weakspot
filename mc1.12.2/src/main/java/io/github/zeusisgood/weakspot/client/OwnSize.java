package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.Trinkets;
import io.github.zeusisgood.weakspot.common.HitKind;
import net.minecraft.client.Minecraft;

/** 自分の弱点の大きさ（設定の大きさ × お守り）。ほかのプレイヤーのマークは、設定の大きさのまま描く。 */
final class OwnSize {

    private OwnSize() {
    }

    static double of(HitKind kind) {
        Minecraft mc = Minecraft.getMinecraft();
        return ClientSettings.get().spotSize(kind) * (mc.player == null ? 1 : Trinkets.sizeFactor(mc.player));
    }
}
