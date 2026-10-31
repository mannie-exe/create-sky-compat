# Create Sky Compat 0.7.0

Add optional Questlog 3.5.0 adapters for readable journal conditions, independent
reward previews, first-progress/completion notifications and authoritative claim
validation. Conditions wrap; measured rows and proportional scrollbars share their
input bounds. Narrow windows show one selected section without shrinking body text.

A Settings bookmark opens grouped display and notification preferences. Header
icons and titles are centered together; custom images have bounded dimensions.
The interface reuses Questlog's replaceable parchment and control textures.
Visiting settings preserves entry position and a completed repeatable quest cycle.

The Mods configuration entry provides registered editors, effective adaptation
status and file locations/reload instructions. Missing startup keys remain visible
with their defaults. Journal preferences are local to each client; server validation
requires installation on the logical server. Incomplete/malformed or duplicate
reward requests are rejected before Questlog grants anything. Legitimate choices
and native inventory overflow keep their existing behavior.

Notifications announce first observed positive progress and completion/reward
readiness. Completion replaces a start; full synchronization/reconnect is silent.
Non-monotonic objectives and pre-completion resets retain the limits documented in
[the integration guide](docs/development/questlog.md). See
[verification](docs/development/verification.md) for tested artifacts and limits.

All integrations remain optional. Terrain, saved generation identity, recipes,
weights and existing world/LOD ownership are unchanged. Modrinth publication remains
deferred. Remove standalone Veil Iris Fix when using the incorporated adapter.
