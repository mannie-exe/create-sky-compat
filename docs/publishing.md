# Publishing

The GitHub workflow checks/builds on Linux, Windows and macOS. A `v<mod_version>`
tag publishes an initial prerelease only after all jobs succeed and their binary
and source JARs match byte for byte. It includes a complete source ZIP and SHA-256/
SHA-512 manifests. A draft becomes public after asset uploads succeed.

Update `mod_version`, release notes and verification evidence before tagging.
Never overwrite published release files. The release script accepts `--stage-only`
to validate and stage downloaded CI artifacts without calling GitHub releases.
The tag must resolve to the checked-out commit. No automatic Modrinth upload is
configured.

## Modrinth selections

Create a project under **mannie-exe** using these fields:

| Field | Selection |
| --- | --- |
| Project type | Mod |
| Name | Sky Village Neighbors |
| Slug | `sky-village-neighbors` |
| Summary | Keeps Sky Villages floating beside Sky Archipelago islands, with configurable clearance and distance |
| Category | World Generation; Utility as an optional second category |
| Environment | Server-side only, works in singleplayer (`server_only`) |
| License | MIT |
| Source | `https://github.com/mannie-exe/sky-village-neighbors` |
| Issues | `https://github.com/mannie-exe/sky-village-neighbors/issues` |
| Description | README installation, placement and configuration sections |

For the first version, select **Beta**, **Minecraft 1.21.1**, and **NeoForge**.
Upload `sky-village-neighbors-0.1.0-mc1.21.1-neoforge.jar` as the primary file.
Required dependencies must pin these versions, not merely their project IDs:

| Dependency | Version | Modrinth version ID |
| --- | --- | --- |
| Sky Archipelago | 1.3.6 | `RwLz4CJb` |
| Sky Villages | 1.0.6, NeoForge | `3InSHTOp` |

Sky Villages declares Architectury API as its own required dependency. Its
13.0.11 release is the tested runtime. Do not upload dependency JARs or the test
fixture. Multiplayer clients do not need this patch; they still need whatever
their server's underlying mods require.

## Upload process

1. Create the draft project and fill the fields above. Use the README's
   player-facing sections for its description, with working project links.
2. Upload the tested GitHub release binary, select the exact dependency versions,
   and paste the matching release notes. A human-created project icon can be added.
3. Review the draft and submit it for moderation when ready.
4. Download the published primary file and compare its SHA-512 with the release
   manifest before referencing it in a pack.

For future automation, use a scoped repository-secret token and upload the tested
release artifact through the version API. Keep tokens outside tracked files.
The current work prepares metadata and release tooling; it does not submit a
Modrinth project or publish a version.

References: [project creation](https://docs.modrinth.com/api/operations/createproject/),
[version uploads](https://docs.modrinth.com/api/operations/createversion/),
[environment selections](https://modrinth.com/news/article/new-environments/).

## Content disclosures and publication eligibility

Select **AI-generated code** and **AI-generated text** for the contributions
produced in this authoring workflow. There is no runtime AI functionality or
AI-generated artwork in these projects. DH Polygon Fix also attributes its
upstream backport in NOTICE and the README; its derivative-code disclosure should
identify that origin.

The [current Modrinth rules](https://modrinth.com/legal/rules) prohibit public
projects whose contents are primarily or entirely AI-generated. Disclosure alone
does not make such a project eligible. Sky Village Neighbors' current code was
written in this agent workflow and must not be presented as ready for public
Modrinth submission under that rule. DH Polygon Fix combines an attributed
upstream fix with agent-authored integration/tests; eligibility is not established
by its passing tests or GitHub release. Resolve eligibility with Modrinth before
submitting either project; do not misstate authorship or treat superficial edits
as changing it.

These fields and upload instructions are a prepared publishing reference, not a
claim of moderation approval. See also the
[disclosure guide](https://support.modrinth.com/en/articles/16567675-content-disclosures).
