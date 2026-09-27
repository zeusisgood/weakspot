package io.github.zeusisgood.weakspot.config;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.GrowthFilters;
import io.github.zeusisgood.weakspot.server.SettingsSync;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;

/**
 * config/weakspot.cfg。
 * [サーバー] はサーバーの値が正で、ログイン時と変更時にクライアントへ送る（SyncedSettings）。
 * クライアントは接続中、自分の [サーバー] の値を使わない。[クライアント] は各プレイヤーの設定のまま使う。
 * カテゴリは 2 段（server / client → 種類・用途。1.10.0）。項目の名前は、カテゴリを分ける前（general）と同じ。
 * 項目は {@link #server}・{@link #client} の入れ子のオブジェクトにある（例: WeakSpotConfig.server.mining.boostMultiplier）。
 */
@Config(modid = WeakSpotMod.MODID, category = "")
public final class WeakSpotConfig {

    @Config.Comment("共通")
    public static final General general = new General();

    @Config.Comment("サーバーの値が使われる設定")
    public static final ServerConfig server = new ServerConfig();

    @Config.Comment("各プレイヤーの設定")
    public static final ClientConfig client = new ClientConfig();

    /** 共通（general）。カテゴリを分ける前の設定ファイルを見分けるため、configVersion だけを置く。 */
    public static final class General {

        @Config.Comment("[内部] 設定ファイルの移行の済んだ版。書き換えないでください")
        public int configVersion = 0;

        General() {
        }
    }

    private WeakSpotConfig() {
    }

    /** 統計画面などから [クライアント] の値を書き換えた後に呼び、weakspot.cfg に保存する（Forge の設定画面と同じ値になる）。 */
    public static void save() {
        ConfigManager.sync(WeakSpotMod.MODID, Config.Type.INSTANCE);
    }

    /**
     * 古い版で作った weakspot.cfg を1回だけ移行する（init とサーバーの起動時に呼ぶ。何度呼んでもよい）。
     * 値は Configuration の Property に書いてから ConfigManager.sync でフィールドへ読み込む（preInit から init の間の sync は
     * ファイルの値でフィールドを上書きする「読み込み」なので、フィールドに書いてから保存することはできない）。
     * 1.9.0 で、1〜5（1.2.x〜1.6.x の古い初期値の直し）は消した。
     * 6: 今の設定にない項目（1.7.x の meleeWeakSpotScale など）を、ファイルから消す（Forge は自分では消さない）。
     * 7: general に並んでいた項目を、server / client の下のカテゴリへ移す（1.10.0）。今の設定にない項目は消す（6 を含む）。
     */
    public static void migrate() {
        if (general.configVersion >= 7) {
            return;
        }
        Configuration cfg = cachedConfiguration();
        if (cfg == null) {
            return;
        }
        moveToCategories(cfg);
        Property version = cfg.getCategory("general").get("configVersion");
        if (version != null) {
            version.set(7);
        }
        ConfigManager.sync(WeakSpotMod.MODID, Config.Type.INSTANCE);
        general.configVersion = 7;
    }

    /**
     * general の項目を、同じ名前の設定のカテゴリへ移す（configVersion 7）。移した先にもう項目があれば値だけを書き、
     * なければ項目ごと移す。今の設定にない項目は消す。configVersion は general に残す。
     */
    static void moveToCategories(Configuration cfg) {
        ConfigCategory old = cfg.getCategory("general");
        for (String key : new ArrayList<>(old.keySet())) {
            if ("configVersion".equals(key)) {
                continue;
            }
            Property from = old.remove(key);
            Setting setting = setting(key);
            if (setting == null) {
                LogManager.getLogger(WeakSpotMod.MODID).info("weakspot.cfg: removed the unused setting {}", key);
                continue;
            }
            ConfigCategory target = cfg.getCategory(setting.category);
            Property to = target.get(key);
            if (to == null) {
                target.put(key, from);
            } else if (from.isList()) {
                to.set(from.getStringList());
            } else {
                to.set(from.getString());
            }
        }
    }

    /** 設定の 1 項目。category は weakspot.cfg のカテゴリ（例: server.mining）。 */
    public static final class Setting {
        public final String category;
        public final Field field;
        private final Object owner;

        Setting(String category, Field field, Object owner) {
            this.category = category;
            this.field = field;
            this.owner = owner;
        }

        public String name() {
            return field.getName();
        }

