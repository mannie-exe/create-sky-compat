package design.inherent.createskycompat.probe;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import design.inherent.createskycompat.OptionalMixinPlugin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.fml.ModList;
import java.nio.file.Files;
import java.nio.file.Path;

/** Exercises the applied upstream setup hook, not a replacement registry or mock. */
final class AnalogAudioProbe {
    static void run(MinecraftServer server, Path output) throws Exception {
        var report = new JsonObject();
        boolean audio = ModList.get().isLoaded("analogaudio"), create = ModList.get().isLoaded("create");
        report.addProperty("analogAudio", audio);
        report.addProperty("create", create);
        boolean selected = new OptionalMixinPlugin().getMixins().contains("AnalogAudioCreateMixin");
        report.addProperty("adapterSelected", selected);
        if (create) {
            var behavior = Class.forName("com.simibubi.create.api.behaviour.movement.MovementBehaviour");
            var registry = behavior.getField("REGISTRY").get(null);
            var get = Class.forName("com.simibubi.create.api.registry.SimpleRegistry").getMethod("get", Object.class);
            var drill = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:mechanical_drill"));
            if (get.invoke(registry, drill) == null) throw new AssertionError("Missing control drill handler");
            report.addProperty("drillHandler", get.invoke(registry, drill).getClass().getName());
            if (audio) {
                var radio = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("analogaudio:radio"));
                var handler = get.invoke(registry, radio);
                if (handler == null || !handler.getClass().getName().equals(
                        "com.palm1.analogaudio.integration.create.RadioMovementBehaviour"))
                    throw new AssertionError("Upstream radio movement handler not registered");
                report.addProperty("radioHandler", handler.getClass().getName());
                var context = Class.forName("com.simibubi.create.content.contraptions.behaviour.MovementContext");
                var contraption = Class.forName("com.simibubi.create.content.contraptions.Contraption");
                var fixture = context.getConstructor(Level.class, StructureBlockInfo.class, contraption)
                        .newInstance(server.overworld(), new StructureBlockInfo(BlockPos.ZERO,
                                radio.defaultBlockState(), new CompoundTag()), null);
                behavior.getMethod("tick", context).invoke(handler, fixture);
                behavior.getMethod("stopMoving", context).invoke(handler, fixture);
                report.addProperty("serverCallbacks", "tick and stopMoving completed without client classes");
            }
        }
        if (selected != (audio && create)) throw new AssertionError("Unexpected adapter selection");
        report.addProperty("complete", true);
        Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(report));
    }
}
