package daa;

import java.util.Objects;

public final class MergeSorter {
    public static final int INSERTION_CUTOFF = 24;

    private MergeSorter() { }

    public static Metrics sort(int[] a) {
        Objects.requireNonNull(a, "array");
        Metrics metrics = new Metrics();
        if (a.length < 2) return metrics;
        int[] auxiliary = new int[a.length];
        metrics.bufferElements = a.length;
        sort(a, auxiliary, 0, a.length, 1, metrics);
        return metrics;
    }

    private static void sort(int[] a, int[] auxiliary, int lo, int hi,
                             int depth, Metrics m) {
        m.enter(depth);
        if (hi - lo <= INSERTION_CUTOFF) {
            m.insertionSort(a, lo, hi);
            return;
        }
        int mid = lo + (hi - lo) / 2;
        sort(a, auxiliary, lo, mid, depth + 1, m);
        sort(a, auxiliary, mid, hi, depth + 1, m);
        System.arraycopy(a, lo, auxiliary, lo, hi - lo);
        m.arrayWrites += hi - lo;
        int left = lo;
        int right = mid;
        for (int k = lo; k < hi; k++) {
            if (left >= mid) a[k] = auxiliary[right++];
            else if (right >= hi) a[k] = auxiliary[left++];
            else if (m.compare(auxiliary[left], auxiliary[right]) <= 0) {
                a[k] = auxiliary[left++];
            } else a[k] = auxiliary[right++];
            m.arrayWrites++;
        }
    }
}
