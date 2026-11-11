package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.BugReport;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Windows・macOS 以外でブラウザーを開く。バニラの開き方は xdg-open を起動したらすぐ出力の受け口を閉じ
 * （それで止まる環境がある）、失敗にも気付けないので使わない。
 */
final class LinuxBrowser {

    private static final long WAIT_MILLIS = 3000;
    private static final File DEV_NULL = new File("/dev/null");

    private LinuxBrowser() {
    }

    /** 最大で候補の数 × 3 秒かかるので、クライアントのスレッドから呼ばない。 */
    static boolean open(String url) {
        for (List<String> command : BugReport.openCommands(url)) {
            if (tryCommand(command)) {
                return true;
            }
        }
        return false;
    }

    private static boolean tryCommand(List<String> command) {
        Process process;
        try {
            process = new ProcessBuilder(command).redirectInput(DEV_NULL).redirectOutput(DEV_NULL)
                    .redirectError(DEV_NULL).start();
        } catch (IOException e) {
            return false;
        }
        try {
            // ブラウザーを閉じるまで戻らない環境があるので、待ちきれなければ開けたとみなす
            return !process.waitFor(WAIT_MILLIS, TimeUnit.MILLISECONDS) || process.exitValue() == 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return true;
        }
    }
}
