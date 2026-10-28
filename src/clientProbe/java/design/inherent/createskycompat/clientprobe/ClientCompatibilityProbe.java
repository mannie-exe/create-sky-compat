package design.inherent.createskycompat.clientprobe;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in flat-world animation scene and live Sable property inventory. */
final class ClientCompatibilityProbe {
    private boolean opened, placed, measured, terrainMeasured;
    private int clientTicks;

    ClientCompatibilityProbe() {
        NeoForge.EVENT_BUS.addListener(this::clientTick);
        NeoForge.EVENT_BUS.addListener(this::serverTick);
    }

    private void clientTick(ClientTickEvent.Post event) {
        var client = Minecraft.getInstance();
        if (client.level != null && client.player != null && !terrainMeasured && ++clientTicks >= 200) {
            try {
                ClientDhFluidProbe.measureTerrain(client);
            } catch (Exception failure) {
                throw new IllegalStateException("DH terrain probe failed", failure);
            }
            terrainMeasured = true;
        }
        if (opened || client.getOverlay() != null || !(client.screen instanceof TitleScreen)) return;
        opened = true;
        if (Files.isDirectory(FMLPaths.GAMEDIR.get().resolve("saves/compatibility-review"))) {
            placed = true;
            client.createWorldOpenFlows().openWorld("compatibility-review", () -> client.setScreen(new TitleScreen()));
        } else {
            client.createWorldOpenFlows().createFreshLevel("compatibility-review",
                    new LevelSettings("Compatibility review", GameType.CREATIVE, false, Difficulty.NORMAL,
                            true, new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(42, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                            .get(ResourceLocation.withDefaultNamespace("flat")).createWorldDimensions(), client.screen);
        }
    }

    private void serverTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getPlayerCount() == 0) return;
        var world = server.overworld();
        if (!measured) {
            exportMass(world);
            verifyMassAssemblies(world);
            measured = true;
        }
        if (placed) return;
        placed = true;
        world.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        world.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        world.setDayTime(6000);
        for (int x = -8; x < 64; x++) for (int z = -8; z < 16; z++)
            world.setBlock(new BlockPos(x, 99, z), Blocks.SMOOTH_STONE.defaultBlockState(), 2);
        for (int i = 0; i < 4; i++) {
            var foot = new BlockPos(i * 5, 100, 0);
            var head = foot.north();
            var bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH);
            world.setBlock(foot, bed.setValue(BedBlock.PART, BedPart.FOOT), 3);
            world.setBlock(head, bed.setValue(BedBlock.PART, BedPart.HEAD), 3);
            var villager = EntityType.VILLAGER.create(world);
            villager.setNoAi(true); villager.setPersistenceRequired();
            villager.moveTo(head.getX() + .5, 100.6, head.getZ() + .5);
            world.addFreshEntity(villager); villager.startSleeping(head);
        }
        String[] kinds = {"villager", "wandering_trader", "cow", "mooshroom", "sheep", "donkey", "mule", "horse", "guardian", "elder_guardian"};
        for (int i = 0; i < kinds.length; i++) {
            var mob = (Mob) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.withDefaultNamespace(kinds[i])).create(world);
            mob.setNoAi(true); mob.setPersistenceRequired(); mob.setInvulnerable(true);
            mob.setCustomName(Component.literal(kinds[i])); mob.setCustomNameVisible(true);
            mob.moveTo(i * 5 + .5, 100, 7.5, 180, 0); world.addFreshEntity(mob);
        }
        var player = server.getPlayerList().getPlayers().getFirst();
        player.teleportTo(.5, 100, 5.5); player.setYRot(180); player.setXRot(15);
    }

    private void exportMass(net.minecraft.server.level.ServerLevel world) {
        var rows = new JsonArray();
        for (var block : BuiltInRegistries.BLOCK) {
            var state = block.defaultBlockState();
            var row = new JsonObject();
            row.addProperty("id", BuiltInRegistries.BLOCK.getKey(block).toString());
            row.addProperty("mass", PhysicsBlockPropertyHelper.getMass(world, new BlockPos(0, 100, 0), state));
            row.addProperty("volume", PhysicsBlockPropertyHelper.getVolume(state));
            var tags = new JsonArray(); state.getTags().forEach(tag -> tags.add(tag.location().toString()));
            row.add("tags", tags); rows.add(row);
        }
        try {
            Files.writeString(FMLPaths.GAMEDIR.get().resolve("block-mass.json"), new GsonBuilder().setPrettyPrinting().create().toJson(rows));
        } catch (java.io.IOException failure) { throw new IllegalStateException(failure); }
    }

    private void verifyMassAssemblies(net.minecraft.server.level.ServerLevel world) {
        var rows = new JsonArray();
        try {
            var helper = Class.forName("dev.simulated_team.simulated.util.SimAssemblyHelper");
            var assemble = helper.getMethod("assembleFromSingleBlock", Level.class, BlockPos.class,
                    BlockPos.class, boolean.class, boolean.class);
            var disassemble = helper.getMethod("disassembleSubLevel", Level.class,
                    dev.ryanhcode.sable.sublevel.SubLevel.class, BlockPos.class, BlockPos.class,
                    net.minecraft.world.level.block.Rotation.class, boolean.class);
            String[] ids = {"minecraft:oak_planks", "minecraft:iron_block", "mcwlights:covered_lantern",
                    "mcwlights:cross_wall_lantern", "mcwstairs:oak_terrace_stairs",
                    "mcwstairs:granite_terrace_stairs", "createdeco:industrial_iron_bars"};
            for (int i = 0; i < ids.length; i++) {
                var pos = new BlockPos(i * 4, 150, 0);
                var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(ids[i]));
                var state = block.defaultBlockState();
                var anchor = Blocks.SLIME_BLOCK.defaultBlockState();
                world.setBlock(pos, anchor, 2); world.setBlock(pos.above(), state, 2);
                double expected = PhysicsBlockPropertyHelper.getMass(world, pos, anchor)
                        + PhysicsBlockPropertyHelper.getMass(world, pos.above(), state);
                var result = assemble.invoke(null, world, pos, pos, true, true);
                if (result == null) throw new AssertionError("Assembly failed: " + ids[i]);
                var ship = result.getClass().getMethod("subLevel").invoke(result);
                var offset = (BlockPos)result.getClass().getMethod("offset").invoke(result);
                ship.getClass().getMethod("buildMassTracker").invoke(ship);
                var tracker = ship.getClass().getMethod("getSelfMassTracker").invoke(ship);
                double actual = ((Number)tracker.getClass().getMethod("getMass").invoke(tracker)).doubleValue();
                var row = new JsonObject(); row.addProperty("block", ids[i]);
                row.addProperty("expected", expected); row.addProperty("assembled", actual); rows.add(row);
                if (Math.abs(actual - expected) > 1e-6) throw new AssertionError("Mass mismatch: " + row);
                disassemble.invoke(null, world, ship, pos.offset(offset), pos,
                        net.minecraft.world.level.block.Rotation.NONE, false);
                world.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2);
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
            Files.writeString(FMLPaths.GAMEDIR.get().resolve("mass-assemblies.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(rows));
        } catch (ReflectiveOperationException | java.io.IOException failure) {
            throw new IllegalStateException("Mass fixture failed", failure);
        }
    }

}
