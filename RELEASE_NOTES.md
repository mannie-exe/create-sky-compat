# Create Sky Compat 0.5.2

Repair Analog Audio 0.1.5-hotfix.1 radio movement registration with Create 6.0.10.
The adapter resolves the erased method signature through Create's public registry
interface and retains Analog Audio's existing playback handler. It activates only
when both exact versions are installed; either mod remains optional.

This release changes no terrain, generation identity, recipes, block weights or
Sable audio handling. Existing worlds need no regeneration. Registration and
server callback checks do not establish audible client playback or contraption
GUI support.

Verification: Java/build/package checks and isolated dedicated-server checks for
the minimum installation, Create only, Analog Audio only, the affected pair and
the full pack. The previous release fails the pair's registration check. See
[verification](docs/development/verification.md) for artifacts and limits.
