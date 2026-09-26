package daa;

import java.util.Objects;
import java.util.SplittableRandom;
import java.util.function.IntUnaryOperator;
public final class QuickSorter {
    private QuickSorter() { }

    public static Metrics sort(int[] a) {
        return sort(a, new SplittableRandom()::nextInt);
    }

    public static Metrics sort(int[] a, long seed) {
        return sort(a, new SplittableRandom(seed)::nextInt);
    }

    static Metrics sort(int[] a, IntUnaryOperator pivotOffset) {
        Objects.requireNonNull(a, "array");
        Objects.requireNonNull(pivotOffset, "pivotOffset");
        Metrics metrics = new Metrics();
        if (a.length > 1) sort(a, 0, a.length, 1, pivotOffset, metrics);
        return metrics;
    }

    private static void sort(int[] a, int lo, int hi, int depth,
                             IntUnaryOperator pivotOffset, Metrics m) {
        m.enter(depth);
        while (hi - lo > 1) {
            int pivot = a[lo + pivotOffset.applyAsInt(hi - lo)];
            int lt = lo;
            int i = lo;
            int gt = hi;
            while (i < gt) {
                int comparison = m.compare(a[i], pivot);
                if (comparison < 0) m.swap(a, lt++, i++);
                else if (comparison > 0) m.swap(a, i, --gt);
                else i++;
            }
            if (lt - lo < hi - gt) {
                if (lt - lo > 1) sort(a, lo, lt, depth + 1, pivotOffset, m);
                lo = gt;
            } else {
                if (hi - gt > 1) sort(a, gt, hi, depth + 1, pivotOffset, m);
                hi = lt;
            }
        }
    }
}
