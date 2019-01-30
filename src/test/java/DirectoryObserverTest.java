import org.k3a.observer.impl.DirectoryObserver;

import java.nio.file.Paths;

/**
 * Created by  k3a
 * on 2018/7/27  9:19
 */
public class DirectoryObserverTest {

    public static void main(String[] args) throws InterruptedException {
        long start = System.currentTimeMillis();
        DirectoryObserver observer = DirectoryObserver.get();
        // notice that too many watched files  will case high load and might gonna crush while running
        observer.registerRecursively(Paths.get("/home/xxx"))/*.register(Paths.get("C:\\"))*/.setMinInterval(0).start();
        System.out.println(System.currentTimeMillis() - start);
    }

}
