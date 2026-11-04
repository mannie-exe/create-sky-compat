package design.inherent.skyweavecompat.terrain;

/** Per-column bounds follow the native prepared-density lifetime and need no global cache. */
public interface PreparedIslandShape {
    IslandUnderside.Bounds skyweave$bounds();
    void skyweave$bounds(IslandUnderside.Bounds bounds);
}
