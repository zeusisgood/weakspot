package io.github.zeusisgood.weakspot.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 設定の項目の注釈。1.12.2 の Forge の @Config と同じ名前にして、ServerConfig・ClientConfig を両方の版で同じ形に保つ
 * （WeakSpotConfig がこれを読んで ModConfigSpec を作る）。
 */
public final class Config {

    private Config() {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Comment {
        String[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface RangeInt {
        int min() default Integer.MIN_VALUE;

        int max() default Integer.MAX_VALUE;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface RangeDouble {
        double min() default -Double.MAX_VALUE;

        double max() default Double.MAX_VALUE;
    }

    /** カテゴリの名前を、フィールドの名前と変えるとき（throw は Java の予約語）。 */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Name {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface LangKey {
        String value();
    }
}
