package daa;

import java.util.Arrays;
import java.util.SplittableRandom;

public final class AlgorithmTests {
    private static int checks;
    private static int sortingCases;
    private static int selectionCases;
    private static int pointCases;

    private AlgorithmTests() { }

    public static void main(String[] args) {
        sorting();
        System.out.println("PASS sorting: " + sortingCases + " cases per sorter vs Arrays.sort");
        selection();
        System.out.println("PASS selection: " + selectionCases + " ranks vs Arrays.sort(a)[k]");
        closestPair();
        System.out.println("PASS closest pair: " + pointCases + " datasets vs quadratic reference");
        contracts();
        System.out.println("PASS contracts: invalid arguments, non-finite points, input preservation");
        System.out.println("PASS stack: smaller-first QuickSort, including forced extreme pivots");
        System.out.println("ALL TESTS PASSED: " + checks + " checks");
    }

    private static void sorting() {
        int[][] edges = {{}, {7}, {2, 1}, {1, 1}, {-1, 0, -1, 4},
                {Integer.MAX_VALUE, 0, Integer.MIN_VALUE, Integer.MAX_VALUE}};
        for (int[] a : edges) checkSort(a, 10);
        for (int n : new int[] {2, 3, 5, 23, 24, 25, 31, 100, 1000, 10000}) {
            for (DataSets.Type type : DataSets.Type.values()) {
                for (long seed = 0; seed < 5; seed++) checkSort(DataSets.integers(n, type, seed), seed);
            }
        }
        SplittableRandom random = new SplittableRandom(811);
        for (int t = 0; t < 200; t++) {
            checkSort(DataSets.integers(random.nextInt(3000), DataSets.Type.RANDOM, t), t);
        }
        int[] allEqual = new int[100000];
        Metrics same = QuickSorter.sort(allEqual, 9);
        check(same.comparisons == allEqual.length, "all-equal quicksort uses one partition");
        check(same.maxRecursionDepth == 1, "all-equal quicksort depth");
        int[] descending = new int[2000];
        for (int i = 0; i < descending.length; i++) descending[i] = descending.length - i;

        Metrics adversarial = QuickSorter.sort(descending, bound -> 0);
        check(isSorted(descending), "forced extreme-pivot quicksort correct");
        check(adversarial.maxRecursionDepth == 1, "worst pivot sequence still constant stack");
        check(adversarial.comparisons > 1_000_000, "forced pivot sequence performs quadratic work");
    }

    private static void checkSort(int[] original, long seed) {
        int[] expected = original.clone();
        Arrays.sort(expected);
        int[] merge = original.clone();
        int[] quick = original.clone();
        MergeSorter.sort(merge);
        Metrics m = QuickSorter.sort(quick, seed);
        check(Arrays.equals(expected, merge), "merge sort agrees with Arrays.sort");
        check(Arrays.equals(expected, quick), "quick sort agrees with Arrays.sort");
        int bound = original.length < 2 ? 0 : 32 - Integer.numberOfLeadingZeros(original.length);
        check(m.maxRecursionDepth <= bound, "quicksort stack bound");
        sortingCases++;
    }

