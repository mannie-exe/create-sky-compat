package design.inherent.skyweavecompat.clientprobe;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.nio.file.Files;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Finite native scene inspection commands for the disposable Linear review client. */
final class ClientGuidanceProbe {
    ClientGuidanceProbe() {
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) -> {
            event.getDispatcher().register(Commands.literal("compatprobe_ponder")
                    .then(Commands.argument("item", StringArgumentType.greedyString()).executes(context -> {
                        Minecraft.getInstance().setScreen(PonderUI.of(ResourceLocation.parse(StringArgumentType.getString(context, "item"))));
                        return 1;
                    })));
            event.getDispatcher().register(Commands.literal("compatprobe_guidance_check").executes(context -> {
                var rows = new JsonArray();
                var items = new java.util.LinkedHashMap<String,Integer>();
                if (net.neoforged.fml.ModList.get().isLoaded("aeronautics_utility_objects")) {
                    items.put("aeronautics_utility_objects:hydraulic_rod",2);
                    items.put("aeronautics_utility_objects:hydraulic_regulator",1);
                    items.put("aeronautics_utility_objects:hydraulic_hinge_head",1);
                    items.put("aeronautics_utility_objects:damping_stress_bearing",1);
                    items.put("aeronautics_utility_objects:brass_universal_joint",1);
                }
                if (net.neoforged.fml.ModList.get().isLoaded("create_redstone_additions")) {
                    for(String item : new String[]{"diode","conjunctor","disjunctor","inverter","crossroad","iron_resistor"}) items.put("create_redstone_additions:"+item,1);
                }
                for (String item : new String[]{"linearbearing:linear_bearing", "linearbearing:magnetic_port",
                        "linearbearing:torsional_anchor", "linearbearing:gyroscopic_gearbox",
                        "absolute_kinematics:hinge", "absolute_kinematics:ball_joint"}) {
                    int expected = item.endsWith(":linear_bearing") || item.startsWith("absolute_kinematics:") ? 3 : 1;
                    items.put(item,expected);
                }
                for (var entry : items.entrySet()) {
                    var item=entry.getKey();var expected=entry.getValue();
                    var scenes=PonderIndex.getSceneAccess().compile(ResourceLocation.parse(item));
                    if (scenes.size() != expected) throw new IllegalStateException(item + " compiled " + scenes.size() + " scenes, expected " + expected);
                    for (var scene : scenes) {
                        scene.begin();
                        scene.seekToTime(scene.getTotalTime());
                        var row = new JsonObject();
                        row.addProperty("item", item); row.addProperty("scene", scene.getId().toString());
                        row.addProperty("title", scene.getTitle()); row.addProperty("ticks", scene.getTotalTime());
                        if (scene.getTitle().contains(".ponder.")) throw new IllegalStateException("Untranslated scene title: " + scene.getTitle());
                        rows.add(row);
                    }
                }
                try {
                    Files.writeString(FMLPaths.GAMEDIR.get().resolve("guidance-client-check.json"), new GsonBuilder().setPrettyPrinting().create().toJson(rows));
                } catch (java.io.IOException failure) { throw new IllegalStateException(failure); }
                return 1;
            }));
        });
    }
}
