package design.inherent.skyweavecompat.clientprobe;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import design.inherent.skyweavecompat.client.BlockMaterialScope;
import java.nio.file.Files;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.system.MemoryUtil;

/** Native transformed-buffer observation in the existing opt-in lighting fixture. */
final class ClientColorwheelProbe {
    private int frames;

    ClientColorwheelProbe() { NeoForge.EVENT_BUS.addListener(this::frame); }

    private void frame(RenderFrameEvent.Post event) {
        if (Minecraft.getInstance().level == null || ++frames % 240 != 0) return;
        var result = new JsonObject();
        try {
            var backend = Class.forName("dev.engine_room.flywheel.impl.BackendManagerImpl");
            result.addProperty("backend", (String)backend.getMethod("getBackendString").invoke(null));
            result.addProperty("shaders", net.irisshaders.iris.Iris.isPackInUseQuick());
            result.addProperty("textureUnits", net.irisshaders.iris.gl.sampler.SamplerLimits.get().getMaxTextureUnits());
            // Iris fills extended attributes only while its terrain format is active.
            if (net.irisshaders.iris.Iris.isPackInUseQuick()) try (var memory = new ByteBufferBuilder(4096)) {
                var buffer = new BufferBuilder(memory, VertexFormat.Mode.QUADS, IrisVertexFormats.TERRAIN);
                var sensitive = (BlockSensitiveBufferBuilder) buffer;
                result.add("before", metadata(buffer));
                BlockMaterialScope.render(sensitive, new BlockMaterialScope.Material(10089, (byte) 15, 3, 4, 5), () -> {
                    result.add("during", metadata(buffer));
                    for (int i = 0; i < 4; i++) {
                        buffer.addVertex(i & 1, 0, i >> 1).setColor(255, 255, 255, 255)
                                .setUv(i & 1, i >> 1).setUv2(0, 0).setNormal(0, 1, 0);
                    }
                    try {
                        var pointer = field(buffer, "vertexPointer");
                        var offsets = (int[]) field(buffer, "offsetsByElement");
                        result.addProperty("packedEmission", MemoryUtil.memGetByte((Long)pointer
                                + offsets[IrisVertexFormats.MID_BLOCK_ELEMENT.id()] + 3));
                    } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
                });
                try (var mesh = buffer.build()) { result.addProperty("vertices", mesh.drawState().vertexCount()); }
            }
            Files.writeString(FMLPaths.GAMEDIR.get().resolve("colorwheel-state.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(result));
        } catch (ReflectiveOperationException | java.io.IOException failure) {
            throw new IllegalStateException("Pinned Colorwheel fixture changed", failure);
        }
    }

    private static JsonObject metadata(BufferBuilder buffer) {
        var state = new JsonObject();
        try {
            state.addProperty("terrain", (Boolean) field(buffer, "colorwheel$isTerrain"));
            state.addProperty("emission", (Byte) field(buffer, "colorwheel$lightEmission"));
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
        return state;
    }

    private static Object field(Object instance, String name) throws ReflectiveOperationException {
        var field = instance.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(instance);
    }
}
