package design.inherent.createskycompat.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import design.inherent.createskycompat.client.BlockMaterialScope;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.sublevel.render.vanilla.SingleBlockSubLevelWrapper;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Annotates Sable's vanilla mesh path; ordinary world and Sodium meshes retain their owners. */
@Mixin(ModelBlockRenderer.class)
public abstract class ConstructMaterialMixin {
    @WrapMethod(method = {
            "tesselateWithAO(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V",
            "tesselateWithoutAO(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V"})
    private void createSky$material(BlockAndTintGetter view, BakedModel model, BlockState state, BlockPos pos,
            PoseStack poses, VertexConsumer vertices, boolean checkSides, RandomSource random, long seed,
            int overlay, ModelData modelData, RenderType layer, Operation<Void> original) {
        var ids = WorldRenderingSettings.INSTANCE.getBlockStateIds();
        if (ids == null || !(vertices instanceof BlockSensitiveBufferBuilder buffer) || !createSky$isConstruct(view, pos)) {
            original.call(view, model, state, pos, poses, vertices, checkSides, random, seed, overlay, modelData, layer);
            return;
        }
        var material = new BlockMaterialScope.Material(ids.getOrDefault(state, -1), (byte) state.getLightEmission(),
                pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
        BlockMaterialScope.render(buffer, material,
                () -> original.call(view, model, state, pos, poses, vertices, checkSides, random, seed, overlay, modelData, layer));
    }

    @Unique
    private static boolean createSky$isConstruct(BlockAndTintGetter view, BlockPos pos) {
        if (view instanceof SingleBlockSubLevelWrapper) return true;
        if (!(view instanceof SubLevelContainerHolder holder)) return false;
        var container = holder.sable$getPlotContainer();
        return container != null && container.getPlot(new ChunkPos(pos)) != null;
    }
}
