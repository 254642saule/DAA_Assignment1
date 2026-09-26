package daa;
public final class Metrics {
    public long comparisons;
    public long swaps;
    public long arrayWrites;
    public long distanceEvaluations;
    public long recursiveCalls;
    public int maxRecursionDepth;
    public long bufferElements;

    void enter(int depth) {
        recursiveCalls++;
        maxRecursionDepth = Math.max(maxRecursionDepth, depth);
    }

    int compare(int a, int b) {
        comparisons++;
        return Integer.compare(a, b);
    }

    void swap(int[] a, int i, int j) {
        if (i != j) {
            int value = a[i];
            a[i] = a[j];
            a[j] = value;
            swaps++;
            arrayWrites += 2;
        }
    }

    void insertionSort(int[] a, int lo, int hi) {
        for (int i = lo + 1; i < hi; i++) {
            int value = a[i];
            int j = i - 1;
            while (j >= lo && compare(a[j], value) > 0) {
                a[j + 1] = a[j];
                arrayWrites++;
                j--;
            }
            a[j + 1] = value;
            arrayWrites++;
        }
    }

    @Override
    public String toString() {
        return "depth=" + maxRecursionDepth + ", calls=" + recursiveCalls
                + ", comparisons=" + comparisons + ", swaps=" + swaps
                + ", distances=" + distanceEvaluations;
    }
}
