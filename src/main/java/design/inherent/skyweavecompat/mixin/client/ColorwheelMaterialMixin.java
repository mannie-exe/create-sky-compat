package design.inherent.skyweavecompat.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import design.inherent.skyweavecompat.client.BlockMaterialScope;
import design.inherent.skyweavecompat.client.ColorwheelMaterials;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import org.spongepowered.asm.mixin.Mixin;

/** Sable's direct flat/AO paths also need Colorwheel's terrain/emission metadata. */
@Mixin(value = BlockMaterialScope.class, remap = false)
public abstract class ColorwheelMaterialMixin {
    @WrapMethod(method = "render")
    private static void skyweave$colorwheelMaterial(BlockSensitiveBufferBuilder buffer,
            BlockMaterialScope.Material material, Runnable render, Operation<Void> original) {
        BlockMaterialScope.renderWith(buffer, material, ColorwheelMaterials.WRITER, render);
    }
}
