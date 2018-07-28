import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Created by HQ.XPS15
 * on 2018/7/27  23:43
 */
public class Test03 {

    public static void main(String[] args) throws IOException {

        long start = System.currentTimeMillis();

        try {

            Files.newDirectoryStream(Paths.get("c:\\")).forEach(System.out::println);

//
//            Path path = Files.walkFileTree(Paths.get("C:\\"), new Search("1"));

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(System.currentTimeMillis() - start);
    }
}

class Search implements FileVisitor<Path> {

    private final PathMatcher matcher;

    public Search(String glob) {
        matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
    }

    void search(Path file) throws IOException {
        Path name = file.getFileName();
        if (name != null && matcher.matches(name)) {
            System.out.println("Searched file was found: " + name +
                    " in " + file.toRealPath().toString());
        }
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        System.out.println("Visited: " + (Path) dir);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        search((Path) file);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }
}
