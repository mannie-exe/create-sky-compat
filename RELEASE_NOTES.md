# Create Sky Compat 0.6.0

Install Compat with NeoForge alone and enable only the adaptations you need.
Sky Archipelago and every integrated content/rendering mod are optional. The
startup feature file controls each independent adaptation before target classes,
mixins, callbacks, and integration data load. Disabled adapters do not reject
installed target versions. Terrain choices retain saved-world identity checks.

This release incorporates Veil Iris Fix and adds optional Aeronautics balloon
shadow-pass, Sable shader material-ID, and single-block model-data corrections.
Remove the standalone Veil Iris Fix when using Compat's framebuffer adapter.
The framebuffer component retains its LGPL-3.0-only license and attribution.

The default-off DH cache switch now lives in the startup feature file. Existing
users of the old per-world cache option must explicitly enable the new switch;
disabling it installs no cache hooks, even for a save with an identity marker.
No cache data is deleted or migrated.

Background build, artifact, transformation/state, and dedicated-server checks
qualify this release. Interactive client startup, shaders, Ponder, balloon visuals,
materials, reload, and flickering shadows remain pending. The separate flickering
or gapped airship-shadow symptom is unresolved. See the verification guide for
exact commands, artifacts, and limits; these changes are not visually verified.
