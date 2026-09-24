package com.example.weakspot.config;

import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.minecraftforge.fml.common.network.ByteBufUtils;

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
    public double weakSpotMinRadius;
    public double weakSpotMaxRadiusRatio;
    public double minFaceSize;
    public int lingerTicks;
    public int growthMinHitIntervalTicks;
    public double growthMinRadius;
    /** 登録名（"minecraft:grass" など）。 */
    public Set<String> growthExcludedBlocks;
    public int machineMinHitIntervalTicks;
    /** 機械の加速の対象外。登録名。 */
    public Set<String> excludedBlocks;
    public double markerShareRange;
    public int markerSendMinIntervalTicks;

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
        s.weakSpotMinRadius = WeakSpotConfig.weakSpotMinRadius;
        s.weakSpotMaxRadiusRatio = WeakSpotConfig.weakSpotMaxRadiusRatio;
        s.minFaceSize = WeakSpotConfig.minFaceSize;
        s.lingerTicks = WeakSpotConfig.lingerTicks;
        s.growthMinHitIntervalTicks = WeakSpotConfig.growthMinHitIntervalTicks;
        s.growthMinRadius = WeakSpotConfig.growthMinRadius;
        s.growthExcludedBlocks = new HashSet<>(Arrays.asList(WeakSpotConfig.growthExcludedBlocks));
        s.machineMinHitIntervalTicks = WeakSpotConfig.machineMinHitIntervalTicks;
        s.excludedBlocks = new HashSet<>(Arrays.asList(WeakSpotConfig.excludedBlocks));
        s.markerShareRange = WeakSpotConfig.markerShareRange;
        s.markerSendMinIntervalTicks = WeakSpotConfig.markerSendMinIntervalTicks;
        return s;
    }

    public void write(ByteBuf buf) {
        buf.writeDouble(boostMultiplier);
        buf.writeInt(boostDurationTicks);
        buf.writeInt(minHitIntervalTicks);
        buf.writeDouble(weakSpotRadiusRatio);
        buf.writeDouble(edgeMargin);
        buf.writeDouble(minMoveDistance);
        buf.writeDouble(weakSpotMinRadius);
        buf.writeDouble(weakSpotMaxRadiusRatio);
        buf.writeDouble(minFaceSize);
        buf.writeInt(lingerTicks);
        buf.writeInt(growthMinHitIntervalTicks);
        buf.writeDouble(growthMinRadius);
        writeStrings(buf, growthExcludedBlocks);
        buf.writeInt(machineMinHitIntervalTicks);
        writeStrings(buf, excludedBlocks);
        buf.writeDouble(markerShareRange);
        buf.writeInt(markerSendMinIntervalTicks);
    }

    public static SyncedSettings read(ByteBuf buf) {
        SyncedSettings s = new SyncedSettings();
        s.boostMultiplier = buf.readDouble();
        s.boostDurationTicks = buf.readInt();
        s.minHitIntervalTicks = buf.readInt();
        s.weakSpotRadiusRatio = buf.readDouble();
        s.edgeMargin = buf.readDouble();
        s.minMoveDistance = buf.readDouble();
        s.weakSpotMinRadius = buf.readDouble();
        s.weakSpotMaxRadiusRatio = buf.readDouble();
        s.minFaceSize = buf.readDouble();
        s.lingerTicks = buf.readInt();
        s.growthMinHitIntervalTicks = buf.readInt();
        s.growthMinRadius = buf.readDouble();
        s.growthExcludedBlocks = readStrings(buf);
        s.machineMinHitIntervalTicks = buf.readInt();
        s.excludedBlocks = readStrings(buf);
        s.markerShareRange = buf.readDouble();
        s.markerSendMinIntervalTicks = buf.readInt();
        return s;
    }

    private static void writeStrings(ByteBuf buf, Set<String> strings) {
        buf.writeInt(strings.size());
        for (String string : strings) {
            ByteBufUtils.writeUTF8String(buf, string);
        }
    }

    private static Set<String> readStrings(ByteBuf buf) {
        int size = buf.readInt();
        Set<String> strings = new HashSet<>();
        for (int i = 0; i < size; i++) {
            strings.add(ByteBufUtils.readUTF8String(buf));
        }
        return strings;
    }
}
