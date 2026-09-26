package com.example.weakspot;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.apache.logging.log4j.LogManager;

/**
 * Minecraft の非公開のフィールド・メソッドを、名前で読む。開発環境は MCP 名、実際の環境（reobf 後）は SRG 名なので、
 * 両方の名前を順に試す。（Forge の ReflectionHelper.findField の3引数の版は、起動環境の判定で片方の名前しか試さず、
 * 非推奨でもあるので使わない。）見つからなければ警告を1回出して null を返し、呼ぶ側はその機能を止める（ゲームは止めない）。
 */
public final class Reflect {

    private Reflect() {
    }

    public static Field field(Class<?> owner, String what, String... names) {
        for (String name : names) {
            try {
                Field field = owner.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException | RuntimeException e) {
                // 次の名前を試す
            }
        }
        LogManager.getLogger(WeakSpotMod.MODID).warn("Cannot find field {}.{}; {} is disabled",
                owner.getSimpleName(), String.join("/", names), what);
        return null;
    }

    /** 引数なしのメソッド。 */
    public static Method method(Class<?> owner, String what, String... names) {
        return method(owner, what, new Class<?>[0], names);
    }

    /** 引数の型を指定したメソッド。 */
    public static Method method(Class<?> owner, String what, Class<?>[] parameterTypes, String... names) {
        for (String name : names) {
            try {
                Method method = owner.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException | RuntimeException e) {
                // 次の名前を試す
            }
        }
        LogManager.getLogger(WeakSpotMod.MODID).warn("Cannot find method {}.{}(); {} is disabled",
                owner.getSimpleName(), String.join("/", names), what);
        return null;
    }

    /** 初めて使うときに 1 回だけ探すフィールド（1.8.9。ゲート・エンチャント。起動時に探さず、使わなければ警告も出さない）。 */
    public static LazyField lazyField(Class<?> owner, String what, String... names) {
        return new LazyField(owner, what, names);
    }

    public static final class LazyField {
        private final Class<?> owner;
        private final String what;
        private final String[] names;
        private Field field;
        private boolean resolved;

        private LazyField(Class<?> owner, String what, String[] names) {
            this.owner = owner;
            this.what = what;
            this.names = names;
        }

        /** 見つからなければ null（警告は最初の 1 回だけ）。 */
        public synchronized Field get() {
            if (!resolved) {
                resolved = true;
                field = field(owner, what, names);
            }
            return field;
        }
    }
}
