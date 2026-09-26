package daa;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public final class Main {
    private Main() { }

    public static void main(String[] args) throws IOException {
        String command = args.length == 0 ? "demo" : args[0];
        switch (command) {
            case "demo" -> demo();
            case "experiment" -> Experiment.run(Path.of(args.length > 1 ? args[1] : "results"));
            default -> {
                System.err.println("Usage: java -cp target/classes daa.Main [demo|experiment [output-dir]]");
                System.exit(2);
            }
        }
    }

    private static void demo() {
        int[] input = {9, -2, 7, 7, 0, 5, -2, 11};
        System.out.println("ASSIGNMENT 1 | DIVIDE-AND-CONQUER");
        System.out.println("Input: " + Arrays.toString(input));
        int[] merge = input.clone();
        Metrics mergeMetrics = MergeSorter.sort(merge);
        System.out.println("MergeSort: " + Arrays.toString(merge));
        System.out.println("  " + mergeMetrics);
        int[] quick = input.clone();
        Metrics quickMetrics = QuickSorter.sort(quick, 42);
        System.out.println("QuickSort: " + Arrays.toString(quick));
        System.out.println("  " + quickMetrics);
        var selected = DeterministicSelector.select(input.clone(), 3);
        System.out.println("Select k=3 (zero-based): " + selected.value());
        System.out.println("  " + selected.metrics());
        Point[] points = {new Point(0, 0), new Point(3, 4), new Point(3.1, 4.1),
                new Point(-5, 2), new Point(10, 10)};
        var closest = ClosestPairSolver.solve(points);
        System.out.println("Closest pair: " + closest.pair().first() + " and " + closest.pair().second());
        System.out.printf(java.util.Locale.ROOT, "  distance=%.8f%n", closest.pair().distance());
        System.out.println("  " + closest.metrics());
        System.out.println("Edge cases: empty/single arrays supported; invalid selection rank rejected.");
        System.out.println("Run tests: bash scripts/run.sh test");
        System.out.println("Run experiments: bash scripts/run.sh experiment");
    }
}
