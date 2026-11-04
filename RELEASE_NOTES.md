# Create Sky Compat 0.18.0

Configuration groups now share one scrollable document. Navigation jumps to measured section anchors and follows manual scrolling. Drafts, focus and position survive control changes, resize and resource reloads.

Two independent, client-only Iris options can exclude text displays and entity nametags from shader shadow passes. Both default off. Enable them under Client → Iris; normal camera rendering and entity-body shadows remain unchanged. Custom renderers that bypass the vanilla text/nametag boundaries are outside this coverage.

Verified with Iris 1.8.14-beta.1+mc1.21.1, Complementary/Euphoria and patched BSL. Minimum client and dedicated-server loading remain supported without Iris. Existing generation and other optional adaptations are unchanged.

See the configuration and rendering documentation for coverage and update checks.
