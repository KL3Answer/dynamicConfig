import org.k3a.dynamicConfig.Dynamically;
import org.k3a.dynamicConfig.impl.DynamicProperties;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Created by HQ.XPS15
 * on 2018/6/21  16:06
 */
public class Test01 {

    public static void main(String[] args) {
        try {
            Dynamically<Path, Properties, String, String> config = DynamicProperties.createWithSystemProps()
                    .register(Paths.get(Test01.class.getResource("config.properties").getPath().substring(1)))
                    .register(Paths.get(Test01.class.getResource("dynamic.properties").getPath().substring(1)));

            config.activate();

            config.register(Paths.get(Test01.class.getResource("test.properties").getPath().substring(1)));
            // which is not exists
            config.register(Paths.get(Test01.class.getResource("").getPath().substring(1), "test01.properties"));

            config.activate();

            config.append("key", "value", Paths.get(Test01.class.getResource("dynamic.properties").getPath().substring(1)));
            config.remove("json", Paths.get(Test01.class.getResource("dynamic.properties").getPath().substring(1)));

            String explicitValue = config.getExplicitValue("database.port", Paths.get(Test01.class.getResource("dynamic.properties").getPath().substring(1)));
            String value = config.getValue("database.port");


            while(true){
                System.currentTimeMillis();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
