# Configuration

`config/sky_village_neighbors-common.toml` is created on first launch. Modpacks
can ship this file in their common overrides. NeoForge's built-in client config
screen edits the same settings; no configuration library is required.

| Setting | Default | Meaning |
| --- | --- | --- |
| `enabled` | `true` | Filter new Sky Villages layouts |
| `minimumGap` | `64` | Minimum horizontal column distance to any island terrain |
| `maximumGap` | `512` | Maximum horizontal column distance to a sampled host island |
| `minimumIslandRadius` | `96` | Minimum native descriptor radius of the host island |

Distances and radius accept integers from 0 to 2048. `maximumGap` must be at least
`minimumGap`; an inverted range fails server startup with a configuration error.
Even with a zero minimum gap, terrain inside the village footprint is rejected.
An island's descriptor radius is not a measurement of its habitable surface area.

Settings are captured when a server starts, including a singleplayer server.
Changes require restarting it. Existing generated structures are not removed or
moved; changing settings affects future starts only. Use a disposable new world
when comparing configurations.

## Interaction with Sky Archipelago

Keep `customStructureRulesEnabled` enabled and retain the SKY category for
`skyvillages:skyvillage`. Disabling custom rules bypasses the upstream validator
this mod extends. Changing the village to a ground category also bypasses this
SKY-specific rule and can change its placement behavior.

Leave the village out of `skyStructureWhitelist`: upstream whitelisting skips
native vertical-clearance validation. It does not disable this mod's horizontal
proximity rule. `DEFAULT` alone does not avoid SKY routing while the ID still
matches a sky category token.

The rule uses Sky Archipelago's resolved terrain settings and island field. It
works with its cluster spacing modes without changing them. It does not make
DYNAMIC spacing consistent across seeds or override biome eligibility.

## Tuning

Increase `maximumGap` to admit more distant floating villages. Decrease it to keep
villages closer, at the cost of more clusters without a village. Decrease
`minimumIslandRadius` to allow small islands to qualify as hosts. Increase
`minimumGap` to leave a wider void around villages.

Frequency remains owned by Sky Villages' structure-set datapack. First measure
retained villages with these placement settings, then adjust its spacing if
needed. A filter cannot guarantee a village quota per island or cluster.
