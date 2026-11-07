package design.inherent.skyweavecompat.clientprobe;

import com.google.gson.JsonObject;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/** Invoke the installed Surveyor API in the marked integrated test world. */
final class ClientStructureRecordsProbe {
    private ClientStructureRecordsProbe() {}
    static JsonObject observe(Minecraft client, int x, int z, String id) throws Exception {
        var server = client.getSingleplayerServer();
        if (server == null) throw new IllegalStateException("Expected isolated integrated server");
        var result = new CompletableFuture<JsonObject>();
        server.execute(() -> {
            try {
                var level = server.overworld();
                var type = Class.forName("folk.sisby.surveyor.structure.WorldStructures");
                var records = type.getMethod("of",Level.class).invoke(null,level);
                if (records == null) throw new AssertionError("Surveyor structure records are inactive");
                var put = type.getMethod("put",ServerLevel.class,StructureStart.class);
                put.invoke(records,level,StructureStart.INVALID_START);
                var chunk = level.getChunk(x,z,ChunkStatus.STRUCTURE_STARTS);
                var before = Map.copyOf(chunk.getAllStarts());
                var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                        .get(ResourceLocation.parse(id));
                var start = chunk.getStartForStructure(structure);
                if (start == null || !start.isValid()) throw new AssertionError("Known valid structure missing: " + id);
                put.invoke(records,level,start);
                boolean retained = (boolean)type.getMethod("contains",Level.class,StructureStart.class).invoke(records,level,start);
                if (!retained || !before.equals(chunk.getAllStarts())) throw new AssertionError("Map recording changed native starts or lost a valid record");
                var observed = new JsonObject();
                observed.addProperty("invalidIgnored",true); observed.addProperty("validRetained",retained);
                observed.addProperty("nativeStartsUnchanged",true); observed.addProperty("structure",id);
                result.complete(observed);
            } catch (Throwable failure) { result.completeExceptionally(failure); }
        });
        return result.get(30,TimeUnit.SECONDS);
    }
}
