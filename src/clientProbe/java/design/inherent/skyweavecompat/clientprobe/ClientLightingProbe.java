package design.inherent.skyweavecompat.clientprobe;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in real client light-engine control. Never opens a personal world or ships in Compat. */
final class ClientLightingProbe {
    private static final boolean MINIMUM = Boolean.getBoolean("skyweave_compat.probe.lighting.minimum");
    private static final BlockPos SOURCE = new BlockPos(4, -58, 4);
    private static final BlockPos SAMPLE = SOURCE.east();
    private final Object owner = new Object();
    private final JsonObject report = new JsonObject();
    private boolean opened, unloadReported;
    private volatile boolean prepared;
    private volatile int ticks;
    private Object ship;
    private BlockPos shipOffset;
    private boolean scene, bearings, removed;
    private final java.util.ArrayList<Long> frameNanos = new java.util.ArrayList<>();
    private long previousFrame;
    private int dimensionStage;
    private boolean checkedServer;
    private volatile int serverLight, serverDynamicSample, serverStaticSample;
    private long cpuStart, cpuEnd, sampleStart, sampleEnd;
    private final com.google.gson.JsonArray samples = new com.google.gson.JsonArray();
    ClientLightingProbe() {
        NeoForge.EVENT_BUS.addListener(this::clientTick);
        NeoForge.EVENT_BUS.addListener(this::serverTick);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) -> {
            long now = System.nanoTime();
            if (ticks >= 500 && ticks <= 850 && previousFrame != 0) frameNanos.add(now - previousFrame);
            previousFrame = now;
        });
    }
    private void clientTick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (!opened && mc.getOverlay() == null && mc.screen instanceof TitleScreen) {
            opened = true;
            if (Files.isDirectory(FMLPaths.GAMEDIR.get().resolve("saves/lighting-review")))
                mc.createWorldOpenFlows().openWorld("lighting-review", () -> mc.setScreen(new TitleScreen()));
            else mc.createWorldOpenFlows().createFreshLevel("lighting-review",
                    new LevelSettings("Lighting review", GameType.CREATIVE, false, Difficulty.PEACEFUL,
                            true, new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(42, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                            .get(ResourceLocation.withDefaultNamespace("flat")).createWorldDimensions(), mc.screen);
        }
        if (mc.level == null && prepared && !unloadReported && !MINIMUM) {
            unloadReported = true;
            try {
                var state = sourceState();
                Files.writeString(FMLPaths.GAMEDIR.get().resolve("lighting-unload.json"), state.toString());
            } catch (ReflectiveOperationException | java.io.IOException failure) {
                throw new IllegalStateException("Lighting unload fixture failed", failure);
            }
        }
        if (mc.level == null || mc.player == null || !prepared) return;
        if (ticks > 1700) return;
        ticks++;
        if (MINIMUM) {
            if (ticks == 80) {
                report.addProperty("world", mc.level.dimension().location().toString());
                report.addProperty("player", mc.player != null);
                report.addProperty("ticks", ticks);
                try { Files.writeString(FMLPaths.GAMEDIR.get().resolve("minimum-client.json"), report.toString()); }
                catch (java.io.IOException failure) { throw new IllegalStateException(failure); }
            }
            return;
        }
        if (ticks == 500) { sampleStart = System.nanoTime(); cpuStart = java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime(); }
        if (ticks == 850) { sampleEnd = System.nanoTime(); cpuEnd = java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime(); }
        try {
            var manager = Class.forName("fr0nick.sabledynlights.virtual.VirtualLightManager");
            if (ticks == 160) {
                report.addProperty("before", mc.level.getBrightness(LightLayer.BLOCK, SAMPLE));
                manager.getMethod("syncExternalPositions", Object.class, Map.class).invoke(null, owner, Map.of(SOURCE, 15));
            }
            if (ticks == 200) {
                report.addProperty("virtualEmission", (Integer) manager.getMethod("getVirtualEmission", BlockPos.class).invoke(null, SOURCE));
                report.addProperty("nativeSource", mc.level.getLightEngine().getLayerListener(LightLayer.BLOCK).getLightValue(SOURCE));
                report.addProperty("serverAdjacent", serverLight);
                report.addProperty("sourcePresent", mc.level.getBrightness(LightLayer.BLOCK, SAMPLE));
                manager.getMethod("clearExternalPositions", Object.class).invoke(null, owner);
            }
            if (ticks == 260) {
                report.addProperty("afterRemoval", mc.level.getBrightness(LightLayer.BLOCK, SAMPLE));
                report.add("ownersBeforeUnload", sourceState());
                report.addProperty("sourceBlock", mc.level.getBlockState(SOURCE).toString());
                report.addProperty("thread", Thread.currentThread().getName());
                Files.writeString(FMLPaths.GAMEDIR.get().resolve("lighting-control.json"),
                        new GsonBuilder().setPrettyPrinting().create().toJson(report));
            }
            if (!Boolean.getBoolean("skyweave_compat.probe.lighting.controlOnly") && ticks >= 400 && ticks % 40 == 0) {
                var row = new JsonObject(); row.addProperty("tick", ticks); row.addProperty("dimension", mc.level.dimension().location().toString());
                row.addProperty("serverDynamic", serverDynamicSample); row.addProperty("serverStatic", serverStaticSample);
                row.addProperty("worldLight", mc.level.getBrightness(LightLayer.BLOCK, new BlockPos(12, -58, 4)));
                Object sources = Class.forName("fr0nick.sabledynlights.lamb.LambDynLightsDelegate")
                        .getMethod("getActiveSources").invoke(null);
                row.addProperty("sources", ((java.util.Collection<?>) sources).size());
                var ships = dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(mc.level).getAllSubLevels();
                row.addProperty("ships", ships.size());
                var points = new com.google.gson.JsonArray();
                for (Object source : (java.util.Collection<?>) sources) {
                    var base = Class.forName("dev.lambdaurora.lambdynlights.DynamicLightSource");
                    var point = new JsonObject(); point.addProperty("type", source.getClass().getSimpleName());
                    for (String axis : new String[]{"X", "Y", "Z"}) point.addProperty(axis,
                            (Number)base.getMethod("getDynamicLight" + axis).invoke(source));
                    point.addProperty("light", (Number)base.getMethod("getLuminance").invoke(source));
                    var sourceLevel = (Level)base.getMethod("getDynamicLightLevel").invoke(source);
                    point.addProperty("dimension", sourceLevel.dimension().location().toString()); points.add(point);
                }
                row.add("lights", points); samples.add(row); report.add("samples", samples);
                if (ticks >= 880 && !frameNanos.isEmpty()) {
                    var sorted = frameNanos.stream().sorted().toList();
                    var frames = new JsonObject(); frames.addProperty("count", sorted.size());
                    frames.addProperty("renderThreadCpuMs", (cpuEnd - cpuStart) / 1e6);
                    frames.addProperty("wallMs", (sampleEnd - sampleStart) / 1e6);
                    frames.addProperty("medianMs", sorted.get(sorted.size() / 2) / 1e6);
                    frames.addProperty("p95Ms", sorted.get((int)(sorted.size() * .95)) / 1e6);
                    frames.addProperty("p99Ms", sorted.get((int)(sorted.size() * .99)) / 1e6);
                    report.add("frameIntervals", frames);
                }
                Files.writeString(FMLPaths.GAMEDIR.get().resolve("lighting-scene.json"),
                        new GsonBuilder().setPrettyPrinting().create().toJson(report));
            }
        } catch (ReflectiveOperationException | java.io.IOException error) {
            throw new IllegalStateException("Pinned lighting fixture failed", error);
        }
    }
    private void serverTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getPlayerCount() == 0) return;
        var world = server.overworld();
        if (prepared) {
            if (MINIMUM) return;
            if (!checkedServer && ticks >= 180) { checkedServer = true; world.getLightEngine().checkBlock(SOURCE); }
            if (ticks >= 190 && ticks < 200) serverLight = world.getLightEngine().getLayerListener(LightLayer.BLOCK).getLightValue(SAMPLE);
            if (Boolean.getBoolean("skyweave_compat.probe.lighting.controlOnly")) return;
            try {
                if (!scene && ticks > 300) { scene = true; createScene(world); }
                if (scene && ticks >= 500) {
                    serverDynamicSample = world.getBrightness(LightLayer.BLOCK, new BlockPos(-7, -57, 1));
                    serverStaticSample = world.getBrightness(LightLayer.BLOCK, new BlockPos(-3, -59, 0));
                }
                if (scene && !bearings && ticks > 360) {
                    bearings = true;
                    startBearing(world, new BlockPos(-8, -60, 0));
                    startBearing(world, new BlockPos(17, -54, 1).offset(shipOffset));
                }
                if (ship != null && !removed && ticks > 1120) {
                    removed = true;
                    var reason = Class.forName("dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason");
                    @SuppressWarnings({"unchecked", "rawtypes"}) Object deleted = Enum.valueOf((Class)reason, "REMOVED");
                    Object container = dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(world);
                    container.getClass().getMethod("removeSubLevel", dev.ryanhcode.sable.sublevel.SubLevel.class, reason)
                            .invoke(container, ship, deleted);
                }
                if (ticks >= 1250 && dimensionStage == 0) { dimensionStage++; server.getCommands().performPrefixedCommand(
                        server.getPlayerList().getPlayers().getFirst().createCommandSourceStack(),
                        "execute in minecraft:the_nether run tp @s 0 80 0"); }
                if (ticks >= 1350 && dimensionStage == 1) { dimensionStage++; server.getCommands().performPrefixedCommand(
                        server.getPlayerList().getPlayers().getFirst().createCommandSourceStack(),
                        "execute in minecraft:overworld run tp @s 4 -60 10"); }
                if (ticks >= 1450 && dimensionStage == 2) { dimensionStage++; server.getCommands().performPrefixedCommand(
                        server.getPlayerList().getPlayers().getFirst().createCommandSourceStack(), "tp @s 1024 -60 0"); }
                if (ticks >= 1550 && dimensionStage == 3) { dimensionStage++; server.getCommands().performPrefixedCommand(
                        server.getPlayerList().getPlayers().getFirst().createCommandSourceStack(), "tp @s 4 -60 10"); }
                if (ship != null && ticks > 450 && ticks < 900) {
                    var type = Class.forName("dev.ryanhcode.sable.sublevel.ServerSubLevel");
                    var handle = Class.forName("dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle");
                    Object body = handle.getMethod("of", type).invoke(null, ship);
                    double angle = (ticks - 450) * .003;
                    handle.getMethod("teleport", org.joml.Vector3dc.class, org.joml.Quaterniondc.class).invoke(body,
                            new org.joml.Vector3d(12 + Math.sin(angle) * 3, -55, 2), new org.joml.Quaterniond().rotateY(angle));
                    var system = Class.forName("dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem");
                    Object physics = system.getMethod("get", Level.class).invoke(null, world);
                    system.getMethod("updatePose", type).invoke(physics, ship);
                }
            } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Pinned lighting scene failed", failure); }
            return;
        }
        if (net.neoforged.fml.ModList.get().isLoaded("sable")) {
            try {
                var system = Class.forName("dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem");
                system.getMethod("setPaused", boolean.class).invoke(system.getMethod("get", Level.class).invoke(null, world), true);
            } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Fixture physics changed", failure); }
        }
        world.setDayTime(18000);
        world.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        world.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        var player = server.getPlayerList().getPlayers().getFirst();
        server.getPlayerList().op(player.getGameProfile()); server.getCommands().sendCommands(player);
        player.teleportTo(4.5, -60, 10.5); player.setYRot(180); player.setXRot(10);
        prepared = true;
    }
    private void createScene(ServerLevel world) throws ReflectiveOperationException {
        for (int x = -16; x <= 24; x++) for (int z = -8; z <= 16; z++)
            world.setBlock(new BlockPos(x, -61, z), Blocks.STONE.defaultBlockState(), 3);
        world.setBlock(new BlockPos(-3, -60, 0), Blocks.LANTERN.defaultBlockState(), 3);
        bearing(world, new BlockPos(-8, -60, 0));
        var origin = new BlockPos(10, -55, 0);
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) {
            world.setBlock(origin.offset(x, 0, z), Blocks.SLIME_BLOCK.defaultBlockState(), 3);
            for (int y = 1; y <= 4; y++) if (x == 0 || x == 4 || z == 0 || z == 4 || y == 4)
                world.setBlock(origin.offset(x, y, z), Blocks.STONE.defaultBlockState(), 3);
        }
        // Doorway, interior lantern, and an actual bearing/motor carried by the ship.
        world.setBlock(origin.offset(2, 1, 4), Blocks.AIR.defaultBlockState(), 3);
        world.setBlock(origin.offset(2, 2, 4), Blocks.AIR.defaultBlockState(), 3);
        world.setBlock(origin.offset(3, 1, 3), Blocks.LANTERN.defaultBlockState(), 3);
        for (int x = 14; x <= 17; x++) world.setBlock(new BlockPos(x, -55, 1),
                Blocks.SLIME_BLOCK.defaultBlockState(), 3);
        bearing(world, new BlockPos(17, -54, 1));
        var glueType = Class.forName("com.simibubi.create.content.contraptions.glue.SuperGlueEntity");
        var glue = (Entity)glueType.getConstructor(Level.class, net.minecraft.world.phys.AABB.class)
                .newInstance(world, new net.minecraft.world.phys.AABB(10, -55, 0, 19, -50, 5));
        world.addFreshEntity(glue);
        var system = Class.forName("dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem");
        Object physics = system.getMethod("get", Level.class).invoke(null, world);
        system.getMethod("setPaused", boolean.class).invoke(physics, true);
        var helper = Class.forName("dev.simulated_team.simulated.util.SimAssemblyHelper");
        Object result = helper.getMethod("assembleFromSingleBlock", Level.class, BlockPos.class,
                BlockPos.class, boolean.class, boolean.class).invoke(null, world, origin, origin, true, true);
        if (result == null) throw new AssertionError("Lighting cabin did not assemble");
        ship = result.getClass().getMethod("subLevel").invoke(result);
        shipOffset = (BlockPos)result.getClass().getMethod("offset").invoke(result);
        world.getServer().getPlayerList().getPlayers().getFirst().teleportTo(3.5, -60, 12.5);
    }
    private void bearing(ServerLevel world, BlockPos pos) {
        for (int i = 0; i < 2; i++) {
            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create",
                    i == 0 ? "creative_motor" : "mechanical_bearing"));
            world.setBlock(pos.above(i), block.defaultBlockState().setValue(
                    BlockStateProperties.FACING, net.minecraft.core.Direction.UP), 3);
        }
        world.setBlock(pos.above(2), Blocks.SLIME_BLOCK.defaultBlockState(), 3);
        world.setBlock(pos.above(2).east(), Blocks.SLIME_BLOCK.defaultBlockState(), 3);
        world.setBlock(pos.above(3).east(), Blocks.LANTERN.defaultBlockState(), 3);
    }
    private void startBearing(ServerLevel world, BlockPos pos) throws ReflectiveOperationException {
        var motor = world.getBlockEntity(pos);
        if (motor == null) throw new AssertionError("Missing fixture motor " + pos);
        Object speed = motor.getClass().getField("generatedSpeed").get(motor);
        speed.getClass().getMethod("setValue", int.class).invoke(speed, 16);
        var bearing = world.getBlockEntity(pos.above());
        bearing.getClass().getMethod("assemble").invoke(bearing);
    }

    private JsonObject sourceState() throws ReflectiveOperationException {
        var state = new JsonObject();
        for (String kind : new String[]{"create.Create", "sable.Sable"}) {
            var holder = Class.forName("fr0nick.sabledynlights.lamb." + kind + "DynLightSourceHolder");
            var field = holder.getDeclaredField("lightSources"); field.setAccessible(true);
            state.addProperty(kind, ((Map<?, ?>)field.get(holder.getField("INSTANCE").get(null))).size());
        }
        var handler = Class.forName("fr0nick.sabledynlights.lamb.sable.SableSubLevelEventHandler");
        var last = handler.getDeclaredField("lastLevel"); last.setAccessible(true);
        state.addProperty("retainedSableLevel", last.get(null) != null);
        return state;
    }

}
