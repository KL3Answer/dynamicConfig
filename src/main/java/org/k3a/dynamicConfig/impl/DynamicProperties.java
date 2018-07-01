package org.k3a.dynamicConfig.impl;

import org.k3a.dynamicConfig.DynamicFile;
import org.k3a.dynamicConfig.Dynamically;
import org.k3a.utils.FileUtils;
import org.k3a.utils.SimplePlaceHolderResolver;
import org.k3a.utils.TriConsumer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 动态读取 properties 文件
 */
@SuppressWarnings({"WeakerAccess", "unused", "UnusedReturnValue"})
public class DynamicProperties extends DynamicFile<Properties, String, String> {

    private DynamicProperties() {
    }

    public static Dynamically<Path, Properties, String, String> create() {
        return new DynamicProperties();
    }

    public static Dynamically<Path, Properties, String, String> create(Consumer<Throwable> errorHandler) {
        return create().onError(errorHandler);
    }

    public static Dynamically<Path, Properties, String, String> createWithSystemProps() {
        return ((DynamicProperties) create()).addSystemProperties();
    }

    public static Dynamically<Path, Properties, String, String> createWithSystemProps(Consumer<Throwable> errorHandler) {
        return ((DynamicProperties) create().onError(errorHandler)).addSystemProperties();
    }

    /**
     * add system properties
     */
    public Dynamically<Path, Properties, String, String> addSystemProperties() {
        try {
            final String key = "System";
            VALUES.put(Paths.get(key), System.getProperties());
            observer.onReject(p -> {
                if (!key.equals(p.toString()))
                    throw new IllegalArgumentException("parent of path:" + p + "can not be null");
            });
        } catch (Throwable t) {
            commonErrorHandler.accept(t);
        }
        return this;
    }

    @Override
    protected BiFunction<String, Properties, String> defaultSearcher() {
        return (k, b) -> b.getProperty(k);
    }

    @Override
    protected Function<Path, Properties> defaultConvertor() {
        return f -> {
            try (FileInputStream fis = new FileInputStream(f.toFile())) {
                Properties properties = new Properties();
                properties.load(fis);
                properties.forEach((k, v) -> properties.setProperty((String) k, SimplePlaceHolderResolver.resolvePlaceHolder(properties, (String) k, (String) v)));
                return properties;
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        };
    }

    @Override
    protected TriConsumer<String, String, Path> defaultAppender() {
        return (k, v, s) -> FileUtils.appendNewLine(k + "=" + v, s);
    }

    @Override
    protected BiConsumer<String, Path> defaultEraser() {
        return (k, s) -> {
            try {
                FileUtils.write(FileUtils.readPropertiesExclusion(k, s), s);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        };
    }
}
