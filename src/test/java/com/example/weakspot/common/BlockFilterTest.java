package com.example.weakspot.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class BlockFilterTest {

    private static Map<String, String> props(String... pairs) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }

    @Test
    public void parsesNamesAndConditions() {
        BlockFilter plain = BlockFilter.parse("somemod:crop");
        assertEquals("somemod:crop", plain.block);
        assertFalse(plain.hasConditions());

        BlockFilter one = BlockFilter.parse(" ic2:rubber_wood [ state = dry_* ] ");
        assertEquals("ic2:rubber_wood", one.block);
        assertEquals("dry_*", one.conditions.get("state"));

        BlockFilter two = BlockFilter.parse("somemod:crystal[age=0-2,half=lower]");
        assertEquals(2, two.conditions.size());
        assertEquals("lower", two.conditions.get("half"));
    }

    @Test
    public void rejectsBrokenLines() {
        assertNull(BlockFilter.parse("somemod:crop[age=0-2"));
        assertNull(BlockFilter.parse("somemod:crop[age]"));
        assertNull(BlockFilter.parse("somemod:crop[]"));
        assertNull(BlockFilter.parse("[age=1]"));
        assertNull(BlockFilter.parse("somemod:crop[age=]"));
        assertNull(BlockFilter.parse("  "));
        assertNull(BlockFilter.parse(null));
    }

    @Test
    public void matchesRanges() {
        assertTrue(BlockFilter.matchesValue("0-2", "0"));
        assertTrue(BlockFilter.matchesValue("0-2", "2"));
        assertFalse(BlockFilter.matchesValue("0-2", "3"));
        assertFalse(BlockFilter.matchesValue("0-2", "dry"));
    }

    @Test
    public void matchesWildcardsAndExactValues() {
        assertTrue(BlockFilter.matchesValue("dry_*", "dry_north"));
        assertFalse(BlockFilter.matchesValue("dry_*", "wet_north"));
        assertTrue(BlockFilter.matchesValue("*_north", "wet_north"));
        assertTrue(BlockFilter.matchesValue("a*c", "abbc"));
        assertFalse(BlockFilter.matchesValue("ab*ba", "aba"));
        assertTrue(BlockFilter.matchesValue("*", "anything"));
        assertTrue(BlockFilter.matchesValue("lower", "lower"));
        assertFalse(BlockFilter.matchesValue("lower", "upper"));
    }

    @Test
    public void needsEveryConditionAndTheProperty() {
        BlockFilter filter = BlockFilter.parse("x:y[age=0-2,half=lower]");
        assertTrue(filter.matches(props("age", "1", "half", "lower")));
        assertFalse(filter.matches(props("age", "1", "half", "upper")));
        assertFalse(filter.matches(props("age", "1")));
        assertTrue(BlockFilter.parse("x:y").matches(Collections.emptyMap()));
    }

    @Test
    public void growthFiltersUseAnyLineAndDefaults() {
        GrowthFilters filters = GrowthFilters.parse(Arrays.asList(
                "somemod:crop[age=0-2]", "somemod:crop[age=5]", "minecraft:nether_wart", "ic2:rubber_wood",
                "somemod:always", "broken[age=1"));
        assertTrue(filters.matches("somemod:crop", props("age", "1")));
        assertTrue(filters.matches("somemod:crop", props("age", "5")));
        assertFalse(filters.matches("somemod:crop", props("age", "4")));
        // 初期の条件
        assertTrue(filters.matches("minecraft:nether_wart", props("age", "2")));
        assertFalse(filters.matches("minecraft:nether_wart", props("age", "3")));
        assertTrue(filters.matches("ic2:rubber_wood", props("state", "dry_east")));
        assertFalse(filters.matches("ic2:rubber_wood", props("state", "wet_east")));
        // 条件なし
        assertTrue(filters.matches("somemod:always", Collections.emptyMap()));
        assertFalse(filters.contains("minecraft:wheat"));
        assertEquals(Collections.singletonList("broken[age=1"), filters.invalid());
    }
}
