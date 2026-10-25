package design.inherent.createskycompat.mixin;

import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StructurePlacement.class)
public interface PlacementSaltAccessor {
    @Accessor("salt") int createSky$salt();
}
