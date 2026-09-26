package daa;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class Experiment {
    private static final int[] SIZES = {100, 1000, 10000, 100000, 300000};
    private static final int TRIALS = 7;
    private static final int WARMUPS_PER_CASE = 3;
    private static volatile double sink;
    private record Case(String algorithm, DataSets.Type type, int n) { }
    private record Run(long nanos, Metrics metrics, double checksum, long inputSeed, long pivotSeed) { }

    private Experiment() { }

    public static void run(Path directory) throws IOException {
        Files.createDirectories(directory);
        writeEnvironment(directory.resolve("environment.txt"));
        System.out.println("Divide-and-conquer experiment | Java " + System.getProperty("java.version"));
        System.out.println("7 measured trials; 3 warm-ups per case; deterministic seeds.");
        System.out.println("Times include algorithm counters and internal buffers; exclude generation, copies and checks.");
               for (DataSets.Type type : DataSets.Type.values()) {
            for (String algorithm : new String[] {"MergeSort", "QuickSort", "Select", "ClosestPair"}) {
                for (int t = 0; t < 12; t++) measure(new Case(algorithm, type, 10000), -100 - t, false);
            }
        }
        List<Case> cases = new ArrayList<>();
        for (int n : SIZES) {
            for (DataSets.Type type : DataSets.Type.values()) {
                for (String algorithm : new String[] {"MergeSort", "QuickSort", "Select", "ClosestPair"}) {
                    cases.add(new Case(algorithm, type, n));
                }
            }
        }
        for (DataSets.Type type : DataSets.Type.values()) {
            cases.add(new Case("ClosestPair", type, 2000));
            for (int n : new int[] {100, 1000, 2000}) cases.add(new Case("BruteForce", type, n));
        }
        Collections.shuffle(cases, new Random(20260926));
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(directory.resolve("results.csv"), StandardCharsets.UTF_8))) {
            out.println("algorithm,input_type,n,trial,input_seed,pivot_seed,k,elapsed_ns,max_recursion_depth,recursive_calls,comparisons,swaps,array_writes,distance_evaluations,buffer_elements,checksum");
            System.out.printf("%-12s %-16s %8s %11s %6s %12s%n", "Algorithm", "Input", "n", "median_ms", "depth", "comparisons");
            for (Case c : cases) {
                for (int t = 0; t < WARMUPS_PER_CASE; t++) measure(c, -t - 1, false);
                long[] times = new long[TRIALS];
                long[] comparisons = new long[TRIALS];
                int depth = 0;
                for (int t = 0; t < TRIALS; t++) {
                    Run run = measure(c, t, true);
                    Metrics m = run.metrics();
                    times[t] = run.nanos();
                    comparisons[t] = m.comparisons;
                    depth = Math.max(depth, m.maxRecursionDepth);
                    out.printf(Locale.ROOT, "%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%.17g%n",
                            c.algorithm(), c.type(), c.n(), t + 1, run.inputSeed(), run.pivotSeed(),
                            c.algorithm().equals("Select") ? c.n() / 2 : -1,
                            run.nanos(), m.maxRecursionDepth, m.recursiveCalls, m.comparisons, m.swaps,
                            m.arrayWrites, m.distanceEvaluations, m.bufferElements, run.checksum());
                }
                Arrays.sort(times);
                Arrays.sort(comparisons);
                System.out.printf(Locale.ROOT, "%-12s %-16s %8d %11.4f %6d %12d%n",
                        c.algorithm(), c.type(), c.n(), times[TRIALS / 2] / 1e6, depth, comparisons[TRIALS / 2]);
                out.flush();
            }
            if (out.checkError()) throw new IOException("Failed to write results.csv");
        }
        System.out.println("Saved " + cases.size() * TRIALS + " measured rows to " + directory.resolve("results.csv"));
        System.out.println("Measured small closest-pair cases validated against brute force; sorting/selection against Arrays.sort.");
        System.out.println("Checksum sink: " + sink);
    }

    private static Run measure(Case c, int trial, boolean verify) {
        long inputSeed = 731L + 1_000_003L * c.n() + 101L * trial;
        long pivotSeed = inputSeed ^ 0x5DEECE66DL;
        Metrics m;
        double checksum;
        long start;
        long elapsed;
        if (c.algorithm().equals("ClosestPair") || c.algorithm().equals("BruteForce")) {
            Point[] points = DataSets.points(c.n(), c.type(), inputSeed);
            start = System.nanoTime();
            ClosestPairSolver.Solution result = c.algorithm().equals("ClosestPair")
                    ? ClosestPairSolver.solve(points) : ClosestPairSolver.bruteForce(points);
            elapsed = System.nanoTime() - start;
            m = result.metrics();
            checksum = result.pair().distance();
            if (verify && c.n() <= 2000) {
                double expected = ClosestPairSolver.bruteForce(points).pair().distance();
                if (Double.compare(checksum, expected) != 0) throw new AssertionError("Closest-pair mismatch");
            }
            if (verify && (!result.pair().hasPair() || !Double.isFinite(checksum))) {
                throw new AssertionError("Invalid pair for the finite benchmark domain");
            }
        } else {
            int[] source = DataSets.integers(c.n(), c.type(), inputSeed);
            int[] a = source.clone();
            start = System.nanoTime();
            switch (c.algorithm()) {
                case "MergeSort" -> { m = MergeSorter.sort(a); checksum = a[c.n() / 2]; }
                case "QuickSort" -> { m = QuickSorter.sort(a, pivotSeed); checksum = a[c.n() / 2]; }
                case "Select" -> {
                    DeterministicSelector.Selection selection = DeterministicSelector.select(a, c.n() / 2);
                    m = selection.metrics();
                    checksum = selection.value();
                }
                default -> throw new IllegalArgumentException(c.algorithm());
            }
            elapsed = System.nanoTime() - start;
            if (verify) {
                Arrays.sort(source);
                if (checksum != source[c.n() / 2]) throw new AssertionError("Wrong middle value");
                if (!c.algorithm().equals("Select") && !Arrays.equals(a, source)) {
                    throw new AssertionError("Sorting mismatch");
                }
            }
        }
        sink = checksum;
        return new Run(elapsed, m, checksum, inputSeed, pivotSeed);
    }

    private static void writeEnvironment(Path file) throws IOException {
        String text = "Experiment started (UTC): " + Instant.now() + "\n"
                + "java.version=" + System.getProperty("java.version") + "\n"
                + "java.vm.name=" + System.getProperty("java.vm.name") + "\n"
                + "os.name=" + System.getProperty("os.name") + "\n"
                + "os.arch=" + System.getProperty("os.arch") + "\n"
                + "os.version=" + System.getProperty("os.version") + "\n"
                + "availableProcessors=" + Runtime.getRuntime().availableProcessors() + "\n"
                + "maxMemoryBytes=" + Runtime.getRuntime().maxMemory() + "\n"
                + "jvmArguments=" + ManagementFactory.getRuntimeMXBean().getInputArguments() + "\n"
                + "sizes=" + Arrays.toString(SIZES) + "\n"
                + "trials=" + TRIALS + "\n"
                + "globalWarmups=12 per algorithm/input type, n=10000\n"
                + "perCaseWarmups=" + WARMUPS_PER_CASE + "\n"
                + "closestPairReferenceSizes=[100, 1000, 2000]\n"
                + "instrumented=true\n"
                + "caseShuffleSeed=20260926\n";
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }
}
