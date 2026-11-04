package design.inherent.createskycompat.clientprobe;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import design.inherent.createskycompat.Feature;
import design.inherent.createskycompat.OptionalIntegrations;
import design.inherent.createskycompat.client.ConfigurationOverview;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Finite file-controlled native UI checks in an explicitly marked disposable client. */
final class ClientConfigurationProbe {
    private final Path root = FMLPaths.GAMEDIR.get();
    private boolean opened;
    private int ticks, sequence = -1;
    private String lastError;
    ClientConfigurationProbe() {
        if (!Files.isRegularFile(root.resolve("config-review-fixture.json"))) throw new IllegalStateException("Configuration probe requires its disposable-fixture marker");
        NeoForge.EVENT_BUS.addListener(this::tick);
        if (Boolean.getBoolean("create_sky_compat.probe.configuration.journal")) new ClientJournalProbe();
    }
    private void open() {
        var mc = Minecraft.getInstance();
        mc.setScreen(new ConfigurationOverview(ModList.get().getModContainerById("create_sky_compat").orElseThrow(), mc.screen));
    }
    private void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (++ticks % 10 != 0 || mc.getOverlay() != null) return;
        try {
            if (!opened && !Boolean.getBoolean("create_sky_compat.probe.resources") && !Boolean.getBoolean("create_sky_compat.probe.redstone") && !Boolean.getBoolean("create_sky_compat.probe.configuration.journal") && mc.screen instanceof TitleScreen) { opened = true; open(); }
            Path control = root.resolve("config-review-control.json");
            if (Files.isRegularFile(control) && Files.size(control) < 16384) {
                var command = JsonParser.parseString(Files.readString(control)).getAsJsonObject();
                int seq = command.get("sequence").getAsInt();
                if (seq != sequence) {
                    sequence = seq; lastError = null;
                    switch (command.get("operation").getAsString()) {
                        case "open" -> open();
                        case "click" -> {
                            var found = widgets(mc.screen).stream().filter(w -> w.getMessage().getString().equals(command.get("label").getAsString())).toList();
                            int index = command.has("index") ? command.get("index").getAsInt() : 0;
                            var widget = found.get(index);
                            boolean accepted = mc.screen.mouseClicked(widget.getX() + widget.getWidth() / 2.0, widget.getY() + widget.getHeight() / 2.0, 0);
                            if (!accepted) throw new IllegalStateException("Native click was refused: " + command.get("label"));
                        }
                        case "field" -> {
                            var fields = widgets(mc.screen).stream().filter(w -> w instanceof net.minecraft.client.gui.components.EditBox).toList();
                            ((net.minecraft.client.gui.components.EditBox) fields.get(command.get("index").getAsInt())).setValue(command.get("value").getAsString());
                        }
                        case "key" -> mc.screen.keyPressed(command.get("key").getAsInt(), 0, command.has("modifiers") ? command.get("modifiers").getAsInt() : 0);
                        case "scroll" -> mc.screen.mouseScrolled(command.get("x").getAsDouble(), command.get("y").getAsDouble(), 0, command.get("amount").getAsDouble());
                        case "drag" -> {
                            double x = command.get("x").getAsDouble(), y = command.get("y").getAsDouble();
                            double end = command.get("endY").getAsDouble();
                            if (!mc.screen.mouseClicked(x, y, 0)) throw new IllegalStateException("Native drag press refused");
                            mc.screen.mouseDragged(x, end, 0, 0, end - y); mc.screen.mouseReleased(x, end, 0);
                        }
                        case "language" -> {
                            String language = command.get("value").getAsString();
                            mc.options.languageCode = language; mc.getLanguageManager().setSelected(language); mc.reloadResourcePacks();
                        }
                        case "scale" -> { mc.options.guiScale().set(command.get("value").getAsInt()); mc.resizeDisplay(); }
                        case "resize" -> org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), command.get("width").getAsInt(), command.get("height").getAsInt());
                        case "snapshot" -> {}
                        case "quit" -> mc.stop();
                        default -> throw new IllegalArgumentException("Unknown configuration fixture action");
                    }
                }
            }
            write(lastError);
        } catch (Exception failure) { lastError = failure.toString(); write(lastError); }
    }
    private static ArrayList<AbstractWidget> widgets(GuiEventListener listener) {
        var found = new ArrayList<AbstractWidget>();
        if (listener instanceof AbstractWidget widget) found.add(widget);
        if (listener instanceof ContainerEventHandler container) for (var child : container.children()) found.addAll(widgets(child));
        return found;
    }
    private void write(String error) {
        try {
            var mc = Minecraft.getInstance(); var report = new JsonObject();
            report.addProperty("pid", ProcessHandle.current().pid()); report.addProperty("sequence", sequence); report.addProperty("screen", mc.screen == null ? "none" : mc.screen.getClass().getName());
            report.addProperty("width", mc.getWindow().getGuiScaledWidth()); report.addProperty("height", mc.getWindow().getGuiScaledHeight()); report.addProperty("scale", mc.options.guiScale().get());
            if (error != null) report.addProperty("error", error);
            if (mc.screen != null) report.addProperty("screenFocusAttached", mc.screen.getFocused() == null || mc.screen.children().contains(mc.screen.getFocused()));
            var rows = new JsonArray();
            if (mc.screen != null) for (var widget : widgets(mc.screen)) {
                var row = new JsonObject();row.addProperty("label", widget.getMessage().getString());row.addProperty("x",widget.getX());row.addProperty("y",widget.getY());row.addProperty("width",widget.getWidth());row.addProperty("height",widget.getHeight());row.addProperty("active",widget.active);row.addProperty("focused",widget.isFocused());if(widget instanceof net.minecraft.client.gui.components.EditBox input)row.addProperty("value",input.getValue());rows.add(row);
            }
            report.add("widgets", rows);
            if (mc.screen instanceof ConfigurationOverview overview) {
                for (String name : java.util.List.of("content", "navigation")) {
                    var panelField = ConfigurationOverview.class.getDeclaredField(name); panelField.setAccessible(true);
                    Object panel = panelField.get(overview);
                    for (String property : java.util.List.of("scroll", "activeSection", "maximumScroll", "focusKey")) {
                        var method = panel.getClass().getDeclaredMethod(property); method.setAccessible(true);
                        Object value = method.invoke(panel);
                        report.addProperty(name + "." + property, value == null ? null : value.toString());
                    }
                }
                var field = ConfigurationOverview.class.getDeclaredField("draft"); field.setAccessible(true);
                var draft = (design.inherent.createskycompat.configuration.StartupEdits) field.get(overview);
                if (draft != null) {
                    report.addProperty("dirty", draft.dirty());
                    var values = new JsonObject();var saved = new JsonObject();
                    for (Feature feature : Feature.values()) { values.addProperty(feature.key, draft.value(feature)); saved.addProperty(feature.key, draft.saved().enabled(feature)); }
                    report.add("draft", values);report.add("saved", saved);
                }
            }
            var active = new JsonArray(); for (Feature feature : OptionalIntegrations.selection().active()) active.add(feature.key); report.add("active", active);
            Files.writeString(root.resolve("config-review-state.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        } catch (Exception failure) { throw new IllegalStateException(failure); }
    }
}
