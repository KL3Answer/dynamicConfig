import org.k3a.utils.SimplePlaceHolderResolver;

import java.util.Properties;

/**
 * Created by k3a
 * on 19-1-28  上午10:04
 */
public class PlaceHolderTest {

    public static void main(String[] args) {

        final Properties properties = new Properties();
        properties.put("a", "b");
        properties.put("b", "1");
        properties.put("ab", "D");
        properties.put("c", "${a}${a}");

        final int[] stPair = SimplePlaceHolderResolver.get1stPair("${", "}", "}}${}${}");


        String s = null;
//          s = SimplePlaceHolderResolver.DEFAULT.resolvePropertiesPlaceHolder(properties, "Zfsfsfs");
          s = SimplePlaceHolderResolver.DEFAULT.resolvePropertiesPlaceHolder(properties, "${a}${a}");
//          s = SimplePlaceHolderResolver.DEFAULT.resolvePropertiesPlaceHolder(properties, "${a${}}");
//          s = SimplePlaceHolderResolver.DEFAULT.resolvePropertiesPlaceHolder(properties, "${a${b}}");
//          s = SimplePlaceHolderResolver.DEFAULT.resolvePropertiesPlaceHolder(properties, "${a${a}}");
//          s = SimplePlaceHolderResolver.DEFAULT.resolvePropertiesPlaceHolder(properties, "${a}${b}");
//          s = SimplePlaceHolderResolver.DEFAULT.resolvePropertiesPlaceHolder(properties, "${b}asdasds");
//          s = SimplePlaceHolderResolver.DEFAULT.resolvePropertiesPlaceHolder(properties, "${ab}");

        System.out.println(s);


    }
}
