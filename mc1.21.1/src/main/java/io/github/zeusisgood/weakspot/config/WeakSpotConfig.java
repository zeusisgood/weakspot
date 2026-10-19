package io.github.zeusisgood.weakspot.config;

import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.GrowthFilters;
import io.github.zeusisgood.weakspot.server.SettingsSync;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * [サーバー] はサーバーの値が正で、ログイン時と変更時にクライアントへ送る（SyncedSettings）。
 * クライアントは接続中、自分の [サーバー] の値を使わない。
 * <p>
 * 項目は 1.12.2 と同じ ServerConfig・ClientConfig のフィールドで持ち、そこから ModConfigSpec を作る（コードからは
 * 1.12.2 と同じく WeakSpotConfig.server.mining.boostMultiplier の形で読む）。ファイルは config/weakspot-server.toml
 * （COMMON。ワールドごとの serverconfig/ に置かず、Modpack の作者・サーバーの管理者が 1 か所で直せるように）と
 * config/weakspot-client.toml。
 */
public final class WeakSpotConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeakSpotMod.MODID);

    public static final ServerConfig server = new ServerConfig();
    public static final ClientConfig client = new ClientConfig();

    private static final Map<Setting, ModConfigSpec.ConfigValue<?>> VALUES = new LinkedHashMap<>();
    private static ModConfigSpec serverSpec;
    private static ModConfigSpec clientSpec;

    private WeakSpotConfig() {
    }

    public static void register(ModContainer container) {
        serverSpec = buildSpec("server");
        clientSpec = buildSpec("client");
        container.registerConfig(ModConfig.Type.COMMON, serverSpec, WeakSpotMod.MODID + "-server.toml");
        container.registerConfig(ModConfig.Type.CLIENT, clientSpec, WeakSpotMod.MODID + "-client.toml");
    }

    private static ModConfigSpec buildSpec(String root) {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        List<String> pushed = new ArrayList<>();
        for (Setting setting : settings()) {
            if (!setting.category.equals(root) && !setting.category.startsWith(root + ".")) {
                continue;
            }
            List<String> path = Arrays.asList(setting.category.split("\\."));
            int common = 0;
            while (common < pushed.size() && common < path.size() && pushed.get(common).equals(path.get(common))) {
                common++;
            }
            while (pushed.size() > common) {
                builder.pop();
                pushed.remove(pushed.size() - 1);
            }
            while (pushed.size() < path.size()) {
                builder.push(path.get(pushed.size()));
                pushed.add(path.get(pushed.size()));
            }
            VALUES.put(setting, define(builder, setting));
        }
        while (!pushed.isEmpty()) {
            builder.pop();
            pushed.remove(pushed.size() - 1);
        }
        return builder.build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ModConfigSpec.ConfigValue<?> define(ModConfigSpec.Builder builder, Setting setting) {
        Field field = setting.field;
        Config.Comment comment = field.getAnnotation(Config.Comment.class);
        if (comment != null) {
            builder.comment(comment.value());
        }
        String name = setting.name();
        Object value = setting.get();
        Class<?> type = field.getType();
        if (type == boolean.class) {
            return builder.define(name, (boolean) value);
        }
        if (type == int.class) {
            Config.RangeInt range = field.getAnnotation(Config.RangeInt.class);
            return builder.defineInRange(name, (int) value, range != null ? range.min() : Integer.MIN_VALUE,
                    range != null ? range.max() : Integer.MAX_VALUE);
        }
        if (type == double.class) {
            Config.RangeDouble range = field.getAnnotation(Config.RangeDouble.class);
            return builder.defineInRange(name, (double) value, range != null ? range.min() : -Double.MAX_VALUE,
                    range != null ? range.max() : Double.MAX_VALUE);
        }
        if (type.isEnum()) {
            return builder.defineEnum(name, (Enum) value);
        }
        if (type == String[].class) {
            return builder.defineListAllowEmpty(name, Arrays.asList((String[]) value), () -> "",
                    o -> o instanceof String);
        }
        if (type == int[].class) {
            List<Integer> list = new ArrayList<>();
            for (int i : (int[]) value) {
                list.add(i);
            }
            return builder.defineListAllowEmpty(name, list, () -> 0, o -> o instanceof Integer);
        }
        return builder.define(name, (String) value);
    }

    /** 設定ファイルを読んだとき・書き換わったときに、フィールドへ写す。 */
    public static void onLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() != serverSpec && event.getConfig().getSpec() != clientSpec) {
            return;
        }
        copyFromSpecs();
        if (event.getConfig().getSpec() == serverSpec) {
            warnIfMisconfigured();
            if (event instanceof ModConfigEvent.Reloading) {
                // ファイルを書き換えて保存すると、NeoForge が読み直す（1.12.2 の設定画面で変えたときと同じく送り直す）
                SettingsSync.resendToAll();
            }
        }
    }

    /**
    * /weakspot reload。ファイルの読み直しそのものは、保存したときに NeoForge が済ませている（ModConfigEvent.Reloading）。
    * ここでは今の値をフィールドへ写し直す。
    */
    public static boolean reloadFromFile() {
        copyFromSpecs();
        return serverSpec.isLoaded();
    }

    private static void copyFromSpecs() {
        for (Map.Entry<Setting, ModConfigSpec.ConfigValue<?>> e : VALUES.entrySet()) {
            ModConfigSpec.ConfigValue<?> value = e.getValue();
            if (isLoaded(value)) {
                e.getKey().setFromFile(value.get());
            }
        }
    }

    private static boolean isLoaded(ModConfigSpec.ConfigValue<?> value) {
        try {
            value.get();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /** 統計画面などから [クライアント] の値を書き換えた後に呼ぶ。 */
    public static void save() {
        saveSpec(clientSpec, "client");
    }

    /**
     * 設定画面から [サーバー] の値を書き換えた後に呼ぶ（シングルプレイ・LAN のホスト、またはワールドの外）。
     * この側でサーバーが動いていれば、つないでいる全員へ送り直す。
     */
    public static void saveServer() {
        saveSpec(serverSpec, "server");
        SettingsSync.resendToAll();
        warnIfMisconfigured();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void saveSpec(ModConfigSpec spec, String root) {
        if (!spec.isLoaded()) {
            return;
        }
        for (Map.Entry<Setting, ModConfigSpec.ConfigValue<?>> e : VALUES.entrySet()) {
            if (e.getKey().category.startsWith(root)) {
                ((ModConfigSpec.ConfigValue) e.getValue()).set(e.getKey().toFile());
            }
        }
        spec.save();
    }

    /** category は設定ファイルのカテゴリ（例: server.mining）。 */
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

        /** 1.12.2 の Forge の決まりと同じ: weakspot.カテゴリ.小文字の名前。 */
        public String nameKey() {
            return WeakSpotMod.MODID + "." + category + "." + name().toLowerCase(Locale.ENGLISH);
        }

        public String tooltipKey() {
            return nameKey() + ".tooltip";
        }

        /** value はフィールドと同じ型（配列は配列のまま）。保存は WeakSpotConfig.save / saveServer。 */
        public void set(Object value) {
            try {
                field.set(owner, value);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        void setFromFile(Object value) {
            Class<?> type = field.getType();
            Object v = value;
            if (type == String[].class) {
                List<?> list = (List<?>) value;
                v = list.stream().map(String::valueOf).toArray(String[]::new);
            } else if (type == int[].class) {
                List<?> list = (List<?>) value;
                int[] array = new int[list.size()];
                for (int i = 0; i < array.length; i++) {
                    array[i] = ((Number) list.get(i)).intValue();
                }
                v = array;
            }
            try {
                field.set(owner, v);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        Object toFile() {
            Object value = get();
            if (value instanceof String[]) {
                return Arrays.asList((String[]) value);
            }
            if (value instanceof int[]) {
                List<Integer> list = new ArrayList<>();
                for (int i : (int[]) value) {
                    list.add(i);
                }
                return list;
            }
            return value;
        }
    }

    private static List<Setting> settings;

    /** カテゴリの順。 */
    public static synchronized List<Setting> settings() {
        if (settings == null) {
            List<Setting> list = new ArrayList<>();
            try {
                collect(ServerConfig.class, server, "server", list);
                collect(ClientConfig.class, client, "client", list);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
            settings = Collections.unmodifiableList(list);
        }
        return settings;
    }

    /** なければ null。 */
    public static Setting setting(String name) {
        for (Setting setting : settings()) {
            if (setting.name().equals(name)) {
                return setting;
            }
        }
        return null;
    }

    /** 1.12.2 の ConfigManager と同じ決まりでたどる（入れ子は static でない public のフィールド）。 */
    private static void collect(Class<?> type, Object owner, String category, List<Setting> out)
            throws IllegalAccessException {
        for (Field field : type.getDeclaredFields()) {
            int mod = field.getModifiers();
            if (!Modifier.isPublic(mod) || Modifier.isStatic(mod)) {
                continue;
            }
            Class<?> t = field.getType();
            if (t.isPrimitive() || t.isArray() || t.isEnum() || t == String.class) {
                out.add(new Setting(category, field, owner));
                continue;
            }
            Config.Name name = field.getAnnotation(Config.Name.class);
            String sub = category + "." + (name != null ? name.value() : field.getName()).toLowerCase(Locale.ENGLISH);
            collect(t, field.get(owner), sub, out);
        }
    }

    /** 足りない分は 0 として扱う。 */
    public static void warnIfMisconfigured() {
        for (String entry : GrowthFilters.parse(Arrays.asList(server.growth.growthExtraBlocks)).invalid()) {
            LOGGER.warn("weakspot-server.toml: ignored an unreadable growthExtraBlocks entry: \"{}\" "
                    + "(write modid:block or modid:block[property=pattern,...])", entry);
        }
        ServerConfig.Milestones m = server.milestones;
        if (m.milestoneXp.length != m.milestones.length || m.milestoneRepair.length != m.milestones.length) {
            LOGGER.warn("weakspot-server.toml: milestones has {} entries but milestoneXp has {} and milestoneRepair has {}; "
                    + "missing amounts are treated as 0", m.milestones.length, m.milestoneXp.length, m.milestoneRepair.length);
        }
        if (m.totalMilestoneXp.length != m.totalMilestones.length) {
            LOGGER.warn("weakspot-server.toml: totalMilestones has {} entries but totalMilestoneXp has {}; "
                    + "missing amounts are treated as 0", m.totalMilestones.length, m.totalMilestoneXp.length);
        }
    }
}
