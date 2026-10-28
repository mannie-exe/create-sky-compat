package design.inherent.createskycompat.mixin.dh;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.seibel.distanthorizons.core.file.structure.ISaveStructure;
import com.seibel.distanthorizons.core.level.DhClientLevel;
import com.seibel.distanthorizons.core.level.IServerKeyedClientLevel;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import design.inherent.createskycompat.dh.WorldIdentity;
import java.io.File;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A dimension-only legacy directory cannot establish which saved world its terrain belongs to. */
@Mixin(value = DhClientLevel.class, remap = false)
abstract class LegacyCacheMixin {
    @WrapOperation(method = "<init>(Lcom/seibel/distanthorizons/core/file/structure/ISaveStructure;Lcom/seibel/distanthorizons/core/wrapperInterfaces/world/IClientLevelWrapper;Ljava/io/File;Lcom/seibel/distanthorizons/core/multiplayer/client/ClientNetworkState;)V",
            at = @At(value = "INVOKE", target = "Ljava/io/File;exists()Z", ordinal = 0))
    private boolean createSky$legacyCacheExists(File oldFolder, Operation<Boolean> original,
            ISaveStructure saveStructure, IClientLevelWrapper level) {
        if (level instanceof IServerKeyedClientLevel keyed
                && WorldIdentity.isDhLevelKey(keyed.getServerLevelKey())) return false;
        return original.call(oldFolder);
    }
}
