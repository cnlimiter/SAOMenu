"""Screenshot regression gate for the SAO Menu preview harness.

Compare two real preview runs using the largest absolute RGB-channel difference
at each pixel. Clock time, particles, world rendering and animations are not
deterministic, so choose screenshots and tolerances from representative repeated
runs. A pixel comparison alone does not prove which Screen was actually opened.

Usage:
    python tools/verification/diff_screens.py <baseline_dir> <current_dir> [options]

Exit status is 0 only when at least one screenshot was compared, no expected
screenshots are missing, and every compared screenshot is within tolerance.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

try:
    from PIL import Image, ImageChops
except ImportError:  # pragma: no cover - depends on the local environment
    sys.exit("Pillow is required: pip install Pillow")


def diff_images(baseline: Path, current: Path, channel_delta: int) -> tuple[float, int, int]:
    """Return (changed_pixel_ratio, max_channel_delta, total_pixels)."""
    with Image.open(baseline) as a_img, Image.open(current) as b_img:
        a = a_img.convert("RGB")
        b = b_img.convert("RGB")
        if a.size != b.size:
            return 1.0, 255, a.size[0] * a.size[1]

        # A large change in any channel must not disappear in a luminance average.
        red, green, blue = ImageChops.difference(a, b).split()
        delta = ImageChops.lighter(ImageChops.lighter(red, green), blue)
        histogram = delta.histogram()

    total = sum(histogram)
    # Pixels whose worst channel differs by *more* than the allowed delta.
    changed = sum(histogram[channel_delta + 1:])
    max_delta = next((value for value in range(255, -1, -1) if histogram[value]), 0)
    return (changed / total if total else 0.0), max_delta, total


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("baseline", type=Path, help="directory from the reference run")
    parser.add_argument("current", type=Path, help="directory from the run under test")
    parser.add_argument("--max-changed-ratio", type=float, default=0.02,
                        help="max fraction of pixels allowed to change (default 2%%)")
    parser.add_argument("--max-channel-delta", type=int, default=24,
                        help="per-channel difference ignored as animation noise (default 24)")
    parser.add_argument("--quiet", action="store_true", help="only print failures and the summary")
    parser.add_argument("--only", default="",
                        help="comma-separated substrings; compare only screenshots whose name "
                             "matches (e.g. 'settings'). Choose deterministic surfaces, "
                             "not screenshots dominated by world overlays.")
    args = parser.parse_args()

    if not args.baseline.is_dir():
        sys.exit(f"baseline directory not found: {args.baseline}")
    if not args.current.is_dir():
        sys.exit(f"current directory not found: {args.current}")

    baseline_files = {p.name: p for p in args.baseline.glob("*.png")}
    current_files = {p.name: p for p in args.current.glob("*.png")}

    if args.only:
        needles = [n.strip() for n in args.only.split(",") if n.strip()]
        keep = lambda name: any(n in name for n in needles)
        baseline_files = {k: v for k, v in baseline_files.items() if keep(k)}
        current_files = {k: v for k, v in current_files.items() if keep(k)}

    missing = sorted(baseline_files.keys() - current_files.keys())
    added = sorted(current_files.keys() - baseline_files.keys())
    shared = sorted(baseline_files.keys() & current_files.keys())
    if not shared:
        print("FAILED: no matching screenshots to compare")
        return 1

    failures: list[str] = []
    worst_ratio = 0.0
    worst_name = ""

    print(f"{'screenshot':<26} {'changed':>9} {'maxΔ':>5}  verdict")
    print("-" * 56)
    for name in shared:
        ratio, max_delta, _ = diff_images(baseline_files[name], current_files[name],
                                          args.max_channel_delta)
        ok = ratio <= args.max_changed_ratio
        if ratio > worst_ratio:
            worst_ratio, worst_name = ratio, name
        if not ok:
            failures.append(f"{name}: {ratio:.4%} of pixels changed "
                            f"(limit {args.max_changed_ratio:.2%}), maxΔ={max_delta}")
        if not args.quiet or not ok:
            print(f"{name:<26} {ratio:>8.3%} {max_delta:>5}  {'ok' if ok else 'FAIL'}")

    print("-" * 56)
    print(f"compared {len(shared)} screenshots; worst = {worst_name} @ {worst_ratio:.3%}")

    if missing:
        failures.append("missing screenshots (present in baseline, absent now): " + ", ".join(missing))
    if added:
        print(f"note: {len(added)} new screenshot(s) not in baseline: {', '.join(added)}")

    if failures:
        print("\nFAILED:")
        for line in failures:
            print(f"  - {line}")
        return 1

    print("PASS: all screenshots within tolerance")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
