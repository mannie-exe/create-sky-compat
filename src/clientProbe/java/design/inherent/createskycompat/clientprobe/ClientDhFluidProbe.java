package design.inherent.createskycompat.clientprobe;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.seibel.distanthorizons.common.wrappers.block.*;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper_neoforge;
import com.seibel.distanthorizons.core.dataObjects.fullData.sources.FullDataSourceV2;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos;
import com.seibel.distanthorizons.core.dataObjects.transformers.FullDataToRenderDataTransformer;
import com.seibel.distanthorizons.core.util.FullDataPointUtil;
import com.seibel.distanthorizons.core.util.RenderDataPointUtil;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Disposable source/flowing-state fixture and exact DH material/color observations. */
final class ClientDhFluidProbe {
    private boolean opened, measured, materialPlaced;
    private volatile boolean placed;
    private int ticks;

    ClientDhFluidProbe() {
        NeoForge.EVENT_BUS.addListener(this::clientTick);
        NeoForge.EVENT_BUS.addListener(this::serverTick);
    }

    /** Known natural-terrain families, sampled through DH's actual color/conversion path. */
    static void measureTerrain(Minecraft client) throws Exception {
        var wrapper = ClientLevelWrapper_neoforge.getWrapper(client.level);
        var biome = BiomeWrapper_neoforge.getBiomeWrapper(client.level.getBiome(client.player.blockPosition()), wrapper);
        var rows = new JsonArray();
        for (String name : new String[] {"minecraft:stone", "minecraft:deepslate", "minecraft:grass_block",
                "create:zinc_ore", "create:deepslate_zinc_ore", "create:scoria", "create:limestone",
                "create:ochrum", "create:veridium", "create:crimsite", "create:asurine",
                "createpropulsion:platinum_ore", "createpropulsion:deepslate_platinum_ore"}) {
            var key = ResourceLocation.parse(name);
            var registry = net.minecraft.core.registries.BuiltInRegistries.BLOCK;
            if (!registry.containsKey(key)) throw new IllegalStateException("Missing terrain fixture block: " + name);
            var state = registry.get(key).defaultBlockState();
            var wrapped = BlockStateWrapper_neoforge.fromBlockState(state, wrapper);
            try (var data = FullDataSourceV2.createEmpty(DhSectionPos.encode((byte)6, 0, 0))) {
                int id = data.mapping.addIfNotPresentAndGetId(biome, wrapped);
                var column = new LongArrayList();
                column.add(FullDataPointUtil.encode(id, 32, 100 - wrapper.getMinHeight(), (byte)0, (byte)15));
                data.dataPoints[0] = column;
                data.isEmpty = false;
                try (var rendered = FullDataToRenderDataTransformer.transformFullDataToRenderSource(data, wrapper)) {
                    long point = rendered.getDataPoint(0, 0, 0);
                    var row = new JsonObject();
                    row.addProperty("block", name);
                    row.addProperty("color", String.format("%08x", RenderDataPointUtil.getColor(point)));
                    row.addProperty("material", RenderDataPointUtil.getBlockMaterialId(point));
                    row.addProperty("minY", RenderDataPointUtil.getYMin(point) + wrapper.getMinHeight());
                    row.addProperty("maxY", RenderDataPointUtil.getYMax(point) + wrapper.getMinHeight());
                    row.addProperty("particle", client.getModelManager().getBlockModelShaper()
                            .getParticleIcon(state).contents().name().toString());
                    rows.add(row);
                }
            }
        }
        try {
            Files.writeString(FMLPaths.GAMEDIR.get().resolve("dh-terrain-materials.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(rows));
        } catch (java.io.IOException failure) { throw new IllegalStateException("DH terrain probe failed", failure); }
    }

    private void clientTick(ClientTickEvent.Post event) {
        var client = Minecraft.getInstance();
        if (!opened && client.getOverlay() == null && client.screen instanceof TitleScreen) {
            opened = true;
            if (Files.isDirectory(FMLPaths.GAMEDIR.get().resolve("saves/dh-fluid-review"))) {
                placed = true;
                client.createWorldOpenFlows().openWorld("dh-fluid-review", () -> client.setScreen(new TitleScreen()));
                return;
            }
            client.createWorldOpenFlows().createFreshLevel("dh-fluid-review",
                    new LevelSettings("DH fluid review", GameType.CREATIVE, false, Difficulty.PEACEFUL,
                            true, new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(42, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                            .get(ResourceLocation.parse("create_sky_compat:islands_v1")).createWorldDimensions(), client.screen);
        }
        if (!placed || measured || client.level == null || client.player == null || ++ticks < 200) return;
        measured = true;
        try {
            var wrapper = ClientLevelWrapper_neoforge.getWrapper(client.level);
            var biome = BiomeWrapper_neoforge.getBiomeWrapper(client.level.getBiome(client.player.blockPosition()), wrapper);
            var rows = new JsonArray();
            try (var data = FullDataSourceV2.createEmpty(DhSectionPos.encode((byte)6, 0, 0))) {
                for (var block : new net.minecraft.world.level.block.Block[] {Blocks.LAVA, Blocks.WATER}) {
                    for (int level : new int[] {0, 1, 7, 8, 15}) {
                        var state = block.defaultBlockState().setValue(LiquidBlock.LEVEL, level);
                        var wrapped = BlockStateWrapper_neoforge.fromBlockState(state, wrapper);
                        var colors = new ClientBlockStateColorCache_neoforge(state, wrapper);
                        var row = new JsonObject();
                        row.addProperty("state", state.toString());
                        row.addProperty("liquid", wrapped.isLiquid());
                        row.addProperty("material", wrapped.getMaterialId());
                        row.addProperty("color", String.format("%08x", colors.getColor(biome, data, new DhBlockPos(0, 600, 0), false)));
                        row.addProperty("particle", client.getModelManager().getBlockModelShaper().getParticleIcon(state).contents().name().toString());
                        rows.add(row);
                    }
                }
            }
            var result = new JsonObject(); result.add("states", rows);
            var transfers = new JsonArray();
            for (var substrate : new net.minecraft.world.level.block.Block[] {Blocks.STONE, Blocks.LAVA, Blocks.WATER}) {
                for (boolean lichen : new boolean[] {false, true}) {
                    try (var data = FullDataSourceV2.createEmpty(DhSectionPos.encode((byte)6, 0, 0))) {
                        var column = new LongArrayList();
                        if (lichen) {
                            var state = Blocks.GLOW_LICHEN.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST, true);
                            int id = data.mapping.addIfNotPresentAndGetId(biome, BlockStateWrapper_neoforge.fromBlockState(state, wrapper));
                            column.add(FullDataPointUtil.encode(id, 1, 640 - wrapper.getMinHeight(), (byte)7, (byte)15));
                        }
                        int id = data.mapping.addIfNotPresentAndGetId(biome, BlockStateWrapper_neoforge.fromBlockState(substrate.defaultBlockState(), wrapper));
                        column.add(FullDataPointUtil.encode(id, 40, 600 - wrapper.getMinHeight(), (byte)(substrate == Blocks.LAVA ? 15 : 0), (byte)15));
                        data.dataPoints[0] = column; data.isEmpty = false;
                        try (var rendered = FullDataToRenderDataTransformer.transformFullDataToRenderSource(data, wrapper)) {
                            long point = rendered.getDataPoint(0, 0, 0);
                            var row = new JsonObject();
                            row.addProperty("substrate", substrate.toString()); row.addProperty("lichenAbove", lichen);
                            row.addProperty("material", RenderDataPointUtil.getBlockMaterialId(point));
                            row.addProperty("color", String.format("%08x", RenderDataPointUtil.getColor(point)));
                            row.addProperty("minY", RenderDataPointUtil.getYMin(point) + wrapper.getMinHeight());
                            row.addProperty("maxY", RenderDataPointUtil.getYMax(point) + wrapper.getMinHeight());
                            transfers.add(row);
                        }
                    }
                }
            }
            result.add("lichenTransfer", transfers);
            result.addProperty("biome", biome.getSerialString());
            result.addProperty("complete", true);
            Files.writeString(FMLPaths.GAMEDIR.get().resolve("dh-fluid-colors.json"), new GsonBuilder().setPrettyPrinting().create().toJson(result));
        } catch (Exception failure) { throw new IllegalStateException("DH fluid probe failed", failure); }
    }

    private void serverTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getPlayerCount() == 0) return;
        var world = server.overworld();
        if (!materialPlaced) {
            materialPlaced = true;
            for (int x : new int[] {64, 80}) {
                for (int y = 600; y <= 650; y++)
                    world.setBlock(new BlockPos(x + 1, y, 0), Blocks.STONE.defaultBlockState(), 2);
                for (int y = 600; y < 640; y++)
                    world.setBlock(new BlockPos(x, y, 0), Blocks.STONE.defaultBlockState(), 2);
            }
            world.setBlock(new BlockPos(64, 640, 0), Blocks.GLOW_LICHEN.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST, true), 3);
        }
        if (placed) return;
        world.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        world.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        world.setDayTime(6000);
        // Prefilled source/flowing columns sit beside open sources. All retain native fluid ticks.
        for (int i = 0; i < 4; i++) {
            var fluid = i < 2 ? Blocks.LAVA : Blocks.WATER;
            int x = i * 16;
            world.setBlock(new BlockPos(x, 499, 0), Blocks.STONE.defaultBlockState(), 2);
            if (i % 2 == 0) {
                for (int y = 500; y <= 700; y++)
                    world.setBlock(new BlockPos(x, y, 0), fluid.defaultBlockState().setValue(LiquidBlock.LEVEL, y == 700 ? 0 : 8), 2);
            } else world.setBlock(new BlockPos(x, 700, 0), fluid.defaultBlockState(), 3);
        }
        var player = server.getPlayerList().getPlayers().getFirst();
        player.getAbilities().flying = true; player.onUpdateAbilities();
        player.teleportTo(24, 600, 64); player.setYRot(180); player.setXRot(0);
        placed = true;
    }
}
