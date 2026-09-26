package com.example.weakspot.config;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class SyncedSettingsTest {

    /** 送る一覧に、公開のフィールドがちょうど 1 回ずつ入っている（足し忘れ・重なりがない）。 */
    @Test
    public void wireListsEveryFieldOnce() {
        Set<String> fields = new HashSet<>();
        for (Field field : SyncedSettings.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers())) {
                fields.add(field.getName());
            }
        }
        assertEquals(fields.size(), SyncedSettings.WIRE.length);
        assertEquals(fields, new HashSet<>(Arrays.asList(SyncedSettings.WIRE)));
    }

    /** serverVersion 以外は、WeakSpotConfig に同じ名前・合う型の項目がある（一覧は Set、設定は String[]）。 */
    @Test
    public void everyFieldHasAConfigEntry() throws Exception {
        for (String name : SyncedSettings.WIRE) {
            if (!name.equals("serverVersion")) {
                Class<?> config = WeakSpotConfig.class.getField(name).getType();
                Class<?> synced = SyncedSettings.class.getField(name).getType();
                assertEquals(name, synced == Set.class ? String[].class : synced, config);
            }
        }
    }

    @Test
    public void everyKindHasAnIntervalAndServerSwitchesMatchTheFields() throws Exception {
        java.lang.reflect.Constructor<SyncedSettings> c = SyncedSettings.class.getDeclaredConstructor();
        c.setAccessible(true);
        SyncedSettings s = c.newInstance();
        s.minHitIntervalTicks = 6;
        s.growthMinHitIntervalTicks = 7;
        s.portalMinHitIntervalTicks = 9;
        s.bowWeakSpotEnabled = false;
        s.portalWeakSpotEnabled = true;
        org.junit.Assert.assertEquals(6, s.minHitInterval(com.example.weakspot.common.HitKind.MINING));
        org.junit.Assert.assertEquals(7, s.minHitInterval(com.example.weakspot.common.HitKind.GROWTH));
        org.junit.Assert.assertEquals(9, s.minHitInterval(com.example.weakspot.common.HitKind.PORTAL));
        org.junit.Assert.assertFalse(s.enabled(com.example.weakspot.common.HitKind.BOW));
        org.junit.Assert.assertTrue(s.enabled(com.example.weakspot.common.HitKind.PORTAL));
        // 全体のオン・オフの設定がない種類は、いつでもオン
        org.junit.Assert.assertTrue(s.enabled(com.example.weakspot.common.HitKind.MINING));
        for (com.example.weakspot.common.HitKind kind : com.example.weakspot.common.HitKind.values()) {
            s.minHitInterval(kind);
        }
    }
}
