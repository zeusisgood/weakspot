package com.example.weakspot.common;

import java.util.HashSet;
import java.util.Set;

/**
 * プレイヤーごとの弱点のオン・オフ（サーバーが持つ、クライアントの HOME キーの状態）。
 * オフのプレイヤーだけを覚えるので、届く前（何も知らない）プレイヤーはオンとして扱う。
 */
public final class PlayerSwitches<K> {

    private final Set<K> off = new HashSet<>();

    public boolean isEnabled(K player) {
        return !off.contains(player);
    }

    /** クライアントから届いた状態を反映する。 */
    public void set(K player, boolean enabled) {
        if (enabled) {
            off.remove(player);
        } else {
            off.add(player);
        }
    }

    /** ログアウトしたとき。 */
    public void forget(K player) {
        off.remove(player);
    }
}
