package io.github.zeusisgood.weakspot.config;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;
import org.junit.Test;

/**
 * 設定の名前が 3 つの版で同じか（正本 common/settings.txt と比べる）。翻訳の確かめは 1.12.2 の LangFilesTest だけが
 * するので、名前が揃っていれば、3 つの版の設定の翻訳がそろっていることになる。3 つの版に同じ中身で置く。
 */
public class SettingNamesTest {

    @Test
    public void settingsMatchTheSharedList() throws IOException {
        Set<String> expected = new TreeSet<>();
        for (String line : Files.readAllLines(Paths.get("common/settings.txt"), StandardCharsets.UTF_8)) {
            String name = line.trim();
            if (!name.isEmpty() && !name.startsWith("#")) {
                expected.add(name);
            }
        }
        Set<String> actual = new TreeSet<>();
        collect(actual, ServerConfig.class);
        collect(actual, ClientConfig.class);
        Set<String> missing = new TreeSet<>(expected);
        missing.removeAll(actual);
        Set<String> extra = new TreeSet<>(actual);
        extra.removeAll(expected);
        assertTrue("in common/settings.txt but not in this version: " + missing, missing.isEmpty());
        assertTrue("in this version but not in common/settings.txt: " + extra, extra.isEmpty());
    }

    /** getDeclaredFields の並びは決まっていないので、集合で比べる。 */
    private static void collect(Set<String> names, Class<?> top) {
        for (Field category : top.getDeclaredFields()) {
            if (!isSetting(category)) {
                continue;
            }
            for (Field field : category.getType().getDeclaredFields()) {
                if (isSetting(field)) {
                    names.add(top.getSimpleName() + "." + category.getType().getSimpleName() + "." + field.getName());
                }
            }
        }
    }

    private static boolean isSetting(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers);
    }
}
