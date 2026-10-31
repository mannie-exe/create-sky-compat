package design.inherent.createskycompat.client;

import java.util.ArrayList;
import java.util.List;
import design.inherent.createskycompat.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

/** File-owned settings and startup decisions remain visible without loading optional APIs. */
public final class ConfigurationOverview extends Screen {
    private final ModContainer container;
    private final Screen parent;
    public ConfigurationOverview(ModContainer container, Screen parent) {
        super(text("title")); this.container = container; this.parent = parent;
    }
    private static Component text(String key, Object... args) { return Component.translatable("create_sky_compat.configuration." + key, args); }
    @Override protected void init() {
        int x = (width - 240) / 2, y = Math.max(46 + font.split(text("local_scope"), Math.min(420, width - 24)).size() * font.lineHeight, height / 2 - 42);
        var registered = Button.builder(text("registered"), button -> minecraft.setScreen(new ConfigurationScreen(container, this))).bounds(x, y, 240, 20).build();
        registered.active = java.util.Arrays.stream(net.neoforged.fml.config.ModConfig.Type.values())
                .flatMap(type -> net.neoforged.fml.config.ModConfigs.getConfigSet(type).stream())
                .anyMatch(config -> config.getModId().equals(container.getModId()));
        if (!registered.active) registered.setTooltip(net.minecraft.client.gui.components.Tooltip.create(text("no_registered")));
        addRenderableWidget(registered);
        addRenderableWidget(Button.builder(text("adaptations"), button -> minecraft.setScreen(new InformationScreen(this, text("adaptations"), statuses()))).bounds(x, y + 28, 240, 20).build());
        addRenderableWidget(Button.builder(text("files"), button -> minecraft.setScreen(new InformationScreen(this, text("files"), files()))).bounds(x, y + 56, 240, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose()).bounds(x, height - 30, 240, 20).build());
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public void render(GuiGraphics graphics, int mx, int my, float tick) {
        renderBackground(graphics, mx, my, tick); super.render(graphics, mx, my, tick);
        graphics.drawCenteredString(font, title, width / 2, 18, 0xffffff);
        graphics.drawWordWrap(font, text("local_scope"), Math.max(12, (width - 420) / 2), 34, Math.min(420, width - 24), 0xc0c0c0);
    }
    private record Information(Component title, Component detail) {}
    private static List<Information> statuses() {
        var rows = new ArrayList<Information>(); var selection = OptionalIntegrations.selection();
        for (var feature : Feature.values()) {
            String side = feature == Feature.BLUEMAP ? "dedicated" : FeatureSelection.clientOnly(feature) ? "client" : "both";
            Component reason;
            if (selection.enabled(feature)) reason = text("active");
            else if (!OptionalIntegrations.configured(feature)) reason = text("disabled");
            else if (FeatureSelection.clientOnly(feature) && !net.neoforged.fml.loading.FMLEnvironment.dist.isClient()
                    || feature == Feature.BLUEMAP && net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) reason = text("wrong_side");
            else if (FeatureSelection.prerequisite(feature) != null && !selection.enabled(FeatureSelection.prerequisite(feature)))
                reason = text("prerequisite", FeatureSelection.prerequisite(feature).key);
            else reason = text("missing", String.join(", ", FeatureSelection.dependencies(feature).keySet().stream().filter(id -> !OptionalIntegrations.loaded(id)).sorted().toList()));
            String dependencies = String.join(", ", FeatureSelection.dependencies(feature).entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).map(e -> e.getKey() + " " + (e.getValue().endsWith(".") ? e.getValue() + "x" : e.getValue())).toList());
            Component configured = Component.translatable(OptionalIntegrations.configured(feature) ? "options.on" : "options.off");
            Component detail = Component.literal(feature.key + "\n").append(text("configured", configured, OptionalIntegrations.defaulted(feature) ? text("defaulted") : text("explicit")))
                    .copy().append("\n").append(reason).append("\n").append(text("scope", text(side)))
                    .append("\n").append(text("requires", dependencies));
            rows.add(new Information(text("feature." + feature.key), detail));
        }
        return rows;
    }
    private static List<Information> files() {
        var rows = new ArrayList<Information>();
        rows.add(new Information(Component.literal("config/" + FeatureSettings.FILE), text("startup_help")));
        rows.add(new Information(Component.literal("config/create_sky_compat-common.toml"), text("village_help")));
        for (String family : List.of("generation", "interiors", "water", "oceans"))
            rows.add(new Information(Component.literal("config/create_sky_compat-" + family + ".json"), text("terrain_help")));
        rows.add(new Information(Component.literal("config/create_sky_compat-bluemap.json"), text("bluemap_help")));
        rows.add(new Information(Component.literal("config/create_sky_compat-journal-client.toml"), text("journal_help")));
        return rows;
    }
    private static final class InformationScreen extends Screen {
        private final Screen parent;
        private final List<Information> rows;
        InformationScreen(Screen parent, Component title, List<Information> rows) { super(title); this.parent = parent; this.rows = rows; }
        @Override protected void init() {
            int rowWidth = Math.max(80, width - 64);
            int rowHeight = rows.stream().mapToInt(r -> 12 + (font.split(r.title, rowWidth).size() + font.split(r.detail, rowWidth).size()) * font.lineHeight).max().orElse(40);
            int top = 36 + font.split(text("local_scope"), width - 24).size() * font.lineHeight;
            var list = new InfoList(minecraft, width, Math.max(40, height - top - 36), top, rowHeight);
            rows.forEach(r -> list.append(new InfoEntry(r, rowWidth))); addRenderableWidget(list);
            addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose()).bounds((width - 160) / 2, height - 28, 160, 20).build());
        }
        @Override public void onClose() { minecraft.setScreen(parent); }
        @Override public void render(GuiGraphics graphics, int mx, int my, float tick) {
            renderBackground(graphics, mx, my, tick); super.render(graphics, mx, my, tick); graphics.drawCenteredString(font, title, width / 2, 14, 0xffffff);
            graphics.drawWordWrap(font, text("local_scope"), 12, 30, width - 24, 0xc0c0c0);
        }
        private final class InfoList extends ObjectSelectionList<InfoEntry> {
            InfoList(Minecraft mc, int width, int height, int y, int rowHeight) { super(mc, width, height, y, rowHeight); }
            @Override public int getRowWidth() { return width - 50; }
            void append(InfoEntry row) { addEntry(row); }
        }
        private final class InfoEntry extends ObjectSelectionList.Entry<InfoEntry> {
            private final Information row;
            private final int wrap;
            InfoEntry(Information row, int wrap) { this.row = row; this.wrap = wrap; }
            @Override public Component getNarration() { return row.title.copy().append(". ").append(row.detail); }
            @Override public void render(GuiGraphics graphics, int index, int y, int x, int width, int height, int mx, int my, boolean hovered, float tick) {
                graphics.drawWordWrap(font, row.title, x, y + 3, wrap, 0xffffff);
                graphics.drawWordWrap(font, row.detail, x, y + 7 + font.split(row.title, wrap).size() * font.lineHeight, wrap, 0xc0c0c0);
            }
        }
    }
}
