package com.example.weakspot.client;

import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.MiningStats;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 統計の保持と保存。累計は全ワールド・全サーバー共通で、.minecraft/weakspot/stats.json に保存する。
 * 「今回」はワールド（サーバー）に入ってから出るまで。
 */
final class StatsManager {

    private static final Logger LOGGER = LogManager.getLogger(WeakSpotMod.MODID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    /** 変更があれば、この間隔（tick）で保存する。 */
    private static final int SAVE_INTERVAL_TICKS = 200;

    private static MiningStats total;
    private static final MiningStats SESSION = new MiningStats();
    private static boolean dirty;
    private static long lastSaveTick;

    private StatsManager() {
    }

    static MiningStats total() {
        if (total == null) {
            total = load();
        }
        return total;
    }

    static MiningStats session() {
        return SESSION;
    }

    static void recordHit(double extraTicks) {
        total().recordHit(extraTicks);
        SESSION.recordHit(extraTicks);
        dirty = true;
    }

    static void recordBlockBroken(int hitsOnBlock) {
        total().recordBlockBroken(hitsOnBlock);
        SESSION.recordBlockBroken(hitsOnBlock);
        dirty = true;
    }

    static void startSession() {
        SESSION.reset();
    }

    static void resetTotal() {
        total().reset();
        dirty = true;
        save();
    }

    /** 変更があり、前回の保存から一定時間たっていれば保存する。 */
    static void tick(long clientTick) {
        if (dirty && clientTick - lastSaveTick >= SAVE_INTERVAL_TICKS) {
            lastSaveTick = clientTick;
            save();
        }
    }

    static void save() {
        if (!dirty || total == null) {
            return;
        }
        File file = file();
        File tmp = new File(file.getPath() + ".tmp");
        try {
            Files.createDirectories(file.getParentFile().toPath());
            try (Writer writer = Files.newBufferedWriter(tmp.toPath(), StandardCharsets.UTF_8)) {
                GSON.toJson(total, writer);
            }
            try {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
        } catch (IOException e) {
            LOGGER.warn("Failed to save weak spot stats to {}", file, e);
        }
    }

    private static MiningStats load() {
        File file = file();
        if (!file.isFile()) {
            return new MiningStats();
        }
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            MiningStats loaded = GSON.fromJson(reader, MiningStats.class);
            return loaded != null ? loaded : new MiningStats();
        } catch (IOException | JsonParseException e) {
            LOGGER.warn("Failed to load weak spot stats from {}, starting from zero", file, e);
            return new MiningStats();
        }
    }

    private static File file() {
        return new File(new File(Minecraft.getMinecraft().mcDataDir, WeakSpotMod.MODID), "stats.json");
    }
}