    private static void selection() {
        SplittableRandom random = new SplittableRandom(912);
        for (int t = 0; t < 500; t++) {
            int n = random.nextInt(1, 2501);
            int[] a = DataSets.integers(n, DataSets.Type.values()[t % 4], t);
            checkSelect(a, random.nextInt(n));
            checkSelect(a, 0);
            checkSelect(a, n - 1);
        }
        for (int n = 1; n <= 6; n++) {
            int combinations = (int) Math.pow(3, n);
            for (int mask = 0; mask < combinations; mask++) {
                int[] a = new int[n];
                int code = mask;
                for (int i = 0; i < n; i++) { a[i] = code % 3 - 1; code /= 3; }
                for (int k = 0; k < n; k++) checkSelect(a, k);
            }
        }
        for (int n : new int[] {4, 5, 6, 9, 10, 11, 24, 25, 26, 100000}) {
            int[] a = DataSets.integers(n, DataSets.Type.DUPLICATE_HEAVY, n);
            checkSelect(a, n / 2);
        }
        int[] extremes = {Integer.MIN_VALUE, 0, Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (int k = 0; k < extremes.length; k++) checkSelect(extremes, k);
    }

    private static void checkSelect(int[] original, int k) {
        int[] expected = original.clone();
        Arrays.sort(expected);
        int[] actual = original.clone();
        int value = DeterministicSelector.select(actual, k).value();
        check(value == expected[k], "selection agrees with sorted rank");
        check(actual[k] == value, "selected value occupies the requested rank");
        Arrays.sort(actual);
        check(Arrays.equals(actual, expected), "selection preserves multiset");
        selectionCases++;
    }

    private static void closestPair() {
        checkPoints(new Point[] {});
        checkPoints(new Point[] {new Point(0, 0)});
        checkPoints(new Point[] {new Point(0, 0), new Point(3, 4)});
        checkPoints(new Point[] {new Point(4, 4), new Point(4, 4), new Point(5, 6)});
        checkPoints(new Point[] {new Point(-100, 0), new Point(-0.1, 0),
                                new Point(0.1, 0), new Point(100, 0)});
        checkPoints(new Point[] {new Point(1e150, 1e150), new Point(-1e150, -1e150),
                                new Point(1e150, 1e150 + 1e140)});
        checkPoints(new Point[] {new Point(0, 0), new Point(1e-200, 1e-200), new Point(3e-200, 0)});
        checkPoints(new Point[] {new Point(-Double.MAX_VALUE, 0), new Point(Double.MAX_VALUE, 0)});
        Point[] vertical = new Point[120];
        Point[] horizontal = new Point[120];
        Point[] grid = new Point[100];
        for (int i = 0; i < 120; i++) {
            vertical[i] = new Point(5, i * 0.25);
            horizontal[i] = new Point(i * 0.25, -2);
        }
        for (int i = 0; i < 100; i++) grid[i] = new Point(i % 10, i / 10);
        checkPoints(vertical);
        checkPoints(horizontal);
        checkPoints(grid);
        SplittableRandom random = new SplittableRandom(442);
        for (int t = 0; t < 160; t++) {
            checkPoints(DataSets.points(random.nextInt(2, 251), DataSets.Type.values()[t % 4], t));
        }
        for (DataSets.Type type : DataSets.Type.values()) checkPoints(DataSets.points(2000, type, 19));
        Point[] large = new Point[100000];
        for (int i = 0; i < large.length; i++) large[i] = new Point(i * 2.0, 0);
        check(ClosestPairSolver.solve(large).pair().distance() == 2, "large known-distance dataset");
    }

    private static void checkPoints(Point[] points) {
        Point[] unchanged = points.clone();
        ClosestPairSolver.Pair expected = ClosestPairSolver.bruteForce(points).pair();
        ClosestPairSolver.Pair actual = ClosestPairSolver.solve(points).pair();
        check(Double.compare(expected.distance(), actual.distance()) == 0, "closest distance matches reference");
        check(actual.hasPair() == (points.length >= 2), "closest pair empty contract");
        check(Arrays.equals(points, unchanged), "closest pair preserves input order");
        if (actual.hasPair()) {
            double distance = Math.hypot(actual.first().x() - actual.second().x(),
                                         actual.first().y() - actual.second().y());
            check(Double.compare(distance, actual.distance()) == 0, "returned pair realizes distance");
            check(Arrays.asList(points).contains(actual.first()) && Arrays.asList(points).contains(actual.second()),
                    "returned pair belongs to input");
        }
        pointCases++;
    }

    private static void contracts() {
        expect(NullPointerException.class, () -> MergeSorter.sort(null));
        expect(NullPointerException.class, () -> QuickSorter.sort(null));
        expect(NullPointerException.class, () -> DeterministicSelector.select(null, 0));
        expect(IllegalArgumentException.class, () -> DeterministicSelector.select(new int[0], 0));
        expect(IllegalArgumentException.class, () -> DeterministicSelector.select(new int[] {1}, -1));
        expect(IllegalArgumentException.class, () -> DeterministicSelector.select(new int[] {1}, 1));
        expect(NullPointerException.class, () -> ClosestPairSolver.solve(null));
        expect(NullPointerException.class, () -> ClosestPairSolver.solve(new Point[] {null}));
        expect(IllegalArgumentException.class, () -> new Point(Double.NaN, 0));
        expect(IllegalArgumentException.class, () -> new Point(0, Double.POSITIVE_INFINITY));
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); }
        catch (Throwable failure) {
            check(type.isInstance(failure), "expected " + type.getSimpleName());
            return;
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }

    private static boolean isSorted(int[] a) {
        for (int i = 1; i < a.length; i++) if (a[i - 1] > a[i]) return false;
        return true;
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
