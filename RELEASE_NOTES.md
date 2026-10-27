# Create Sky Compat 0.5.0

Expand the existing Create Sky Islands preset with less regular cluster placement,
island-relative interiors, local water and optional BlueMap presentation.
Minecraft 1.21.1, NeoForge 21.1.264+ within 21.1, and Sky Archipelago 1.3.6 remain
required. Other integrations remain optional.

## Terrain

- Increase center jitter to 750 blocks within the retained 2400-block cells.
  Deterministic clearance checks consider complete neighboring cluster supports,
  including companions crossing cell boundaries; minimum support separation is 350 blocks.
- Retain 250–400-block target main-island heights, fitted vertical placement,
  building headroom and the continuous underside taper.
- Use OVERLAP with explicit terrain stages. Add selectable noise interiors and
  hollows while retaining native classic carvers and island-relative geology.
- Enable large iron/copper veins, local basins and controlled water outlets.
  Protect planned source water from subsequent carving and retain native fluid flow.
- Preserve biome-specific surface materials, including mangrove mud, and adapt
  applicable cave-biome and ancient-city placement to the island frame.
- Add experimental basin, coastal and perimeter ocean forms. They remain disabled
  by default; long-term fluid cost and ecological balance need further playtesting.

Geometry, interiors, water and oceans have separate validated JSON configurations.
Install matching generation settings on client and server. Compared with released
0.4.0, this changes saved generation identity and requires a fresh world. Do not
remove a world's identity marker to force an upgrade. Worlds already created with
the qualified `islands_v1-prototype-10` candidate and identical settings remain compatible.

## Optional BlueMap adapter

Dedicated servers with BlueMap 5.7 can opt selected Sky Archipelago maps into
extended detailed rendering, bounded tile loading and empty-edge clipping.
Selection uses the actual world height bounds and excludes foreign generators.
No BlueMap installation is needed on clients. The adapter does not change stored
tiles or generation; coarse layers still cannot fully represent stacked terrain
or island undersides.

## Verification and limits

The terrain candidate passed minimum/full-installation native generation and reopen
checks, island-relative resource/material surveys, fluid fixtures, and a bounded
dedicated DH pregen. Isolated client review covered island bodies, cave interiors,
water forms and biome transitions. BlueMap checks covered absence, selected and
foreign worlds, reload, shutdown, browser detach and distant camera movement.
The release build passes 40 JUnit and five release-tool tests; package and upstream
artifact checks pass. See `docs/development/verification.md` for exact scope.

These checks do not establish resource balance, complete rare-structure coverage,
natural aquatic populations or long-term survival progression. No island-priority
DH or DH SeedGen adapter is included.
