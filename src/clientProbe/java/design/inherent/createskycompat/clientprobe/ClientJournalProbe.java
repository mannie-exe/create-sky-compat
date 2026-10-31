package design.inherent.createskycompat.clientprobe;

import java.nio.file.Files;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.infernalstudios.questlog.QuestlogClient;
import org.infernalstudios.questlog.client.gui.screen.QuestDetails;

/** Explicit journal-only fixture. No production registration or personal-save access. */
final class ClientJournalProbe {
    private boolean opened, screenOpened, checked;
    private int ticks;
    private long renderStart;
    private final java.util.ArrayList<Double> frameTimes = new java.util.ArrayList<>();
    ClientJournalProbe() {
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RegisterClientCommandsEvent event) ->
            event.getDispatcher().register(net.minecraft.commands.Commands.literal("compatprobe_journal_state").executes(context -> {
                var result = new JsonObject();
                var mc = Minecraft.getInstance();
                result.addProperty("screen", mc.screen == null ? "game" : mc.screen.getClass().getName());
                for (var quest : QuestlogClient.getLocal().getAllQuests()) {
                    if (!quest.getId().getPath().startsWith("ux/")) continue;
                    var state = new JsonObject(); state.addProperty("completed", quest.isCompleted());
                    state.addProperty("rewarded", quest.isRewarded()); state.addProperty("repeatable", quest.isRepeatable());
                    state.addProperty("data", quest.serialize().toString()); result.add(quest.getId().toString(), state);
                }
                write("journal-state.json", result); return 1;
            })));
        NeoForge.EVENT_BUS.addListener(this::clientTick);
        NeoForge.EVENT_BUS.addListener(this::serverTick);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ScreenEvent.Render.Pre event) -> {
            if (event.getScreen() instanceof QuestDetails) renderStart = System.nanoTime();
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ScreenEvent.Render.Post event) -> {
            if (!(event.getScreen() instanceof QuestDetails) || frameTimes.size() >= 360) return;
            frameTimes.add((System.nanoTime() - renderStart) / 1e6);
            if (frameTimes.size() == 360) {
                var measured = frameTimes.subList(60, 360).stream().sorted().toList();
                var result = new JsonObject();
                result.addProperty("frames", measured.size());
                result.addProperty("medianRenderCpuMs", measured.get(150));
                result.addProperty("p95RenderCpuMs", measured.get(285));
                write("journal-render.json", result);
            }
        });
    }
    private void clientTick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (!opened && mc.getOverlay() == null && mc.screen instanceof TitleScreen) {
            opened = true;
            String remote = System.getProperty("create_sky_compat.probe.journal.server");
            if (remote != null) {
                net.minecraft.client.gui.screens.ConnectScreen.startConnecting(mc.screen, mc,
                        net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(remote),
                        new net.minecraft.client.multiplayer.ServerData("Journal fixture", remote,
                                net.minecraft.client.multiplayer.ServerData.Type.OTHER), false, null);
                return;
            }
            if (Files.isDirectory(FMLPaths.GAMEDIR.get().resolve("saves/journal-review")))
                mc.createWorldOpenFlows().openWorld("journal-review", () -> mc.setScreen(new TitleScreen()));
            else mc.createWorldOpenFlows().createFreshLevel("journal-review",
                    new LevelSettings("Journal review", GameType.CREATIVE, false, Difficulty.PEACEFUL,
                            true, new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(42, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                            .get(ResourceLocation.withDefaultNamespace("flat")).createWorldDimensions(), mc.screen);
        }
        if (mc.level == null || mc.player == null || screenOpened || ++ticks < 120) return;
        var quest = QuestlogClient.getLocal().getQuest(ResourceLocation.parse(System.getProperty("create_sky_compat.probe.journal.entry", "questlog:ux/long")));
        if (quest != null) {
            if (Boolean.getBoolean("create_sky_compat.probe.journal.largeIcon")) {
                try {
                    var field = quest.getDisplay().getClass().getDeclaredField("icon"); field.setAccessible(true);
                    field.set(quest.getDisplay(), new org.infernalstudios.questlog.util.texture.Blittable() {
                        public int width() { return 64; }
                        public int height() { return 48; }
                        public void blit(net.minecraft.client.gui.GuiGraphics graphics, int x, int y) {
                            graphics.fill(x, y, x + 64, y + 48, 0xff9e7852);
                            graphics.renderItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.AMETHYST_SHARD), x + 24, y + 16);
                        }
                    });
                } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Pinned journal icon fixture changed", failure); }
            }
            screenOpened = true; mc.setScreen(new QuestDetails(null, quest));
        }
    }
    private void serverTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (checked || server.getPlayerCount() == 0 || server.getTickCount() < 60) return;
        checked = true;
        var player = server.getPlayerList().getPlayers().getFirst();
        server.getPlayerList().op(player.getGameProfile());
        server.getCommands().sendCommands(player);
        write("journal-claims.json", JournalClaimChecks.run(player));
    }
    private static void write(String name, JsonObject result) {
        try { Files.writeString(FMLPaths.GAMEDIR.get().resolve(name), new GsonBuilder().setPrettyPrinting().create().toJson(result)); }
        catch (java.io.IOException error) { throw new IllegalStateException(error); }
    }
}
