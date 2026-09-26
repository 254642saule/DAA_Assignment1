package daa;

import java.util.Arrays;
import java.util.Objects;

public final class ClosestPairSolver {
    public record Pair(Point first, Point second, double distance) {
        public boolean hasPair() { return first != null; }
    }
    public record Solution(Pair pair, Metrics metrics) { }
    private static final Pair NONE = new Pair(null, null, Double.POSITIVE_INFINITY);

    private ClosestPairSolver() { }

    public static Solution solve(Point[] points) {
        validate(points);
        Metrics m = new Metrics();
        if (points.length < 2) return new Solution(NONE, m);
        Point[] work = points.clone();
        m.arrayWrites += work.length;
        Arrays.sort(work, (a, b) -> {
            m.comparisons++;
            int x = Double.compare(a.x(), b.x());
            return x != 0 ? x : Double.compare(a.y(), b.y());
        });
        Point[] auxiliary = new Point[points.length];
        Point[] strip = new Point[points.length];
        m.bufferElements = 3L * points.length;
        Pair pair = solve(work, auxiliary, strip, 0, work.length, 1, m);
        return new Solution(pair, m);
    }

    private static Pair solve(Point[] work, Point[] auxiliary, Point[] strip,
                              int lo, int hi, int depth, Metrics m) {
        m.enter(depth);
        if (hi - lo <= 3) {
            Pair best = bruteForce(work, lo, hi, m);
            // Constant-size insertion sort establishes the y-order invariant.
            for (int i = lo + 1; i < hi; i++) {
                Point p = work[i];
                int j = i - 1;
                while (j >= lo && compareY(work[j], p, m) > 0) {
                    work[j + 1] = work[j--];
                    m.arrayWrites++;
                }
                work[j + 1] = p;
                m.arrayWrites++;
            }
            return best;
        }
        int mid = lo + (hi - lo) / 2;
        double middleX = work[mid].x();
        Pair left = solve(work, auxiliary, strip, lo, mid, depth + 1, m);
        Pair right = solve(work, auxiliary, strip, mid, hi, depth + 1, m);
        Pair best = left.distance() <= right.distance() ? left : right;
        int i = lo;
        int j = mid;
        for (int k = lo; k < hi; k++) {
            if (i >= mid) auxiliary[k] = work[j++];
            else if (j >= hi) auxiliary[k] = work[i++];
            else if (compareY(work[i], work[j], m) <= 0) auxiliary[k] = work[i++];
            else auxiliary[k] = work[j++];
            m.arrayWrites++;
        }
        System.arraycopy(auxiliary, lo, work, lo, hi - lo);
        m.arrayWrites += hi - lo;
        int count = 0;
        for (int k = lo; k < hi; k++) {
            m.comparisons++;
            if (Math.abs(work[k].x() - middleX) < best.distance()) {
                strip[count++] = work[k];
                m.arrayWrites++;
            }
        }
        for (i = 0; i < count; i++) {
            // The packing argument bounds useful successors by seven.
            for (j = i + 1; j < count && j <= i + 7; j++) {
                m.comparisons++;
                if (strip[j].y() - strip[i].y() >= best.distance()) break;
                double distance = distance(strip[i], strip[j], m);
                m.comparisons++;
                if (distance < best.distance()) best = new Pair(strip[i], strip[j], distance);
            }
        }
        return best;
    }

    public static Solution bruteForce(Point[] points) {
        validate(points);
        Metrics m = new Metrics();
        return new Solution(bruteForce(points, 0, points.length, m), m);
    }

    private static Pair bruteForce(Point[] points, int lo, int hi, Metrics m) {
        Pair best = NONE;
        for (int i = lo; i < hi; i++) {
            for (int j = i + 1; j < hi; j++) {
                double distance = distance(points[i], points[j], m);
                m.comparisons++;
                if (!best.hasPair() || distance < best.distance()) {
                    best = new Pair(points[i], points[j], distance);
                }
            }
        }
        return best;
    }

    private static double distance(Point a, Point b, Metrics m) {
        m.distanceEvaluations++;
        return Math.hypot(a.x() - b.x(), a.y() - b.y());
    }

    private static int compareY(Point a, Point b, Metrics m) {
        m.comparisons++;
        int y = Double.compare(a.y(), b.y());
        return y != 0 ? y : Double.compare(a.x(), b.x());
    }

    private static void validate(Point[] points) {
        Objects.requireNonNull(points, "points");
        for (Point point : points) Objects.requireNonNull(point, "point");
    }
}
