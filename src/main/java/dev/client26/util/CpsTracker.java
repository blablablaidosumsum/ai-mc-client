package dev.client26.util;

import java.util.Arrays;

public final class CpsTracker {
    private static final int N = 64;
    private static final long[][] TS = new long[2][N];
    private static final int[] HEAD = new int[2];
    private static final boolean[] PREV = new boolean[2];

    static { for (long[] a : TS) Arrays.fill(a, Long.MIN_VALUE / 4); }

    private CpsTracker() {}

    public static void poll(int btn, boolean down) {
        if (down && !PREV[btn]) {
            TS[btn][HEAD[btn]] = System.nanoTime();
            HEAD[btn] = (HEAD[btn] + 1) & (N - 1);
        }
        PREV[btn] = down;
    }

    public static int cps(int btn) {
        long limit = System.nanoTime() - 1_000_000_000L;
        int c = 0;
        long[] a = TS[btn];
        for (int i = 0; i < N; i++) if (a[i] > limit) c++;
        return c;
    }
}
