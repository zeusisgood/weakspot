package io.github.zeusisgood.weakspot;

import net.minecraft.entity.player.EntityPlayer;

/** 弱点を出せるプレイヤーか（両側）。 */
public final class PlayerRules {

    private PlayerRules() {
    }

    /**
     * 観戦モードでなければ出す（クリエイティブでも出す）。
     */
    public static boolean canUse(EntityPlayer player) {
        return !player.isSpectator();
    }
}
