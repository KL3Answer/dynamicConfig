package org.k3a.dynamicConfig;

import org.k3a.observer.FileObserver;
import org.k3a.utils.FileUtils;
import org.k3a.utils.SimplePlaceHolderResolver;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Created by HQ.XPS15
 * on 2018/7/1  15:35
 */
@SuppressWarnings("unused")
public class DynamicConfigFactory {

    public static Dynamically<Path, Properties, String, String> createDynamicProperties() {
        return new Dynamically<Path, Properties, String, String>()
                .onConvert(f -> {
                    try (FileInputStream fis = new FileInputStream(f.toFile())) {
                        Properties properties = new Properties();
                        properties.load(fis);
                        properties.forEach((k, v) -> properties.setProperty((String) k, SimplePlaceHolderResolver.resolvePlaceHolder(properties, (String) k, (String) v)));
                        return properties;
                    } catch (Throwable t) {
                        throw new IllegalArgumentException(t);
                    }
                })
                .onSearch((k, b) -> b.getProperty(k))
                .onObserver(FileObserver.get())
                .onAppend((k, v, s) -> FileUtils.appendNewLine(k + "=" + v, s))
                .onErase((k, s) -> {
                    try {
                        FileUtils.write(FileUtils.readPropertiesExclusion(k, s), s);
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e);
                    }
                })
                .onError(Throwable::printStackTrace);
    }
}