        public Object get() {
            try {
                return field.get(owner);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        /** 設定画面の説明の翻訳キー（Forge の決まり: weakspot.カテゴリ.小文字の名前.tooltip）。 */
        public String tooltipKey() {
            return WeakSpotMod.MODID + "." + category + "." + name().toLowerCase(Locale.ENGLISH) + ".tooltip";
        }
    }

    private static List<Setting> settings;

    /** すべての設定（カテゴリの順）。 */
    public static synchronized List<Setting> settings() {
        if (settings == null) {
            List<Setting> list = new ArrayList<>();
            collect(WeakSpotConfig.class, null, "", list);
            settings = Collections.unmodifiableList(list);
        }
        return settings;
    }

    /** 名前から設定を探す（なければ null）。 */
    public static Setting setting(String name) {
        for (Setting setting : settings()) {
            if (setting.name().equals(name)) {
                return setting;
            }
        }
        return null;
    }

    /** ConfigManager と同じ決まりでたどる（根は static、入れ子は static でない public のフィールド）。 */
    private static void collect(Class<?> type, Object owner, String category, List<Setting> out) {
        for (Field field : type.getDeclaredFields()) {
            int mod = field.getModifiers();
            if (!Modifier.isPublic(mod) || Modifier.isStatic(mod) != (owner == null)) {
                continue;
            }
            Class<?> t = field.getType();
            if (t.isPrimitive() || t.isArray() || t.isEnum() || t == String.class) {
                out.add(new Setting(category, field, owner));
                continue;
            }
            Config.Name name = field.getAnnotation(Config.Name.class);
            String sub = (category.isEmpty() ? "" : category + ".")
                    + (name != null ? name.value() : field.getName()).toLowerCase(Locale.ENGLISH);
            try {
                collect(t, field.get(owner), sub, out);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static boolean reloadWarned;

    /**
     * weakspot.cfg をファイルから読み直して、フィールドに反映する（/weakspot reload）。読み直せなければ false。
     * 起動後の ConfigManager.sync は、「変わった」（Property#hasChanged）項目だけファイルの値をフィールドへ読み込み、
     * ほかはフィールドの値をファイルへ書き出す。そこで ConfigManager が持っている Configuration を読み直し、
     * すべての項目に「変わった」の印を付けてから sync する（Forge の設定画面と同じ流れ）。
     */
    public static boolean reloadFromFile() {
        Configuration cfg = cachedConfiguration();
        if (cfg == null) {
            return false;
        }
        cfg.load();
        for (String name : cfg.getCategoryNames()) {
            ConfigCategory category = cfg.getCategory(name);
            for (Property property : category.values()) {
                if (property.isList()) {
                    property.set(property.getStringList());
                } else {
                    property.set(property.getString());
                }
            }
        }
        ConfigManager.sync(WeakSpotMod.MODID, Config.Type.INSTANCE);
        return true;
    }

    /** ConfigManager の非公開の CONFIGS（ファイルの絶対パス → Configuration）から weakspot.cfg を取り出す。Forge のクラスなので名前は1つ。 */
    @SuppressWarnings("unchecked")
    private static Configuration cachedConfiguration() {
        try {
            Field field = ConfigManager.class.getDeclaredField("CONFIGS");
            field.setAccessible(true);
            Map<String, Configuration> configs = (Map<String, Configuration>) field.get(null);
            File file = new File(Loader.instance().getConfigDir(), WeakSpotMod.MODID + ".cfg");
            return configs.get(file.getAbsolutePath());
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!reloadWarned) {
                reloadWarned = true;
                LogManager.getLogger(WeakSpotMod.MODID).warn("Could not reload weakspot.cfg", e);
            }
            return null;
        }
    }

    /** 節目と量の数が合っていなければ、ログに警告を出す（足りない分は 0 として扱う）。 */
    public static void warnIfMisconfigured() {
        for (String entry : GrowthFilters.parse(Arrays.asList(server.growth.growthExtraBlocks)).invalid()) {
            LogManager.getLogger(WeakSpotMod.MODID).warn(
                    "weakspot.cfg: ignored an unreadable growthExtraBlocks entry: \"{}\" "
                            + "(write modid:block or modid:block[property=pattern,...])", entry);
        }
        ServerConfig.Milestones m = server.milestones;
        if (m.milestoneXp.length != m.milestones.length || m.milestoneRepair.length != m.milestones.length) {
            LogManager.getLogger(WeakSpotMod.MODID).warn(
                    "weakspot.cfg: milestones has {} entries but milestoneXp has {} and milestoneRepair has {}; "
                            + "missing amounts are treated as 0",
                    m.milestones.length, m.milestoneXp.length, m.milestoneRepair.length);
        }
        if (m.totalMilestoneXp.length != m.totalMilestones.length) {
            LogManager.getLogger(WeakSpotMod.MODID).warn(
                    "weakspot.cfg: totalMilestones has {} entries but totalMilestoneXp has {}; missing amounts are treated as 0",
                    m.totalMilestones.length, m.totalMilestoneXp.length);
        }
    }

    @Mod.EventBusSubscriber(modid = WeakSpotMod.MODID)
    public static final class Sync {

        private Sync() {
        }

        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (WeakSpotMod.MODID.equals(event.getModID())) {
                ConfigManager.sync(WeakSpotMod.MODID, Config.Type.INSTANCE);
                SettingsSync.resendToAll();
                warnIfMisconfigured();
            }
        }
    }
}
