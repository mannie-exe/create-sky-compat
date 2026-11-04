# Rendering adapters

These optional physical-client adapters do not require Sky Archipelago or the
Skyweave modpack. [Configuration](../configuration.md#adaptation-switches) owns
activation and versions. No renderer classes load on dedicated-server paths.

## Glass backfaces

`create.glass_backfaces` and `fusion.glass_backfaces` are separate opt-in client
adaptations. They append reverse-facing copies of final processed quads while
retaining native neighbor-cull groups. Existing window-material and glass-axis
visibility switches remain independent: they decide whether a boundary is exposed;
backfaces decide whether its surface can also be viewed from behind.

The Create hook runs after `CTModel.getQuads` has selected CT tiles. It qualifies
Create's connected glass, window and glass-pane implementations, including their
subclasses. The Fusion hook runs after `BaseBakedModel` processors and qualifies
vanilla glass/panes and Rechiseled's glass pillar implementation.
The selected pack registers 146 Rechiseled glass pillars; other Rechiseled glass
shape classes are not registered and are excluded. It resolves no Rechiseled class
and excludes Create models from this path. Native
Fusion modifier wrappers and renderer dispatch remain intact.

`ReverseVertices` reverses winding and packed normals without separating positions
from UVs, color or light attributes. `GlassBackfaces` retains sprite, tint, shade
and AO flags. It never modifies source lists or vertices; existing coplanar reverse
geometry and its own output are not duplicated. There is no global model wrapper,
render-state change or persistent quad cache.

Sodium 0.8.13 uses the source cull group for neighbor rejection, then computes
normal-based mesh batches from emitted geometry. Both boundaries matter when
updating: changing the source group exposes hidden interfaces, while incorrect
reverse winding/normal can make mesh batching discard the new face. Review native
lighting and translucent sorting alongside model/UV evidence.

Before updating Create or Fusion, inspect the exact output methods, CT-data
ownership, modifier dispatch, render-type handling and vertex format. Update pins,
selection gates, metadata, package checks and native evidence together. Retire a
hook only after a released upstream implementation covers the same cases. These
adapters do not claim arbitrary transparent blocks, fluids, block-entity renderers,
Copycat materials or DH geometry. Native verification scope is recorded separately.

## Window faces

`create.window_culling` selects `CreateWindowCullingMixin` for Create's
`ConnectedGlassBlock` and `WindowBlock`. Native `skipRendering` decisions remain
unchanged for matching blocks. A different window material no longer suppresses
the boundary face through these overrides. Minecraft still performs ordinary
shape occlusion, including opaque neighbors; native models keep their `cullface`
declarations.

Create's `CTModel.createCTData` normally skips hidden directions. Exposing the
boundary through the native visibility decision also lets Create calculate its
existing CT context. Removing model culling declarations alone cannot ensure
that context exists. The adapter does not replace CT textures, force calculations
for every hidden face, change render layers or process other namespaces' models.

Fusion and Rechiseled use independent rendering paths. Rechiseled's connecting
models use Fusion predicates, including state/axis conditions, rather than Create's
CT model. Different-material boundaries already work in the tested variants.

`rechiseled.glass_culling` independently selects `RechiseledGlassCullingMixin`.
The vertical `RechiseledGlassPillarBlock` path hides matching shapes even when the
neighbor has another axis; the horizontal path already preserves that boundary.
The adapter retains a face when the same block has a different axis, making the
two directions consistent. Matching-axis decisions and native slab/stair shape
handling remain unchanged. It uses Minecraft's AXIS property, which is the
property used by the pinned Rechiseled implementation.

This second correction requires Rechiseled, without linking Create or Fusion
APIs. Rechiseled's own required dependencies still apply. No Fusion model or
texture processor is replaced. Its pane-culling option addresses pane geometry,
not these full-window boundaries. When updating Rechiseled, inspect both axis
paths and property identity. Retire this hook when upstream respects differently
oriented glass on both sides while retaining its native shape rules.

When updating Create, inspect both block overrides and `CTModel` against the
released artifact. Retire this adapter when upstream preserves visible mixed
boundaries and their CT context. Recheck horizontal/vertical matching windows,
mixed corners, opaque neighbors, native axis variants, shader rendering and
resource reload. Do not replace a material-specific rule with a blanket policy
for all transparent blocks.

Upstream context: [Create 6.0.10 CT model](https://github.com/Creators-of-Create/Create/blob/mc1.21.1-6.0.10/src/main/java/com/simibubi/create/foundation/block/connected/CTModel.java)
and [Fusion issue 148](https://github.com/SuperMartijn642/Fusion/issues/148).
The latter explains the separation between texture connections and block culling;
it is not an exact upstream report of this Create defect. Native results and
remaining limits belong in [verification](verification.md).

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

## World-text shadows

`rendering.text_display_shadow_pass` and `rendering.nametag_shadow_pass` are
independent, default-on physical-client policies requiring only the pinned Iris
API. They do not depend on terrain, Veil, Sable or Colorwheel. Metadata selection
omits their mixins entirely when unavailable, disabled or on a dedicated server.

`TextDisplayShadowMixin` cancels the concrete `TextDisplayRenderer.renderInner`
overload during [`IrisApi.isRenderingShadowPass()`](https://github.com/IrisShaders/Iris/blob/1.21.1/common/src/api/java/net/irisshaders/iris/api/v0/IrisApi.java). It skips glyphs and background
together without also injecting into its generic bridge. Item/block display
renderers are untouched.

`NametagShadowMixin` guards the base `EntityRenderer.renderNameTag` method. Its
priority is 1100, before Iris's priority-1000 nametag material setup. Cancelling
after that setup would skip Iris's RETURN restoration and leak material state.
The transformed-mixin test includes the exact Iris mixin and checks this ordering.
Ordinary nametag rendering retains Iris's setup/restoration, and entity-body
rendering is not cancelled. Custom renderers bypassing the base method remain
outside coverage.

These policies affect shader shadow-map geometry, not native full-bright display
lighting, glyph drop shadows, entity blob shadows or post-processing. They do not
classify entities by server tags or introduce a synchronization protocol.

When updating Minecraft or Iris, review concrete/bridge descriptors, base-renderer
overrides, Iris material setup/reset hooks and priorities, and the public shadow
API. Recheck transformed injection count/order, normal/shadow behavior and both
physical sides. Retire or revise a hook only after confirming equivalent behavior
in the selected upstream version. The optional native scene is owned by
`tools/fixtures/rendering`; dated results belong in [verification](verification.md).

## World-text lighting

`rendering.text_display_lighting` and `rendering.nametag_lighting` independently
route selected glyphs and backgrounds through Iris's native fallback programs.
Both default on and require only the pinned Iris client. They are separate from
cast-shadow exclusions. Ordinary brightness, depth, alpha, billboarding, text
contents and native visibility remain with Minecraft.

Full-bright display data only overrides packed light. The selected
Complementary/Euphoria entity program additionally applies directional lighting
and fog and caps block-light input. Nametags retain subsequent shader processing
even in their special material branch. The fallback route avoids that entity
program; later exposure, grading and compositing can still affect displayed color.
It does not promise exact RGB output or identical results with every shader.

The concrete text-display renderer and base `EntityRenderer.renderNameTag` receive
a buffer-source adapter before emitting vertices. `WorldTextRenderType` keeps
those batches distinct from ordinary font rendering, forwards native render
states and Iris transparency metadata, and marks setup/draw rather than just the
vertex-emission call. This matters because Iris defers and groups entity buffers.
It deliberately does not subclass Iris's `OuterWrappedRenderType`, which
`wrapExactlyOnce` would unwrap and thereby erase the classification.

`WorldTextScope` restores nested state through exceptions. `WorldTextShaderMixin`
selects a fallback only for the current pipeline's `TEXT`, `TEXT_INTENSITY` and
`TEXT_BG` lookup during that scope. Shared shader-map slots are never replaced.
Signs/block-entity font programs, hand text, shadow programs, entity bodies,
item/block displays and ordinary world/HUD text retain their owners. Custom
nametag renderers bypassing the base method are outside coverage.

Fallbacks are created on the render thread through Iris's own factory, which
registers them with `loadedShaders` and owns their destruction. Each pipeline
caches at most three programs and 256 recent font-type wrappers. Destruction clears
Compat's references without double-closing Iris resources. Shaders off and
non-Iris pipelines return the original buffer source.

Before updating Minecraft or Iris, recheck concrete/bridge descriptors, font
render types, base-renderer overrides, deferred batch setup/draw and merge mode,
transparency/depth state, shader keys, fallback output attachments and
`loadedShaders` cleanup. Test reload, shader switches and world transitions;
never infer lifecycle safety from a successful first frame. Shader updates also
need native glyph/background and unaffected-geometry comparisons. Dated results
and remaining limits belong in [verification](verification.md).

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

## Colorwheel

Colorwheel 1.3.0+mc1.21.1 supplies shader-aware Flywheel backends. The optional
`rendering.colorwheel_materials` bridge requires the existing construct-material
adaptation and is selected only on physical clients. Its compile artifact is
Modrinth `8rq5KP4U`, SHA-256
`3c7c1f91b1add47053f40a58375de492b87ce83309be378527f100cb842c07c0`.

Colorwheel wraps NeoForge `ModelBlockRenderer.tesselateBlock`, but Sable 2.0.6's
platform calls `tesselateWithoutAO` directly. The existing Compat scope reaches
that path; plain Iris `beginBlock` alone does not set Colorwheel's terrain and
emission fields. Colorwheel stores that emission in the fourth byte of Iris's
mid-block vertex attribute. `ColorwheelMaterialMixin` selects a writer for
Compat's own scope, using `clrwlBeginBlock`/`clrwlEndBlock` where supported and
retaining the Iris protocol on plain mesh emitters. Nested scopes restore their
parent writer and material through failures. No upstream mixin is cancelled.

`rendering.colorwheel_samplers` is independent of Sable and the material bridge.
Colorwheel reserves slots 0–6 for base rendering, 7–8 for OIT depth/noise and 9+
for its coefficient textures. Its Apple renderer guard disables OIT drawing, but
`ClrwlProgram.postLink` still reserves and registers all those OIT slots. With
Complementary/Euphoria, the native embedded translucent program then exhausts
Iris's sampler allocation at `shadowtex0`.

`ColorwheelSamplerMixin` retains all base slots and omits OIT reservations and
external registrations only when Colorwheel's own `SUPPORTS_OIT` is false.
Unsupported OIT already falls back to ordinary translucent drawing upstream; the
adaptation enables no hardware feature and changes no shader setting. On supported
renderers the original sets and registrations pass through unchanged. Review
Colorwheel's slot layout, OIT guard and binding order, and Iris's reserved/external
sampler contract before updating. Retire when upstream skips unsupported OIT's
reservations itself. This is not a general sampler-limit bypass.

This bridge does not take ownership of ordinary world tessellation, mesh storage,
Flywheel engines, Sable transforms or shader programs. It does not claim to repair
all nested upstream annotation paths. The other rendering adaptations remain
independent; disabling construct material IDs also disables this dependent bridge.
Missing/disabled Colorwheel loads no Colorwheel API through Compat.

Review Colorwheel's buffer API and mid-block encoding, its tessellation wrappers,
Sable's direct flat/AO calls and Flywheel mesh emitters together before upgrading.
Remove this bridge if Sable or Colorwheel covers direct construct tessellation with
complete material metadata; do not retain duplicate annotation owners. Recheck
Sable light-storage/embedded-environment APIs and supported shader backend/GL
requirements independently. Version metadata alone does not establish these.

Colorwheel's [backend documentation](https://djefrey.github.io/colorwheel/whatis/colorwheel/)
explains shader integration. Complementary 5.7+ and Euphoria 1.7+ include native
support; Patcher is unnecessary for the tested newer selection. Apple OpenGL 4.1
uses the supported instancing path, with indirect rendering and OIT unavailable.
This is not a promise of a measured performance improvement.

## Single-block render passes

`SingleBlockPassMixin` flushes Sable's single-block queue at the end of each
`VanillaSubLevelRenderDispatcher.renderSectionLayer` call while Iris shaders are
active. Sable 2.0.6 otherwise flushes before entities, after opaque terrain. A
translucent layer queued later is drawn in a subsequent opaque pass, where Iris
cannot composite it correctly. Ordinary glass and glass Copycat single-block
constructs disappeared in the native 0.6.0 fixture while stationary controls
remained visible. The material-ID switch did not resolve this discrepancy.

The adaptation reuses Sable's renderer and queue clearing. It changes no mesh,
physics, or material classification, and leaves shader-off scheduling unchanged.
It is independent of the model-data and material-ID switches. Review Sable's
queue ownership/call sites and Iris opaque, translucent, and shadow-pass order
when updating either renderer. Retire when upstream draws single-block geometry
within its matching pass; do not retain a second flush after that correction.

## Verification boundary and unresolved shadows

`./gradlew check build` transforms the released Veil and Aeronautics targets,
NeoForge ModelBlockRenderer, and Sable's platform class. Controlled execution
checks framebuffer restoration and shadow-pass cancellation; material-scope tests
exercise recursion and exceptions. These tests require no window or GL context.
They do not prove native shader output. [Verification](verification.md) records
the tested artifact and current startup checks.

Native checks cover the released 0.6.0 and the 0.6.1 correction on macOS arm64,
with the pack's Complementary Unbound/Euphoria shader selection. Ponder,
shader toggling/reload, valid heated balloons, materials, and single-block glass
controls are recorded in [verification](verification.md). These are bounded
observations, not validation of every driver, shader pack, or special block model.

Airship shadows flickering or appearing separated remain unresolved. There is no
source evidence here establishing whether precision/filtering, culling, or moving
construct transforms cause that symptom. A future authorized comparison should
use ordinary blocks versus special models, balloon inflated/deflated, stationary
versus moving ship, and camera movement. Compare shader settings only where they
distinguish causes. Neither the material-ID nor balloon guard is claimed to fix
geometric shadow continuity. Sable's separate default-off skylight-shadow setting
is not Iris's shadow enable switch and is not changed.
