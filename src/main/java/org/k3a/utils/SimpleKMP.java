package org.k3a.utils;

/**
 * KMP 算法简单实现
 */
public class SimpleKMP {

    private SimpleKMP() {
    }

    /**
     * 获取跳转值
     */
    protected static int[] next(char p[]) {
        final int pl = p.length;
        final int[] next = new int[pl];
        int k = -1;
        int j = 0;
        next[0] = -1;
        while (j < pl - 1) {
            if (k == -1 || p[j] == p[k]) {
                k++;
                j++;
                if (p[j] != p[k]) {
                    next[j] = k;
                } else {
                    next[j] = next[k];
                }
            } else {
                k = next[k];
            }
        }
        return next;
    }

    /**
     * -1 表示没有找到
     */
    public static int indexOf(String source, String pattern) {
        int i = 0, j = 0;
        final char[] s = source.toCharArray();
        final char[] p = pattern.toCharArray();
        final int sl = s.length;
        final int pl = p.length;
        //预处理获取前缀函数
        final int[] next = next(p);
        while (i < sl && j < pl) {
            if (j == -1 || s[i] == p[j]) {
                i++;
                j++;
            } else {
                j = next[j];
            }
        }
        if (j == pl)
            return i - j;
        return -1;
    }

    public static int[] allIndexOf(String source, String pattern) {
        int i = SimpleKMP.indexOf(source, pattern);
        if (i < 0) {
            return new int[]{-1};
        } else {
            final int[] tmp = new int[source.length() / pattern.length()];
            int a = 0;
            tmp[a++] = i;
            while (i < source.length() - pattern.length() && (i = i + pattern.length() + SimpleKMP.indexOf(source.substring(i + pattern.length()), pattern)) > 0) {
                tmp[a++] = i;
            }
            if (a == tmp.length) {
                return tmp;
            }
            final int[] rs = new int[a];
            System.arraycopy(tmp, 0, rs, 0, a);
            return rs;
        }
    }

}