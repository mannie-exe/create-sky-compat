# Create Sky Compat 0.16.0

Add independent, opt-in client adaptations for two-sided glass:

- `create.glass_backfaces` shows exposed Create glass/window/pane surfaces from
  behind after native connected-texture selection. Create Deco windows using
  those implementations are included. Requires Create 6.0.10.
- `fusion.glass_backfaces` shows exposed Fusion-backed vanilla and Rechiseled
  glass surfaces from behind after texture processing. Requires Fusion 1.3.16;
  Rechiseled remains optional.

Both default off. The existing material-boundary and orientation-boundary switches
remain separate. Matching interfaces and opaque-neighbor occlusion retain native
rules; no global render-state or connected-texture atlas changes are made.

The configuration screen distinguishes the visibility controls. The disposable
resource probe also supports bounded model-query timings and acknowledges old
commands on reopening, preventing an earlier quit command from closing a new run.

See the verification guide for tested renderer combinations, cases and limitations.
