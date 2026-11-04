# Configuration interface

[Operator configuration](../configuration.md) owns controls, supported targets,
file names, and application timing. This reference owns the editor and UI boundaries.

## Ownership

`SettingsCatalog` assigns each feature to one category and mod group, supplies its
text keys and startup-file owner, and indexes the registered and file-owned tuning.
It contains no optional mod APIs. Its catalogue test covers all features and owned
files. Extend the catalogue and English text when introducing a feature; do not
create another activation switch solely for navigation.

`FeatureSelection` remains the dependency, side, prerequisite, and supported-version
authority. Structured inactive reasons are recorded where it decides selection;
the configuration interface translates them without repeating that decision tree.
The existing inactive debug strings are retained for diagnostics.

`ConfigurationOverview` owns one category/mod document, auxiliary feature details
and startup drafts. `SettingsPanel` measures rows once per rebuild and shares
their bounds across rendering, widget placement, clipping, focus and section
anchors. Native widgets retain their keyboard actions and narration.

`SectionNavigation` stores only measured anchor positions and scroll policy. It
retains an explicitly selected clamped destination until scrolling moves, then
tracks the section at the viewport's top; manually reaching the bottom selects
the final section. Bookmarks preserve section-relative offsets across reflow.
Stable row/control keys restore focus. The sidebar keeps its own scroll; changing
the document section does not rebuild either panel.

Native pointer dispatch assigns focus after invoking a button callback. A callback
that rebuilds the screen can therefore leave focus on a detached panel or footer
button. The overview repairs that focus after dispatch through the stable control
key or corresponding footer action. If an action disappears, focus returns to a
visible content control so Tab does not jump to the document's beginning. Preserve this boundary when changing rebuilds;
a widget's focused flag alone does not establish that the screen routes keys to it.

Details and registered editors retain the document bookmark and useful focus.
Save feedback remains outside the scrolling panels. The file index links to
owning groups; only the ungrouped startup file is edited from the index.
`ClientResourceRevision` is shared with journal screens/toasts and invalidates
measurements after resource/font reloads without changing gameplay state.

The journal bookmark keeps its native skin and parent screen. Both entrances use
`JournalPreferences` and the same localized labels. Its save helpers restore the
previous in-memory value and propagate a reported failure. No second preference
file, network administration API, or live mixin reload is introduced.

## Startup editing

`StartupEdits` retains the last successfully read file and a small delta of intended
changes. The loader's running snapshot stays immutable. A saved next-start value
can therefore differ from both the running selection and the current unsaved draft.

The editor reads only the known feature file, requires a regular non-symlink file,
and bounds input to 128 KiB. It uses `FeatureSettings` for strict validation. An
edit replaces the matching logical property, including Java properties escapes
and continuations, or appends an omitted changed key. Other comments, ordering,
line endings, and omitted defaults remain intact where possible.

Saving compares the baseline before preparing output and again before replacement.
A detected external edit refuses the save and retains the draft. Explicit reload
requires discarding a dirty draft. Candidate output is validated, written to a
same-directory temporary file, and atomically replaces the original. POSIX file
permissions are retained where supported. Unsupported atomic replacement or I/O
failure leaves the original in place and reports failure; there is no in-place
truncation fallback. The optimistic comparisons do not claim a cross-process
compare-and-swap against non-cooperating editors.

Before saving, the UI checks the proposed values with the existing selector and
installed metadata. Missing targets may remain configured on but inactive; an
unsupported active combination is rejected with its existing explanation.
Generation-category edits require confirmation and retain all saved-world identity
checks. Saving never selects a world preset or converts a save.

## Registered and file-owned settings

Village tuning uses NeoForge's registered section editor. Its existing restart
restrictions remain in effect. The section also checks raw minimum/maximum values
before closing, so an individually valid but inverted range cannot be written.
The active village rule is still captured when a local server starts.

Generation, interiors, water, oceans, and BlueMap use known file paths and their
existing loaders. File access does not manufacture missing configs or resolve
foreign classes. A local common file does not configure a remote server. Unsupported
config formats, full JSON editors, and remote administration remain outside this UI.

## Bounded verification

Run `./gradlew --no-daemon check build clientProbeJar`. Writer tests cover actual
file edits, omitted defaults, continuations, stale/invalid files, removal, and
read-only/symlink refusal. Do not replace native input checks with authored-value
or screenshot-coordinate assertions.

The existing client-probe JAR has an opt-in mode:

```text
-Dcreate_sky_compat.probe.configuration=true
```

Use only an isolated instance with `config-review-fixture.json` in its game directory.
It opens the real config screen at the title screen. Numbered commands in
`config-review-control.json` operate native screen clicks, keys, wheel input,
window/GUI sizing, and native numeric edit boxes. `config-review-state.json` records
screen, widget, draft/saved, and active-selection observations; failures remain
visible until the next command. Control files are bounded and contain no executable
scripts. Clear control and state files before relaunching a fixture to avoid
replaying an old command. Neither probe nor fixture files are distributed with the mod.

The same probe accepts native scrollbar `drag` coordinates and a `language` code
to exercise resource reload. Its state includes panel scroll, maximum scroll,
active section and stable focus keys. These are observations of the real screen,
not a substitute layout implementation.
`screenFocusAttached` checks the root input owner after pointer-driven rebuilds.
The resource fixture can enable this same probe alongside
`create_sky_compat.probe.resources`; it then opens settings only on command rather
than competing with automatic world loading.

For journal return checks, also enable
`create_sky_compat.probe.configuration.journal=true` and stage the existing
`tools/fixtures/questlog` definitions/resources in the disposable client. It reuses
the journal fixture. Verify the bookmark, shared values, selected view, and scroll
before and after returning. Use the desktop harness for visual review and actual
pointer checks after confirming availability.

When updating Minecraft or NeoForge, review `ScrollPanel` clipping, pointer bounds,
focus traversal, narration, registered section saving/restart behavior, and raw
`ConfigValue` access. Recheck atomic replacement on supported CI platforms and
minimum client loading without optional APIs. For preference or Questlog updates,
check both entrances and the parent-screen return contract.

For a dedicated-side exclusion check, the existing startup fixture accepts
`--enable create.glass_backfaces --enable fusion.glass_backfaces`. Explicit client
switches must still select no client hooks on a dedicated server.
