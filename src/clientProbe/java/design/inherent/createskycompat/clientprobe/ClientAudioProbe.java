package design.inherent.createskycompat.clientprobe;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.palm1.analogaudio.block.entity.RadioBlockEntity;
import com.palm1.analogaudio.item.CassetteData;
import com.palm1.analogaudio.registry.ModBlocks;
import com.palm1.analogaudio.registry.ModDataComponents;
import com.palm1.analogaudio.registry.ModItems;
import com.palm1.analoglib.client.audio.ClientAudioEngine;
import com.palm1.analoglib.client.audio.engine.StreamSessionManager;
import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Finite native queue-position observation; never distributed with the mod. */
final class ClientAudioProbe {
    private boolean opened, placed;
    private int ticks;
    private int controlId = -1, closing;
    private String phase = "initial";
    private final JsonArray samples = new JsonArray();
    ClientAudioProbe() {
        NeoForge.EVENT_BUS.addListener(this::clientTick);
        NeoForge.EVENT_BUS.addListener(this::serverTick);
    }
    private void clientTick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (!opened && mc.getOverlay() == null && mc.screen instanceof TitleScreen) {
            opened = true;
            if (System.getProperty("create_sky_compat.probe.audio.server") != null) { connect(); return; }
            mc.createWorldOpenFlows().createFreshLevel("audio-review",
                    new LevelSettings("Audio review", GameType.CREATIVE, false, Difficulty.PEACEFUL,
                            true, new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(42, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                            .get(ResourceLocation.withDefaultNamespace("flat")).createWorldDimensions(), mc.screen);
        }
        if (++ticks % 20 != 0) return;
        try {
            var control = FMLPaths.GAMEDIR.get().resolve("audio-control.json");
            if (Files.exists(control)) {
                var job = JsonParser.parseString(Files.readString(control)).getAsJsonObject();
                if (job.get("id").getAsInt() != controlId) {
                    controlId = job.get("id").getAsInt();
                    phase = job.get("action").getAsString();
                    switch (phase) {
                        case "insert" -> {
                            if (mc.player == null || mc.gameMode == null) throw new IllegalStateException("Not connected");
                            mc.player.getInventory().selected = 0;
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                                    new BlockHitResult(new Vec3(.5,-59.5,.5), Direction.UP, new BlockPos(0,-60,0), false));
                        }
                        case "disconnect" -> { if (mc.level != null) {
                            mc.getConnection().getConnection().disconnect(net.minecraft.network.chat.Component.literal("Audio fixture reconnect"));
                            mc.disconnect(); mc.setScreen(new TitleScreen());
                        } }
                        case "connect" -> connect();
                        case "close" -> {
                            var server = mc.getSingleplayerServer();
                            if (server != null) server.execute(() -> server.halt(false));
                            closing = 1;
                        }
                        case "observe" -> { }
                        default -> throw new IllegalArgumentException("Unknown audio fixture action: " + phase);
                    }
                }
            }
            if (closing > 0 && ++closing >= 3) { mc.stop(); return; }
            if (mc.level == null || mc.player == null) return;
            var field = ClientAudioEngine.class.getDeclaredField("SESSION_MANAGER");
            field.setAccessible(true);
            var manager = (StreamSessionManager) field.get(null);
            for (var entry : manager.getPlayingStreams().values()) {
                var stream = entry.streamer();
                if (stream == null) continue;
                var row = new JsonObject();
                row.addProperty("phase", phase);
                row.addProperty("controlId", controlId);
                row.addProperty("player", mc.player.getGameProfile().getName());
                row.addProperty("x", mc.player.getX());
                row.addProperty("z", mc.player.getZ());
                row.addProperty("radioRange", com.palm1.analogaudio.config.ModConfig.Synced.globalRadioRange);
                row.addProperty("speakerRange", com.palm1.analogaudio.config.ModConfig.Synced.globalSpeakerRange);
                row.addProperty("gameTick", mc.level.getGameTime());
                row.addProperty("pending", stream.isPending());
                row.addProperty("playing", stream.isPlaying());
                row.addProperty("paused", stream.isPaused());
                row.addProperty("offsetMs", stream.getCurrentOffsetMs());
                var radio = (RadioBlockEntity) mc.level.getBlockEntity(new BlockPos(0, -60, 0));
                if (radio != null) {
                    row.addProperty("expectedMs", (mc.level.getGameTime() - radio.getStartTime()) * 50);
                    row.addProperty("radioPlaying", radio.isPlaying());
                    row.addProperty("startTick", radio.getStartTime());
                }
                samples.add(row);
            }
            Files.writeString(FMLPaths.GAMEDIR.get().resolve("audio-samples.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(samples));
        } catch (ReflectiveOperationException | java.io.IOException failure) { throw new IllegalStateException(failure); }
    }
    private void connect() {
        var mc = Minecraft.getInstance();
        String address = System.getProperty("create_sky_compat.probe.audio.server");
        if (address == null || !address.startsWith("127.0.0.1:")) throw new IllegalArgumentException("Expected loopback fixture server");
        ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(address),
                new ServerData("Audio fixture", address, ServerData.Type.OTHER), false, null);
    }
    private void serverTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (placed || server.getPlayerCount() == 0) return;
        placed = true;
        var level = server.overworld();
        level.setBlockAndUpdate(new BlockPos(0, -61, 0), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(0, -60, 0), ModBlocks.RADIO.get().defaultBlockState());
        var radio = (RadioBlockEntity) level.getBlockEntity(new BlockPos(0, -60, 0));
        var cassette = new ItemStack(ModItems.CASSETTE_TAPE.get());
        cassette.set(ModDataComponents.CASSETTE_DATA.get(), new CassetteData("audio-probe",
                System.getProperty("create_sky_compat.probe.audio.url", "http://127.0.0.1:18849/slow.wav"),
                "Timed fixture", 0x55bb77, .5f, 120_000, ""));
        radio.setItem(0, cassette);
        server.getPlayerList().getPlayers().getFirst().teleportTo(level, .5, -60, 3.5, 0, 0);
    }
}
