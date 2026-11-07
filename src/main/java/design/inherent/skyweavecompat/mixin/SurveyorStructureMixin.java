package design.inherent.skyweavecompat.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Rejected starts have no registered structure and are not map records. */
@Pseudo
@Mixin(targets = "folk.sisby.surveyor.structure.WorldStructures", remap = false)
public abstract class SurveyorStructureMixin {
    @Inject(method = "put(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/levelgen/structure/StructureStart;)V",
            at = @At("HEAD"), cancellable = true)
    private void rejectedStart(ServerLevel level, StructureStart start, CallbackInfo callback) {
        if (!start.isValid()) callback.cancel();
    }
}
