# Rendering adapters

These optional physical-client adapters do not require Sky Archipelago or the
Create Sky modpack. [Configuration](../configuration.md#adaptation-switches) owns
activation and versions. No renderer classes load on dedicated-server paths.

## Framebuffer state

`ShaderWrapperMixin` incorporates Veil Iris Fix 0.1.0. It wraps the complete Veil
shader apply/clear methods, retaining the shipped Veil Iris injection inside the
wrapper. It saves separate read/draw targets, uses Iris's active world target only
when appropriate, and restores prior bindings for offscreen/shadow contexts and
on normal or exceptional cleanup. Repeated apply calls retain the first target.
The exact Veil 4.3.2 nested artifact comes from Sable 2.0.6; Iris is pinned in
`build.gradle`. The standalone patch must not activate alongside this adapter.

Upstream reference: [Veil PR #213](https://github.com/FoundryMC/Veil/pull/213).
Retire when the selected released Veil artifact contains complete framebuffer and
Iris pass handling. Review both shipped Veil injections and Iris `bindDefault`,
`shouldOverrideShaders`, and shadow-pass state before updating either dependency.

## Balloon overlay

`BalloonShadowMixin` skips Aeronautics' hot-air effect callback when Iris reports
its shadow pass. Aeronautics 1.3.2 otherwise creates an overlay framebuffer and
runs screen-space post-processing during that pass. The ordinary render-stage
callback is preserved, as is actual ship geometry in the shadow map.

Upstream reference: [Simulated PR #1298](https://github.com/Creators-of-Aeronautics/Simulated-Project/pull/1298).
This is separate from framebuffer restoration. The PR's End Sea and physics-staff
guards are not included: the reported symptom and inspected balloon callback
justify this bounded adaptation, while those additional effects lack equivalent
pack-specific evidence. Review their own callbacks if symptoms appear.
Retire when released Aeronautics excludes the hot-air composite from shadow passes.

## Construct materials and single-block models

`ConstructMaterialMixin` brackets both NeoForge AO and non-AO tessellation paths
only for Sable construct views and an Iris block-sensitive buffer with a material
map. Ordinary world meshes and Sodium's own annotation path retain their owners.
`BlockMaterialScope` balances begin/end through exceptions and restores the outer
annotation during recursive rendering on the same buffer. Shader block IDs and
emission describe existing blocks; this adapter changes no physics or weights.

`SingleBlockModelDataMixin` independently makes render-layer selection query the
same level model data used by Sable's tessellation path. It does not require Iris.

Upstream reference: [Sable PR #1417](https://github.com/ryanhcode/sable/pull/1417).
Review Sable's reacharound dispatcher, construct view wrappers, Iris's block-ID
producer/consumer, and NeoForge tessellation overloads before updating any target.
An upstream fix may add its own begin/end calls; remove this adapter before that
would double-annotate geometry. Retire the model-data hook independently when
Sable's released layer-selection implementation uses the matching data.

The earlier Sable matrix-refresh and directional-shading corrections are already
present in 2.0.6 and are not duplicated here. This material correction does not
establish correct Ponder previews, Copycat normals, or shadow-map projection.

## Verification boundary and unresolved shadows

`./gradlew check build` transforms the released Veil and Aeronautics targets,
NeoForge ModelBlockRenderer, and Sable's platform class. Controlled execution
checks framebuffer restoration and shadow-pass cancellation; material-scope tests
exercise recursion and exceptions. These tests require no window or GL context.
They do not prove native shader output. [Verification](verification.md) records
the tested artifact and current startup checks.

Native client startup, shader-on/off Ponder controls, balloon overlays, construct
materials, reload, and flickering-shadow comparisons remain pending by operator
instruction. Do not take desktop focus or launch an interactive client for this
release workflow. Historical Veil Iris Fix native evidence remains historical;
it is not a new validation of the combined artifact.

Airship shadows flickering or appearing separated remain unresolved. There is no
source evidence here establishing whether precision/filtering, culling, or moving
construct transforms cause that symptom. A future authorized comparison should
use ordinary blocks versus special models, balloon inflated/deflated, stationary
versus moving ship, and camera movement. Compare shader settings only where they
distinguish causes. Neither the material-ID nor balloon guard is claimed to fix
geometric shadow continuity. Sable's separate default-off skylight-shadow setting
is not Iris's shadow enable switch and is not changed.
