package com.example.weakspot.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 成長の弱点の追加リスト（growthExtraBlocks）を読んだもの（SPEC_v1.4 §1）。Minecraft に依存しない。
 * 同じブロックの複数行は、どれかを満たせばよい。条件を書かない行には、初期の条件（DEFAULT_CONDITIONS）を付ける。
 */
public final class GrowthFilters {

    /** 条件を書かないときの初期の条件（今までの個別対応と同じ動き）。 */
    public static final Map<String, String> DEFAULT_CONDITIONS;

    static {
        Map<String, String> defaults = new HashMap<>();
        defaults.put("minecraft:nether_wart", "age=0-2");
        defaults.put("ic2:rubber_wood", "state=dry_*");
        DEFAULT_CONDITIONS = Collections.unmodifiableMap(defaults);
    }

    private final Map<String, List<BlockFilter>> byBlock = new HashMap<>();
    private final List<String> invalid = new ArrayList<>();

    private GrowthFilters() {
    }

    public static GrowthFilters parse(Collection<String> entries) {
        GrowthFilters filters = new GrowthFilters();
        for (String entry : entries) {
            BlockFilter filter = BlockFilter.parse(entry);
            if (filter == null) {
                filters.invalid.add(entry);
                continue;
            }
            if (!filter.hasConditions() && DEFAULT_CONDITIONS.containsKey(filter.block)) {
                filter = BlockFilter.parse(filter.block + "[" + DEFAULT_CONDITIONS.get(filter.block) + "]");
            }
            filters.byBlock.computeIfAbsent(filter.block, k -> new ArrayList<>()).add(filter);
        }
        return filters;
    }

    /** そのブロックが追加リストにあるか（条件は問わない）。 */
    public boolean contains(String block) {
        return byBlock.containsKey(block);
    }

    /** そのブロックの今の状態（プロパティの名前 → 値の名前）が、どれかの行を満たすか。リストになければ false。 */
    public boolean matches(String block, Map<String, String> properties) {
        List<BlockFilter> filters = byBlock.get(block);
        if (filters == null) {
            return false;
        }
        for (BlockFilter filter : filters) {
            if (filter.matches(properties)) {
                return true;
            }
        }
        return false;
    }

    /** 読めなかった行（ログの警告用）。 */
    public List<String> invalid() {
        return Collections.unmodifiableList(invalid);
    }
}
