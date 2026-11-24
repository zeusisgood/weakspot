package io.github.zeusisgood.weakspot.server;

import io.github.zeusisgood.weakspot.common.MiningStats;
import net.minecraft.nbt.CompoundTag;

/**
 * 統計と NBT の受け渡し。キーと型は MiningStats.save / load が決め、ここは NBT の API に合わせるだけ。
 * 置き場所（どのタグに入れるか）は版ごとの ServerStats。
 */
final class StatsNbt {

    private StatsNbt() {
    }

    static MiningStats read(CompoundTag tag) {
        return MiningStats.load(store(tag));
    }

    static CompoundTag write(MiningStats stats) {
        CompoundTag tag = new CompoundTag();
        stats.save(store(tag));
        return tag;
    }

    private static MiningStats.Store store(CompoundTag tag) {
        return new MiningStats.Store() {
            @Override
            public boolean has(String key) {
                return tag.contains(key);
            }

            @Override
            public long getLong(String key) {
                return tag.getLong(key);
            }

            @Override
            public double getDouble(String key) {
                return tag.getDouble(key);
            }

            @Override
            public void putLong(String key, long value) {
                tag.putLong(key, value);
            }

            @Override
            public void putDouble(String key, double value) {
                tag.putDouble(key, value);
            }
        };
    }
}
