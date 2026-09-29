package io.github.zeusisgood.weakspot.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/** 設定画面のまとめ（1.10.2）: すべての設定が、ちょうど 1 つのまとめに入っている（画面に出さない項目を除く）。 */
public class SettingGroupsTest {

    @Test
    public void everySettingIsInExactlyOneGroup() {
        Map<String, Integer> count = new HashMap<>();
        for (List<String> names : SettingGroups.GROUPS.values()) {
            for (String name : names) {
                count.merge(name, 1, Integer::sum);
            }
        }
        List<String> wrong = new ArrayList<>();
        for (WeakSpotConfig.Setting setting : WeakSpotConfig.settings()) {
            int expected = SettingGroups.HIDDEN.contains(setting.name()) ? 0 : 1;
            if (count.getOrDefault(setting.name(), 0) != expected) {
                wrong.add(setting.name());
            }
        }
        assertTrue("settings not in exactly one group: " + wrong, wrong.isEmpty());
        for (String name : count.keySet()) {
            assertTrue("unknown setting in a group: " + name, WeakSpotConfig.setting(name) != null);
        }
        assertEquals(2, SettingGroups.HIDDEN.size());
    }
}
