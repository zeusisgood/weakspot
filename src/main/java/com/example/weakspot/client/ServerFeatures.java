package com.example.weakspot.client;

import com.example.weakspot.common.ModVersions;

/**
 * 接続しているサーバーの Mod の版で、機能を使えるかを決める（1.6.0）。サーバーの版は SyncedSettings.serverVersion で届く。
 * 届く前やシングルプレイでは、自分の版として扱う。
 * <p>
 * 1.6.1 以降のパッチで、サーバーとクライアントの両方が要る機能（弱点の対象を増やす、操作を変えるなど）を足すときは、
 * クライアント側を {@code ServerFeatures.since("1.6.x")} で囲み、古いサーバーでは弱点を出さない
 * （1.4.4 のディスペンサーで、古いサーバーにつなぐと弱点が出るのに GUI が開いた、という食い違いを防ぐ）。
 */
public final class ServerFeatures {

    private ServerFeatures() {
    }

    /** サーバーの版が version 以上か。 */
    public static boolean since(String version) {
        String server = ClientSettings.get().serverVersion;
        return server == null || ModVersions.compare(server, version) >= 0;
    }
}
