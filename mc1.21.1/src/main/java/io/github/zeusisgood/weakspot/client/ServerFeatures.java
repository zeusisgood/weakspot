package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.ModVersions;

/**
 * サーバーの版は SyncedSettings.serverVersion で届く。届く前やシングルプレイでは、自分の版として扱う。
 * <p>
 * パッチで、サーバーとクライアントの両方が要る機能（弱点の対象を増やす、操作を変えるなど）を足すときは、
 * クライアント側を {@code ServerFeatures.since("1.6.x")} で囲み、古いサーバーでは弱点を出さない
 * （古いサーバーにつなぐと、弱点が出るのに GUI も開く、という食い違いを防ぐ）。
 */
public final class ServerFeatures {

    private ServerFeatures() {
    }

    public static boolean since(String version) {
        String server = ClientSettings.get().serverVersion;
        return server == null || ModVersions.compare(server, version) >= 0;
    }
}
