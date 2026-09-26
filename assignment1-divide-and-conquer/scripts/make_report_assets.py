
"""Regenerate aggregate CSV, Markdown tables, plots and exact saved-output images."""
from pathlib import Path
from collections import defaultdict
import csv
import math
import statistics
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
RESULTS = ROOT / "results"
PLOTS = ROOT / "docs/plots"
SHOTS = ROOT / "docs/screenshots"
ALGORITHMS = ["MergeSort", "QuickSort", "Select", "ClosestPair"]
TYPES = ["RANDOM", "SORTED", "REVERSE_SORTED", "DUPLICATE_HEAVY"]
SIZES = [100, 1000, 10000, 100000, 300000]
COLORS = ["#2457A7", "#C44E35", "#168270", "#8154A0"]
groups = defaultdict(list)
with (RESULTS / "results.csv").open() as stream:
    for row in csv.DictReader(stream):
        groups[row["algorithm"], row["input_type"], int(row["n"])].append(row)
summary = {}
for key, rows in groups.items():
    assert len(rows) == 7, (key, len(rows))
    times = [int(r["elapsed_ns"]) / 1e6 for r in rows]
    quartiles = statistics.quantiles(times, n=4, method="inclusive")
    summary[key] = dict(
        median_ms=statistics.median(times), q1_ms=quartiles[0], q3_ms=quartiles[2],
        min_ms=min(times), max_ms=max(times),
        max_depth=max(int(r["max_recursion_depth"]) for r in rows),
        median_comparisons=statistics.median(int(r["comparisons"]) for r in rows),
        median_distances=statistics.median(int(r["distance_evaluations"]) for r in rows),
        median_calls=statistics.median(int(r["recursive_calls"]) for r in rows),
    )
with (RESULTS / "summary.csv").open("w", newline="") as stream:
    fields = ["algorithm", "input_type", "n", "trials"] + list(next(iter(summary.values())))
    writer = csv.DictWriter(stream, fieldnames=fields)
    writer.writeheader()
    for (algorithm, kind, n), values in sorted(summary.items()):
        writer.writerow(dict(algorithm=algorithm, input_type=kind, n=n, trials=7, **values))

plt.rcParams.update({"font.family": "DejaVu Sans", "font.size": 10,
                     "axes.spines.top": False, "axes.spines.right": False,
                     "axes.titleweight": "bold", "figure.facecolor": "white"})

def panels(filename, depth=False):
    fig, axes = plt.subplots(2, 2, figsize=(12.4, 8.4), constrained_layout=True)
    for ax, kind in zip(axes.flat, TYPES):
        for algorithm, color in zip(ALGORITHMS, COLORS):
            data = [summary[algorithm, kind, n] for n in SIZES]
            values = [d["max_depth" if depth else "median_ms"] for d in data]
            ax.plot(SIZES, values, "o-", label=algorithm, color=color, linewidth=2, markersize=4)
            if not depth:
                ax.fill_between(SIZES, [d["q1_ms"] for d in data], [d["q3_ms"] for d in data], color=color, alpha=.13)
        ax.set_xscale("log")
        if not depth: ax.set_yscale("log")
        ax.set_xlabel("Input size n")
        ax.set_ylabel("Maximum active recursion depth" if depth else "Median execution time (ms)")
        ax.set_title(kind.replace("_", " ").title())
        ax.grid(True, alpha=.2)
        if depth: ax.set_ylim(0, 21)
    axes[0, 0].legend(fontsize=9, loc="upper left")
    fig.suptitle("Recursion depth vs. input size" if depth else "Execution time vs. input size", fontsize=18, weight="bold")
    fig.supxlabel("Maximum across 7 trials; root depth = 1" if depth else "7 trials per case; bands show interquartile range; instrumented Java 17", fontsize=10)
    fig.savefig(PLOTS / filename, dpi=170)
    plt.close(fig)

PLOTS.mkdir(parents=True, exist_ok=True)
SHOTS.mkdir(parents=True, exist_ok=True)
panels("time_vs_n.png")
panels("depth_vs_n.png", depth=True)

fig, axes = plt.subplots(2, 2, figsize=(12.4, 8.4), constrained_layout=True)
for ax, algorithm in zip(axes.flat, ALGORITHMS):
    for kind, color in zip(TYPES, COLORS):
        values = [summary[algorithm, kind, n]["median_comparisons"] /
                  (n if algorithm == "Select" else n * math.log2(n)) for n in SIZES]
        ax.plot(SIZES, values, "o-", color=color, label=kind.replace("_", " ").title())
    ax.set_xscale("log")
    ax.set_xlabel("Input size n")
    ax.set_ylabel("Comparisons / n" if algorithm == "Select" else "Comparisons / (n log2 n)")
    ax.set_title(algorithm)
    ax.grid(True, alpha=.2)
