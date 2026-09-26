package daa;

import java.util.Objects;

public final class DeterministicSelector {
    public record Selection(int value, Metrics metrics) { }

    private DeterministicSelector() { }

    public static Selection select(int[] a, int k) {
        Objects.requireNonNull(a, "array");
        if (k < 0 || k >= a.length) {
            throw new IllegalArgumentException("k must satisfy 0 <= k < array length");
        }
        Metrics metrics = new Metrics();
        int value = select(a, 0, a.length, k, 1, metrics);
        return new Selection(value, metrics);
    }

    private static int select(int[] a, int lo, int hi, int k, int depth, Metrics m) {
        m.enter(depth);
        if (hi - lo <= 5) {
            m.insertionSort(a, lo, hi);
            return a[k];
        }
        int groups = 0;
        for (int start = lo; start < hi; start += 5) {
            int end = Math.min(start + 5, hi);
            m.insertionSort(a, start, end);
            int median = start + (end - start) / 2;
            // Store medians in the same array, avoiding temporary subarrays.
            m.swap(a, lo + groups, median);
            groups++;
        }
        int pivot = select(a, lo, lo + groups, lo + groups / 2, depth + 1, m);
        int lt = lo;
        int i = lo;
        int gt = hi;
        while (i < gt) {
            int comparison = m.compare(a[i], pivot);
            if (comparison < 0) m.swap(a, lt++, i++);
            else if (comparison > 0) m.swap(a, i, --gt);
            else i++;
        }
        if (k < lt) return select(a, lo, lt, k, depth + 1, m);
        if (k >= gt) return select(a, gt, hi, k, depth + 1, m);
        return pivot;
    }
}
