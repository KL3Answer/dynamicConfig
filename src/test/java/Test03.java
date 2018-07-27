import org.k3a.observer.DirectoryObserver;

import java.nio.file.Paths;

/**
 * Created by HQ.XPS15
 * on 2018/7/27  9:19
 */
public class Test03 {

    public static void main(String[] args) {
        DirectoryObserver.get().register(Paths.get("E:\\")).setMinInterval(200).start();
        while(true){
            System.currentTimeMillis();
        }
    }

}
