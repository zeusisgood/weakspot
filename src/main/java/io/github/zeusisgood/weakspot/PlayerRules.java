package io.github.zeusisgood.weakspot;

import net.minecraft.entity.player.EntityPlayer;

public final class PlayerRules {

    private PlayerRules() {
    }

    /** クリエイティブでも出す。 */
    public static boolean canUse(EntityPlayer player) {
        return !player.isSpectator();
    }
}
