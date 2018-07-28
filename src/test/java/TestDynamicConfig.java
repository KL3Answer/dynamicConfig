import org.k3a.dynamicConfig.Dynamically;
import org.k3a.dynamicConfig.impl.DynamicProperties;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Created by HQ.XPS15
 * on 2018/6/21  16:06
 */
public class TestDynamicConfig {

    public static void main(String[] args) {
        try {
            Dynamically<Path, Properties, String, String> config = DynamicProperties.createWithSystemProps()
                    .link(Paths.get(TestDynamicConfig.class.getResource("config.properties").getPath().substring(1)))
                    .link(Paths.get(TestDynamicConfig.class.getResource("dynamic.properties").getPath().substring(1)));

            config.activate();

            config.link(Paths.get(TestDynamicConfig.class.getResource("test.properties").getPath().substring(1)));
            // which is not exists
            config.link(Paths.get(TestDynamicConfig.class.getResource("").getPath().substring(1), "test01.properties"));

            config.activate();

            config.append("key", "value", Paths.get(TestDynamicConfig.class.getResource("dynamic.properties").getPath().substring(1)));
            config.remove("json", Paths.get(TestDynamicConfig.class.getResource("dynamic.properties").getPath().substring(1)));

            String explicitValue = config.getExplicitValue("database.port", Paths.get(TestDynamicConfig.class.getResource("dynamic.properties").getPath().substring(1)));
            String value = config.getValue("database.port");


            while(true){
                System.currentTimeMillis();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
