package design.inherent.skyweavecompat.mixin.linear;

import design.inherent.skyweavecompat.linear.BrakeRail;
import design.inherent.skyweavecompat.linear.BrakedCarriage;
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
    @Unique private final Set<DockingConnectorBlockEntity> skyweave$carriages = Collections.newSetFromMap(new IdentityHashMap<>());
    protected LinearCasingMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    @Override public void skyweave$addCarriage(DockingConnectorBlockEntity carriage) { skyweave$carriages.add(carriage); }
    @Override public void skyweave$removeCarriage(DockingConnectorBlockEntity carriage) { skyweave$carriages.remove(carriage); }
    @Override public List<DockingConnectorBlockEntity> skyweave$carriages() {
        skyweave$carriages.removeIf(carriage -> carriage.isRemoved());
        return List.copyOf(skyweave$carriages);
    }
    @Override public Iterable<SubLevel> sable$getConnectionDependencies() {
        skyweave$carriages.removeIf(carriage -> carriage.isRemoved());
        return skyweave$carriages.stream().map(Sable.HELPER::getContaining)
                .filter(java.util.Objects::nonNull).distinct().toList();
    }
    @Unique private void skyweave$releaseCarriages() {
        for (var carriage : List.copyOf(skyweave$carriages)) ((BrakedCarriage) carriage).skyweave$releaseBrake();
        skyweave$carriages.clear();
    }
    @Override public void invalidate() { skyweave$releaseCarriages(); super.invalidate(); }
    @Override public void remove() { skyweave$releaseCarriages(); super.remove(); }
}
