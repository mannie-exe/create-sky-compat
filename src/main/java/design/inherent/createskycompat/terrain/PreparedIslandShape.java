package design.inherent.createskycompat.terrain;

/** Per-column bounds follow the native prepared-density lifetime and need no global cache. */
public interface PreparedIslandShape {
    IslandUnderside.Bounds createSky$bounds();
    void createSky$bounds(IslandUnderside.Bounds bounds);
}
