package io.github.zeusisgood.weakspot;

import net.minecraft.world.entity.player.Player;

public final class PlayerRules {

    private PlayerRules() {
    }

    /** クリエイティブでも出す。 */
    public static boolean canUse(Player player) {
        return !player.isSpectator();
    }
}
