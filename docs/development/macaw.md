# Bridge lanterns

`macaw.bridge_lantern_shape` matches Macaw’s Bridges stair-lantern selection to a short-chain hanging resource model. It defaults off and requires Macaw’s Bridges 3.1.2. Enable it on client and server with the matching resource pack. Flat lanterns keep their native shape and model. Collision remains empty.

## Ownership

The resource pack owns the visible model. Compat owns the outline and selection correction; it includes no Macaw or Better Lanterns assets. `LanternLayout` records the approved model envelope in pixels. The canonical east-facing bounds rotate around the block center using the native blockstate rotations. Update this layout and the pack recipe together.

The lantern body extends below the owning cell and slightly past one horizontal face. Changing `getShape` alone leaves rays that never visit the owner unable to select it. A native shape-only check confirmed that a direct outline intersection can coexist with a world-ray miss.

`LanternRayMixin` supplements only OUTLINE queries in real levels. It traverses the requested segment up to the existing native hit, checking loaded neighboring owners whose stair outlines can enter each visited cell. A candidate must intersect inside that cell and precede the native obstruction. The returned hit identifies the real lantern owner. It does not extend reach, load chunks, replace fluid/collision queries or change physical collision. Ordinary block getters and unrelated blocks retain their native results.

The outline is a bounding envelope, like the upstream lantern outline, rather than a voxel copy of every chain opening. Bridge rail geometry can legitimately obstruct low rays. Do not make the lantern selectable through its support to compensate.

## Update checks

The compile-only artifact is Modrinth version `aQ7rY7ng`, SHA-256 `070b817d3282760d9789b22ce13779613c8b558e02012fd5884d343175d88fd9`. Review `Bridge_Lantern.getShape`, inherited collision, `LIGHTSTATE` updates, facing rotations and placement/removal behavior before changing its pin. Review Minecraft's `BlockGetter.clip` traversal and `ClipContext` shape modes when changing Minecraft or renderer integrations.

Keep the feature opt-in unless the matching model is independently guaranteed. Retire the shape and selection hooks together if upstream adopts the same placement. A new native model or changed support behavior requires a fresh comparison; a matching filename is insufficient.

## Verification

The isolated resource harness owns `macaw_lantern_review.py` and its marked saved-world fixture. It checks native outline rays, collision queries, segment endpoints, solid obstruction, nearest selection and removal across all four facings. `build/verification/macaw-shape-only.json` records the original selection failure; `macaw-selection.json` records corrected queries. These temporary build reports are summarized in the maintained verification guide before cleanup.

Dedicated startup scenarios `macaw` and `macaw-disabled` isolate the optional target. Also run standalone, unsupported-disabled and full-pack checks. The client resource probe's `ray` action reports actual world hits, direct shape intersections and collision without changing the personal installation.
