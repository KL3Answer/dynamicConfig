import org.k3a.dynamicConfig.Dynamically;
import org.k3a.dynamicConfig.impl.DynamicProperties;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Created by k3a
 * on 2018/6/21  16:06
 */
public class DynamicConfigTest {

    public static void main(String[] args) {
        try {
            //
            Dynamically<Path, Properties, String, String> config = DynamicProperties.createWithSystemProps()
                    .link(Paths.get(DynamicConfigTest.class.getResource("config.properties").getPath()))
                    // note: take the first '/'  away when running  on Windows
//                    .link(Paths.get(DynamicConfigTest.class.getResource("config.properties").getPath().substring(1)))
                    .link(Paths.get(DynamicConfigTest.class.getResource("dynamic.properties").getPath()));

            config.activate();

            //add something new and active it
            config.link(Paths.get(DynamicConfigTest.class.getResource("test.properties").getPath()));
            // which is not exists
            config.link(Paths.get(DynamicConfigTest.class.getResource("").getPath(), "test01.properties"));
            config.activate();

            //changes will be written into files
            config.append("key", "value", Paths.get(DynamicConfigTest.class.getResource("dynamic.properties").getPath()));
            config.remove("json", Paths.get(DynamicConfigTest.class.getResource("dynamic.properties").getPath()));

            // from explicit file
            System.out.println(config.getExplicitValue("database.port", Paths.get(DynamicConfigTest.class.getResource("dynamic.properties").getPath())));
            //from all registered files
            System.out.println(config.getValue("database.port"));


            final String sdssf = config.getValue("sdssf");
            System.out.println();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
