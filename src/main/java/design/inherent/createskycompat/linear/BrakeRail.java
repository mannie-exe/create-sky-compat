package design.inherent.createskycompat.linear;

import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity;

/** Local reciprocal tracking for Sable's connected-body traversal. */
public interface BrakeRail {
    java.util.List<DockingConnectorBlockEntity> createSky$carriages();
    void createSky$addCarriage(DockingConnectorBlockEntity carriage);
    void createSky$removeCarriage(DockingConnectorBlockEntity carriage);
}
