package design.inherent.skyweavecompat.linear;

import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity;

/** Local reciprocal tracking for Sable's connected-body traversal. */
public interface BrakeRail {
    java.util.List<DockingConnectorBlockEntity> skyweave$carriages();
    void skyweave$addCarriage(DockingConnectorBlockEntity carriage);
    void skyweave$removeCarriage(DockingConnectorBlockEntity carriage);
}
