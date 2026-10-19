package io.github.zeusisgood.weakspot.config;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** configVersion 7: 1.9.x の形（general に並ぶ）の weakspot.cfg を、1.10.0 のカテゴリへ移す。 */
public class ConfigMigrationTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /** Configuration は Minecraft のフォルダ（FMLInjectionData）を読むので、テストでは一時フォルダを入れる。 */
    @Before
    public void setMinecraftHome() throws Exception {
        Field home = Class.forName("net.minecraftforge.fml.relauncher.FMLInjectionData").getDeclaredField("minecraftHome");
        home.setAccessible(true);
        if (home.get(null) == null) {
            home.set(null, folder.getRoot());
        }
    }

    private Configuration migrated() throws Exception {
        File file = folder.newFile("weakspot.cfg");
        try (InputStream in = getClass().getResourceAsStream("/config/weakspot-1.9.6.cfg")) {
            Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        Configuration cfg = new Configuration(file);
        WeakSpotConfig.moveToCategories(cfg);
        cfg.save();
        return new Configuration(file);
    }

    @Test
    public void valuesMoveToTheirCategories() throws Exception {
        Configuration cfg = migrated();
        assertEquals(6.5, cfg.getCategory("server.mining").get("boostMultiplier").getDouble(), 0);
        assertEquals(0.75, cfg.getCategory("server.throw").get("throwChargePerHit").getDouble(), 0);
        assertArrayEquals(new int[] {10, 20}, cfg.getCategory("server.milestones").get("milestones").getIntList());
        assertArrayEquals(new String[] {"bow", "eat"}, cfg.getCategory("client.markers").get("disabledKinds").getStringList());
        assertFalse(cfg.getCategory("client.sound").get("hitChordEnabled").getBoolean());
        assertEquals("BELL", cfg.getCategory("client.sound").get("myHitSound").getString());
    }

    @Test
    public void onlyTheVersionStaysInGeneral() throws Exception {
        Configuration cfg = migrated();
        ConfigCategory general = cfg.getCategory("general");
        assertEquals(1, general.size());
        assertEquals(6, general.get("configVersion").getInt());
        for (String name : cfg.getCategoryNames()) {
            assertFalse(name, cfg.getCategory(name).containsKey("meleeWeakSpotScale"));
        }
    }

    @Test
    public void everySettingHasACategoryUnderServerOrClient() {
        for (WeakSpotConfig.Setting setting : WeakSpotConfig.settings()) {
            if (setting.name().equals("configVersion")) {
                assertEquals("general", setting.category);
            } else {
                assertTrue(setting.name(), setting.category.matches("(server|client)\\.[a-z]+"));
            }
        }
    }
}
