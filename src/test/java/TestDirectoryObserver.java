import org.k3a.observer.impl.DirectoryObserver;

import java.nio.file.Paths;

/**
 * Created by HQ.XPS15
 * on 2018/7/27  9:19
 */
public class TestDirectoryObserver {

    public static void main(String[] args) throws InterruptedException {
        long start = System.currentTimeMillis();
        DirectoryObserver observer = DirectoryObserver.get();
        observer.registerRecursively(Paths.get("e:\\"))/*.register(Paths.get("C:\\"))*/.setMinInterval(200).start();
        System.out.println(System.currentTimeMillis() - start);
        while(true){
            System.currentTimeMillis();
        }

    }

}
