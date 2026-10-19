package io.github.zeusisgood.weakspot.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 同じブロックの複数行は、どれかを満たせばよい。条件を書かない行には、DEFAULT_CONDITIONS を付ける。 */
public final class GrowthFilters {

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

    /** 条件は問わない。 */
    public boolean contains(String block) {
        return byBlock.containsKey(block);
    }

    /** properties はプロパティの名前 → 値の名前。リストになければ false。 */
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

    /** ログの警告用。 */
    public List<String> invalid() {
        return Collections.unmodifiableList(invalid);
    }
}
