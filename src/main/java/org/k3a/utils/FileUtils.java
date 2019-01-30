package org.k3a.utils;

import java.io.*;
import java.nio.file.Path;
import java.util.LinkedList;

/**
 * Created by  k3a
 * on 2018/6/30  22:22
 */
@SuppressWarnings("UnusedReturnValue")
public class FileUtils {

    public static void appendNewLine(String content, Path path) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path.toFile(), true))) {
            bw.newLine();
            bw.append(content);
        }
    }

    public static void write(LinkedList<String> list, Path path) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path.toFile()))) {
            while (list.size() > 1) {
                bw.write(list.removeFirst());
                bw.newLine();
            }
            bw.write(list.removeFirst());
        }
    }

    /**
     * remove key line from properties file
     */
    public static LinkedList<String> readPropertiesExcept(final String key, Path path) throws IOException {
        final LinkedList<String> list = new LinkedList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path.toFile()))) {
            String tmp = null;
            while ((tmp = br.readLine()) != null) {
                if (tmp.trim().startsWith(key + "="))
                    continue;
                list.add(tmp);
            }
        }
        return list;
    }


    public static LinkedList<String> readExcept(final long lineNum, Path path) throws IOException {
        final LinkedList<String> list = new LinkedList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path.toFile()))) {
            int currentNum = 0;
            String tmp = null;
            while ((tmp = br.readLine()) != null) {
                ++currentNum;
                if (currentNum == lineNum)
                    continue;
                list.add(tmp);
            }
        }
        return list;
    }


}
