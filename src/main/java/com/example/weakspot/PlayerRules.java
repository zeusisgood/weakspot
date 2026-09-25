package com.example.weakspot;

import net.minecraft.entity.player.EntityPlayer;

/** 弱点を出せるプレイヤーか（両側）。 */
public final class PlayerRules {

    private PlayerRules() {
    }

    /**
     * 観戦モードでなければ出す。1.8.6 からクリエイティブでも出す（それまでは、弱点の種類によって出なかった）。
     * クライアントは、サーバーが 1.8.6 以降のときだけクリエイティブでも出す（古いサーバーは受け付けないので、
     * 出ても何も起きない食い違いを防ぐ。CommonProxy#serverAcceptsCreative）。
     */
    public static boolean canUse(EntityPlayer player) {
        if (player.isSpectator()) {
            return false;
        }
        if (!player.capabilities.isCreativeMode) {
            return true;
        }
        return !player.world.isRemote || WeakSpotMod.proxy.serverAcceptsCreative();
    }
}
