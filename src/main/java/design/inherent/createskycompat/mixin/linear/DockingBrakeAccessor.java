package design.inherent.createskycompat.mixin.linear;

import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintHandle;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Native connector invalidation owns the brake handle's destruction. */
@Mixin(value = DockingConnectorBlockEntity.class, remap = false)
public interface DockingBrakeAccessor {
    @Accessor("constraintHandle") void createSky$brakeHandle(FixedConstraintHandle handle);
}