axes[0, 0].legend(fontsize=9)
fig.suptitle("Operation counts and theoretical growth", fontsize=18, weight="bold")
fig.supxlabel("Counters have algorithm-specific meanings; use trends within each panel", fontsize=10)
fig.savefig(PLOTS / "normalized_operations.png", dpi=170)
plt.close(fig)

fig, ax = plt.subplots(figsize=(9, 5.4), constrained_layout=True)
for algorithm, color in [("ClosestPair", COLORS[3]), ("BruteForce", "#5B6573")]:
    sizes = [100, 1000, 2000]
    ax.plot(sizes, [summary[algorithm, "RANDOM", n]["median_ms"] for n in sizes],
            "o-", color=color, label=algorithm, linewidth=2)
ax.set_xscale("log")
ax.set_yscale("log")
ax.set_xlabel("Number of points n (random input)")
ax.set_ylabel("Median execution time (ms)")
ax.set_title("Closest pair: divide-and-conquer vs. brute force", pad=14)
ax.grid(True, alpha=.2)
ax.legend()
fig.savefig(PLOTS / "closest_vs_bruteforce.png", dpi=170)
plt.close(fig)

def markdown_table(headers, rows):
    return "\n".join(["| " + " | ".join(map(str, headers)) + " |",
                       "| " + " | ".join(["---"] * len(headers)) + " |"] +
                      ["| " + " | ".join(map(str, row)) + " |" for row in rows])

sections = ["# Complete experimental tables", "", "Generated from `results/results.csv`. Times are medians of seven trials in milliseconds; depth is the maximum across those trials.", ""]
for kind in TYPES:
    sections += ["## " + kind.replace("_", " ").title(), "",
                 markdown_table(["n"] + ALGORITHMS,
                     [[f"{n:,}"] + [f"{summary[a, kind, n]['median_ms']:.4f}" for a in ALGORITHMS] for n in SIZES]),
                 "", "Maximum recursion depth:", "",
                 markdown_table(["n"] + ALGORITHMS,
                     [[f"{n:,}"] + [summary[a, kind, n]["max_depth"] for a in ALGORITHMS] for n in SIZES]), ""]
(ROOT / "docs/EXPERIMENT_TABLES.md").write_text("\n".join(sections), encoding="utf-8")

def font(size, bold=False):
    name = "DejaVuSansMono-Bold.ttf" if bold else "DejaVuSansMono.ttf"
    candidates = [Path("/usr/share/fonts/truetype/dejavu") / name, Path(name)]
    for candidate in candidates:
        try: return ImageFont.truetype(str(candidate), size)
        except OSError: pass
    return ImageFont.load_default()

def output_capture(text, title, source, filename):
    lines = text.rstrip().splitlines()
    f = font(20)
    width = max(1080, int(max(f.getlength(line) for line in lines)) + 70)
    image = Image.new("RGB", (width, 146 + 30 * len(lines)), "#101B2D")
    draw = ImageDraw.Draw(image)
    draw.text((32, 23), title, font=font(25, True), fill="#FFFFFF")
    draw.text((32, 66), "Saved stdout: " + source, font=font(15), fill="#9AB2D2")
    draw.line((32, 98, width - 32, 98), fill="#304563", width=2)
    for i, line in enumerate(lines):
        color = "#93E2B6" if line.startswith(("PASS", "ALL TESTS")) else "#E4EDF9"
        draw.text((32, 117 + 30 * i), line, font=f, fill=color)
    image.save(SHOTS / filename)

output_capture((RESULTS / "demo.txt").read_text(), "Program output", "results/demo.txt", "program_output.png")
test_output = (RESULTS / "tests.txt").read_text()
assert "ALL TESTS PASSED:" in test_output, "Do not render an incomplete test run"
output_capture(test_output, "Correctness tests", "results/tests.txt", "test_results.png")
raw_lines = (RESULTS / "experiment.txt").read_text().splitlines()
excerpt = raw_lines[:4] + [line for line in raw_lines if line.startswith(tuple(ALGORITHMS)) and "RANDOM" in line and "300000" in line] + raw_lines[-3:]
output_capture("\n".join(excerpt), "Experiment output - selected lines", "results/experiment.txt", "experiment_results.png")
print(f"Generated {len(summary)} summary rows, 4 plots, 3 saved-output images and full tables.")
