package daa;

import java.util.Arrays;
import java.util.Comparator;
import java.util.SplittableRandom;

public final class DataSets {
    public enum Type { RANDOM, SORTED, REVERSE_SORTED, DUPLICATE_HEAVY }
    private DataSets() { }

    public static int[] integers(int n, Type type, long seed) {
        SplittableRandom random = new SplittableRandom(seed);
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = type == Type.DUPLICATE_HEAVY ? random.nextInt(8) : random.nextInt();
        }
        if (type == Type.SORTED || type == Type.REVERSE_SORTED) Arrays.sort(a);
        if (type == Type.REVERSE_SORTED) {
            for (int i = 0; i < n / 2; i++) {
                int temporary = a[i];
                a[i] = a[n - 1 - i];
                a[n - 1 - i] = temporary;
            }
        }
        return a;
    }

    public static Point[] points(int n, Type type, long seed) {
        SplittableRandom random = new SplittableRandom(seed);
        Point[] points = new Point[n];
        for (int i = 0; i < n; i++) {
            points[i] = type == Type.DUPLICATE_HEAVY
                    ? new Point(random.nextInt(8), random.nextInt(8))
                    : new Point(random.nextDouble(-1_000_000, 1_000_000),
                                random.nextDouble(-1_000_000, 1_000_000));
        }
        Comparator<Point> xy = Comparator.comparingDouble(Point::x).thenComparingDouble(Point::y);
        if (type == Type.SORTED) Arrays.sort(points, xy);
        if (type == Type.REVERSE_SORTED) Arrays.sort(points, xy.reversed());
        return points;
    }
}
