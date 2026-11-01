package design.inherent.createskycompat.mixin.linear;

import design.inherent.createskycompat.linear.BrakeRail;
import design.inherent.createskycompat.linear.BrakedCarriage;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = "com.bearing.linearbearing.blockentity.LinearCasingBlockEntity", remap = false)
public abstract class LinearCasingMixin extends DockingConnectorBlockEntity implements BrakeRail {
    @Unique private final Set<DockingConnectorBlockEntity> createSky$carriages = Collections.newSetFromMap(new IdentityHashMap<>());
    protected LinearCasingMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    @Override public void createSky$addCarriage(DockingConnectorBlockEntity carriage) { createSky$carriages.add(carriage); }
    @Override public void createSky$removeCarriage(DockingConnectorBlockEntity carriage) { createSky$carriages.remove(carriage); }
    @Override public List<DockingConnectorBlockEntity> createSky$carriages() {
        createSky$carriages.removeIf(carriage -> carriage.isRemoved());
        return List.copyOf(createSky$carriages);
    }
    @Override public Iterable<SubLevel> sable$getConnectionDependencies() {
        createSky$carriages.removeIf(carriage -> carriage.isRemoved());
        return createSky$carriages.stream().map(Sable.HELPER::getContaining)
                .filter(java.util.Objects::nonNull).distinct().toList();
    }
    @Unique private void createSky$releaseCarriages() {
        for (var carriage : List.copyOf(createSky$carriages)) ((BrakedCarriage) carriage).createSky$releaseBrake();
        createSky$carriages.clear();
    }
    @Override public void invalidate() { createSky$releaseCarriages(); super.invalidate(); }
    @Override public void remove() { createSky$releaseCarriages(); super.remove(); }
}
