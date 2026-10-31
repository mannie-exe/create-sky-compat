# Create Sky Compat 0.8.0

Add independent, optional client adaptations for Colorwheel 1.3.0+mc1.21.1:

- Supply Colorwheel terrain/emission metadata on Sable's direct model paths,
  retaining Iris-only emitters and nested material scopes.
- Release unused OIT texture slots when Colorwheel disables OIT on the renderer.
  This corrects sampler exhaustion in the tested Apple instancing path without
  changing shader settings or the supported-OIT path.

Add separate Sable Dynamic Lights 2.0.1 adaptations for Sodium 0.8.13 lookup and
client-world ownership. Moving illumination stays out of authoritative server
light queries. World unload/disconnect clears tracked Create/Sable owners and
SDL's retained client level. The lifecycle repair needs SDL, Lamb and Create;
it does not require Sable, Sodium or Iris. Sodium lookup is independently selected.

Every new adaptation has a startup switch, exact active-version checks and
physical-client gating. No lighting mod or renderer becomes a required dependency.
Existing terrain, gameplay, Questlog, DH and BlueMap adaptations remain intact.
No world generation, physics weights, recipes, saved worlds or LOD caches change.

See [verification](docs/development/verification.md) for native observations and
limits, and [adapter maintenance](docs/development/adapters.md) before updating
patched dependencies. Complete cabin occlusion, all shader/driver combinations
and the older airship-shadow report are not claimed fixed. Modrinth publication
remains deferred.
