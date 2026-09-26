package com.example.weakspot;

import net.minecraft.entity.player.EntityPlayer;

/** 弱点を出せるプレイヤーか（両側）。 */
public final class PlayerRules {

    private PlayerRules() {
    }

    /**
     * 観戦モードでなければ出す。1.8.6 からクリエイティブでも出す（それまでは、弱点の種類によって出なかった。1.8.x の間は、
     * 古いサーバーにつないだクライアントでは出さなかった。1.9.0 は 1.8.x のサーバーにつながらないので、その判定はやめた）。
     */
    public static boolean canUse(EntityPlayer player) {
        return !player.isSpectator();
    }
}
