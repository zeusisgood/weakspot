package com.example.weakspot.config;

import io.netty.buffer.ByteBuf;

/**
 * サーバーの値が正の設定のうち、クライアントが使うもの。サーバーが自分の WeakSpotConfig から作って送り、
 * クライアントは接続中この値を使う（WeakSpotConfig の static フィールドには書き込まない。書き込むと
 * ConfigManager.sync でサーバーの値がクライアントの weakspot.cfg に保存されてしまうため）。
 *
 * 作った後は読むだけにする。項目を足すと通信内容が変わる（マイナーを上げる）。
 */
public final class SyncedSettings {

    public double boostMultiplier;
    public int boostDurationTicks;
    public int minHitIntervalTicks;
    public double weakSpotRadiusRatio;
    public double edgeMargin;
    public double minMoveDistance;
    public int lingerTicks;

    private SyncedSettings() {
    }

    /** この側の weakspot.cfg の値。 */
    public static SyncedSettings fromConfig() {
        SyncedSettings s = new SyncedSettings();
        s.boostMultiplier = WeakSpotConfig.boostMultiplier;
        s.boostDurationTicks = WeakSpotConfig.boostDurationTicks;
        s.minHitIntervalTicks = WeakSpotConfig.minHitIntervalTicks;
        s.weakSpotRadiusRatio = WeakSpotConfig.weakSpotRadiusRatio;
        s.edgeMargin = WeakSpotConfig.edgeMargin;
        s.minMoveDistance = WeakSpotConfig.minMoveDistance;
        s.lingerTicks = WeakSpotConfig.lingerTicks;
        return s;
    }

    public void write(ByteBuf buf) {
        buf.writeDouble(boostMultiplier);
        buf.writeInt(boostDurationTicks);
        buf.writeInt(minHitIntervalTicks);
        buf.writeDouble(weakSpotRadiusRatio);
        buf.writeDouble(edgeMargin);
        buf.writeDouble(minMoveDistance);
        buf.writeInt(lingerTicks);
    }

    public static SyncedSettings read(ByteBuf buf) {
        SyncedSettings s = new SyncedSettings();
        s.boostMultiplier = buf.readDouble();
        s.boostDurationTicks = buf.readInt();
        s.minHitIntervalTicks = buf.readInt();
        s.weakSpotRadiusRatio = buf.readDouble();
        s.edgeMargin = buf.readDouble();
        s.minMoveDistance = buf.readDouble();
        s.lingerTicks = buf.readInt();
        return s;
    }
}
