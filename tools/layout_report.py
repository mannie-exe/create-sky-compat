#!/usr/bin/env python3
"""Compare native terrain probe reports and draw equal-scale descriptor layouts (requires matplotlib)."""
import argparse
import hashlib
import json
import math
import statistics
from pathlib import Path


def summarize(report):
    rows = []
    for run in report['runs']:
        if not run['adapted']:
            continue
        anchors = [p for p in run['descriptors'] if p['family'] == 'ANCHOR_PLATEAU']
        distances, aligned = [], 0
        for a in anchors:
            if math.hypot(a['x'], a['z']) > 10000:
                continue
            b = min((b for b in anchors if b is not a), key=lambda b: math.hypot(b['x'] - a['x'], b['z'] - a['z']))
            distances.append(math.hypot(b['x'] - a['x'], b['z'] - a['z']))
            angle = math.degrees(math.atan2(b['z'] - a['z'], b['x'] - a['x'])) % 90
            aligned += min(angle, 90 - angle) <= 10
        rows.append(dict(seed=run['seed'], anchors=len(distances), axisAligned=aligned,
                         nearestMean=statistics.mean(distances), nearestMax=max(distances),
                         minimumSupportGap=min(c['minimumSupportGap'] for c in run['clusterClearances']),
                         sampledShoreGaps=[c['sampledShoreGap'] for c in run['clusterFootprints']]))
    return rows


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('baseline', type=Path)
    parser.add_argument('candidate', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--seed', type=int, default=42)
    args = parser.parse_args()
    import matplotlib
    matplotlib.use('Agg')
    import matplotlib.pyplot as plt
    from matplotlib.patches import Circle
    fig, axes = plt.subplots(1, 2, figsize=(12, 6), layout='constrained')
    summary = {}
    for ax, label, path in zip(axes, ['Baseline', 'Candidate'], [args.baseline, args.candidate]):
        raw = path.read_bytes()
        report = json.loads(raw)
        summary[label.lower()] = dict(sourceSha256=hashlib.sha256(raw).hexdigest(),
                                      profile=report['profile'], runs=summarize(report))
        run = next(r for r in report['runs'] if r['adapted'] and r['seed'] == args.seed)
        for p in run['descriptors']:
            if abs(p['x']) > 10500 or abs(p['z']) > 10500:
                continue
            anchor = p['family'] == 'ANCHOR_PLATEAU'
            ax.add_patch(Circle((p['x'], p['z']), p['radius'], color='#17695c' if anchor else '#87aaa1', linewidth=0))
        ax.set(xlim=(-10000, 10000), ylim=(10000, -10000), aspect='equal', xlabel='X (blocks)', ylabel='Z (blocks)',
               title=f"{label}: ±{report['profile']['jitter']} jitter")
        ax.set_xticks([-10000, -5000, 0, 5000, 10000]); ax.set_yticks([-10000, -5000, 0, 5000, 10000])
        ax.tick_params(labelsize=8)
    fig.suptitle(f'Seed {args.seed} · native biome admission · 2400-block candidate grid\n'
                 'Circles show descriptor radii, not generated shorelines; dark = main island', fontsize=11)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    fig.savefig(args.output, metadata={'Date': None} if args.output.suffix == '.svg' else None)
    print(json.dumps(summary, indent=2))


if __name__ == '__main__':
    main()
