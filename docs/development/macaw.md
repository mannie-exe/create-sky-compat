# Bridge lanterns

`macaw.bridge_lantern_shape` matches the standing flat-bridge and short-chain
stair-lantern selection outlines to the pack's resource models. It defaults on and
requires Macaw’s Bridges 3.1.2. Use matching resources and enable the feature on
client and server. Both variants retain their native empty physical collision.

## Ownership

The resource pack owns the visible model. Skyweave: Compatibility Patches owns the outline and selection correction; it includes no Macaw or Better Lanterns assets. `LanternLayout` records the approved model envelope in pixels. The canonical east-facing bounds rotate around the block center using the native blockstate rotations. Update this layout and the pack recipe together.

Both lantern models extend below the owning cell and slightly past one horizontal face. Changing `getShape` alone leaves rays that never visit the owner unable to select it. A native shape-only check confirmed that a direct outline intersection can coexist with a world-ray miss.

`LanternRayMixin` supplements only OUTLINE queries in real levels. It traverses the requested segment up to the existing native hit, checking loaded neighboring owners whose lantern outlines can enter each visited cell. A candidate must intersect inside that cell and precede the native obstruction. The returned hit identifies the real lantern owner. It does not extend reach, load chunks, replace fluid/collision queries or change physical collision. Ordinary block getters and unrelated blocks retain their native results.

Flat lanterns occupy x=5.5..10.5, y=-1..7 and z=-1..4 pixels before the native
quarter-turn. The old outline omitted the one-pixel bottom/front overhang.
The stair envelope remains unchanged. The native lantern has only `bridge` and
`stair` light states and four facings; regular, rope and stair support families
choose those states. Materials and bridge connection variants do not introduce
additional lantern geometry.

The outline is a bounding envelope, like the upstream lantern outline, rather than a voxel copy of every chain opening. Bridge rail geometry can legitimately obstruct low rays. Do not make the lantern selectable through its support to compensate.

## Update checks

The compile-only artifact is Modrinth version `aQ7rY7ng`, SHA-256 `070b817d3282760d9789b22ce13779613c8b558e02012fd5884d343175d88fd9`. Review `Bridge_Lantern.getShape`, inherited collision, `LIGHTSTATE` updates, facing rotations and placement/removal behavior before changing its pin. Review Minecraft's `BlockGetter.clip` traversal and `ClipContext` shape modes when changing Minecraft or renderer integrations.

The feature defaults on and expects the matching model resources. Consumers without those resources must disable it. Retire the shape and selection hooks together if upstream adopts the same placement. A new native model or changed support behavior requires a fresh comparison; a matching filename is insufficient.

## Verification

The isolated resource harness owns `macaw_lantern_review.py` and its marked saved-world fixture. It checks native outline rays, collision queries, segment endpoints, solid obstruction, nearest selection and removal across all four facings. `build/verification/macaw-shape-only.json` records the original selection failure; `macaw-selection.json` records corrected queries. These temporary build reports are summarized in the maintained verification guide before cleanup.

Dedicated startup scenarios `macaw` and `macaw-disabled` isolate the optional target. Also run standalone, unsupported-disabled and full-pack checks. The client resource probe's `ray` action reports actual world hits, direct shape intersections and collision without changing the personal installation.
