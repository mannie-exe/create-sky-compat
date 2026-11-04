package design.inherent.createskycompat.client;

import design.inherent.createskycompat.*;
import design.inherent.createskycompat.configuration.SettingsCatalog;
import design.inherent.createskycompat.configuration.SettingsCatalog.Category;
import design.inherent.createskycompat.configuration.SettingsCatalog.Group;
import design.inherent.createskycompat.configuration.StartupEdits;
import design.inherent.createskycompat.configuration.SectionNavigation;
import design.inherent.createskycompat.questlog.client.JournalPreferences;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

/** Local preference editing and next-start configuration; no remote administration or live mixin reload. */
public final class ConfigurationOverview extends Screen {
    private final ModContainer container;
    private final Screen parent;
    private StartupEdits draft;
    private Component feedback = Component.empty();
    private Component notice = Component.empty();
    private int noticeY, noticeWidth;
    private Feature detail;
    private SettingsPanel navigation, content;
    private Button save;
    private net.minecraft.locale.Language language;
    private long resourceRevision;
    private float navigationScroll;
    private final Map<String, Float> scroll = new HashMap<>();
    private final Map<String, SettingsPanel.NavigationButton> links = new java.util.LinkedHashMap<>();
    private SectionNavigation.Bookmark documentPosition;
    private String contentFocus, documentFocus, navigationFocus;

    public ConfigurationOverview(ModContainer container, Screen parent) {
        super(Component.translatable("create_sky_compat.configuration.title")); this.container = container; this.parent = parent;
        load();
    }
    static net.minecraft.network.chat.MutableComponent text(String key, Object... args) { return Component.translatable("create_sky_compat.settings." + key, args); }
    private static Component featureName(Feature feature) { return Component.translatable(SettingsCatalog.entry(feature).labelKey()); }
    private static Component modName(Group group) { return text("mod." + group.label); }
    private static Component category(Category category) { return text("category." + category.name().toLowerCase(Locale.ROOT)); }
    private static Component on(boolean value) { return Component.translatable(value ? "options.on" : "options.off"); }
    private void load() {
        try { if (draft == null) draft = StartupEdits.open(FMLPaths.CONFIGDIR.get()); else draft.reload(); feedback = Component.empty(); }
        catch (Exception failure) { feedback = text("load_failed").append(" ").append(Component.literal(String.valueOf(failure.getMessage()))); }
    }
    private static String section(Group group) { return "group/" + group.name(); }
    private String route() { return detail == null ? "document" : "detail/" + detail.key; }
    private void remember() {
        if (navigation != null) { navigationScroll = navigation.scroll(); navigationFocus = navigation.focusKey(); }
        if (content != null) {
            scroll.put(route(), content.scroll()); contentFocus = content.focusKey();
            if (detail == null) { documentPosition = content.bookmark(); documentFocus = contentFocus; }
        }
    }
    private void navigate(String anchor) {
        if (detail != null) { remember(); detail = null; contentFocus = null; rebuildWidgets(); }
        content.jump(anchor); contentFocus = null;
        updateSelection();
    }
    private void inspect(Feature feature) {
        remember(); detail = feature; contentFocus = null; rebuildWidgets();
    }
    private void updateSelection() {
        String anchor = detail == null ? content.activeSection() : section(SettingsCatalog.group(detail));
        links.forEach((id, link) -> link.selected(id.equals(anchor)));
    }
    private Button button(Component label, int width, Runnable action) {
        var button = Button.builder(label, b -> action.run()).bounds(0, 0, width, 20).build(); return button;
    }
    private Button valueButton(Component label, Component subject, int width, Runnable action) {
        return Button.builder(label, b -> action.run()).createNarration(supplier -> subject.copy().append(": ").append(supplier.get())).bounds(0, 0, width, 20).build();
    }
    @Override protected void init() {
        language = net.minecraft.locale.Language.getInstance(); resourceRevision = ClientResourceRevision.revision();
        int top = 34 + font.split(text("local"), Math.max(100, width - 24)).size() * font.lineHeight;
        notice = !feedback.getString().isEmpty() ? feedback : draft == null ? text("load_failed") : Component.empty();
        if (!notice.getString().isEmpty()) {
            noticeY = top; noticeWidth = Math.max(80, width - 128);
            int noticeHeight = Math.max(20, font.split(notice, noticeWidth).size() * font.lineHeight);
            var reload = button(text("reload"), 90, this::reload);
            reload.setPosition(width - 102, noticeY); addRenderableWidget(reload);
            top += noticeHeight + 8;
        }
        int paneHeight = Math.max(30, height - top - 38);
        int sidebar = Math.clamp(width / 4, 102, 170);
        navigation = new SettingsPanel(minecraft, 10, top, sidebar, paneHeight, true);
        content = new SettingsPanel(minecraft, sidebar + 18, top, Math.max(80, width - sidebar - 28), paneHeight, false);
        links.clear();
        for (Category category : Category.values()) {
            String categoryId = "category/" + category.name();
            var header = SettingsPanel.navigationButton(category(category), sidebar - 20, true, false, () -> navigate(categoryId));
            navigation.row(categoryId, category(category), Component.empty(), header);
            links.put(categoryId, header);
            for (Group group : SettingsCatalog.groups(category)) {
                String id = section(group);
                var entry = SettingsPanel.navigationButton(modName(group), sidebar - 20, false, false, () -> navigate(id));
                navigation.row(id, entry.getMessage(), Component.empty(), entry); links.put(id, entry);
            }
        }
        var status = SettingsPanel.navigationButton(text("status_files"), sidebar - 20, false, false, () -> navigate("files"));
        navigation.row("files", text("status_files"), Component.empty(), status); links.put("files", status);
        navigation.finish(navigationScroll); addRenderableWidget(navigation);
        if (detail != null) buildDetails(detail);
        else buildDocument();
        content.finish(scroll.getOrDefault(route(), 0f));
        if (detail == null) content.restore(documentPosition);
        addRenderableWidget(content);
        navigation.restoreFocus(navigationFocus);
        content.restoreFocus(contentFocus);
        if (contentFocus != null && content.focusKey() != null) setFocused(content);
        else if (navigationFocus != null && navigation.focusKey() != null) setFocused(navigation);
        updateSelection();
        var back = button(Component.translatable("gui.back"), 80, this::onClose); back.setPosition(10, height - 28); addRenderableWidget(back);
        save = button(text("save"), Math.min(110, Math.max(80, width / 4)), this::saveDraft);
        save.setPosition(width - save.getWidth() - 10, height - 28); save.active = draft != null && draft.dirty(); addRenderableWidget(save);
    }
    private void buildDocument() {
        for (Category category : Category.values()) {
            content.section("category/" + category.name(), category(category));
            for (Group group : SettingsCatalog.groups(category)) buildGroup(group);
        }
        buildStatus();
    }
    private void buildGroup(Group group) {
        content.section(section(group), modName(group));
        if (group == Group.JOURNAL) {
            if (OptionalIntegrations.enabled(Feature.QUESTLOG_INTERFACE) || OptionalIntegrations.enabled(Feature.QUESTLOG_NOTIFICATIONS)) buildPreferences();
            else content.row(text("preferences"), text("unavailable"));
        }
        var features = SettingsCatalog.features(group);
        if (!features.isEmpty()) content.heading(text("fixes"));
        for (Feature feature : features) addFeature(feature);
        var files = SettingsCatalog.FILES.stream().filter(file -> file.group() == group).toList();
        if (!files.isEmpty()) content.heading(text("tuning"));
        for (var file : files) addFile(file);
        if (files.stream().anyMatch(file -> !Files.isRegularFile(FMLPaths.CONFIGDIR.get().resolve(file.filename())))) folder();
    }
    private void buildPreferences() {
        content.heading(text("preferences"));
        preference("images", on(JournalPreferences.ITEM_IMAGES.get()), JournalPreferences::toggleImages);
        preference("symbols", on(JournalPreferences.CONDITION_SYMBOLS.get()), JournalPreferences::toggleSymbols);
        preference("notifications", JournalPreferences.NOTIFICATIONS.get().getTranslatedName(), JournalPreferences::cycleNotifications);
        content.row(text("automatic"), Component.empty());
    }
    private void preference(String key, Component value, Runnable action) {
        var control = valueButton(value, Component.translatable("create_sky_compat.journal." + key), Math.max(65, Math.min(135, (width - navigationWidth()) / 2)), () -> {
            remember();
            try { action.run(); feedback = Component.empty(); } catch (RuntimeException failure) { feedback = text("change_failed"); }
            rebuildWidgets();
        });
        content.row("preference/" + key, Component.translatable("create_sky_compat.journal." + key),
                Component.translatable("create_sky_compat.journal." + key + "_description"), control);
    }
    private int navigationWidth() { return Math.clamp(width / 4, 102, 170); }
    private void addFeature(Feature feature) {
        var inspect = valueButton(text("details"), featureName(feature), 52, () -> inspect(feature));
        Component summary = Component.translatable(SettingsCatalog.entry(feature).summaryKey());
        if (!OptionalIntegrations.enabled(feature)) summary = summary.copy().append("\n").append(text("running", state(feature)));
        if (draft != null && draft.saved().enabled(feature) != OptionalIntegrations.configured(feature)) summary = summary.copy().append("\n").append(text("saved_next")).append(": ").append(on(draft.saved().enabled(feature)));
        if (draft != null && draft.edited(feature)) summary = summary.copy().append("\n").append(text("modified"));
        var toggle = valueButton(draft == null ? Component.literal("?") : on(draft.value(feature)), featureName(feature), 45, () -> {
            draft.set(feature, !draft.value(feature)); remember(); rebuildWidgets();
        });
        toggle.active = draft != null;
        toggle.setTooltip(Tooltip.create(featureName(feature).copy().append(". ").append(text("restart"))));
        content.row("feature/" + feature.key, featureName(feature), summary, toggle, inspect);
    }
    private Component state(Feature feature) {
        var selection = OptionalIntegrations.selection();
        if (selection.enabled(feature)) return text("active");
        var reason = selection.reasons().get(feature);
        if (reason == null) return text("disabled");
        return switch (reason.kind()) {
            case DISABLED -> text("disabled");
            case WRONG_SIDE -> text("wrong_side");
            case PREREQUISITE -> text("prerequisite", featureName(FeatureSelection.prerequisite(feature)));
            case MISSING -> text("missing", String.join(", ", reason.details().stream().map(this::dependencyName).toList()));
            case EXTERNAL -> text("external", String.join(", ", reason.details()));
        };
    }
    private String dependencyName(String id) {
        var mod = net.neoforged.fml.loading.LoadingModList.get().getMods().stream().filter(m -> m.getModId().equals(id)).findFirst();
        return mod.map(net.neoforged.neoforgespi.language.IModInfo::getDisplayName).orElseGet(() -> {
            String key = switch (id) {
                case "createredstonelinkgui" -> "redstone_link_gui"; case "create_redstone_additions" -> "redstone_additions";
                case "sky_archipelago" -> "sky_archipelago"; case "skyvillages" -> "sky_villages";
                case "aeronautics_delivery_quests" -> "delivery"; case "analogaudio" -> "analog_audio";
                case "aeronautics_utility_objects" -> "transmission_linkage"; case "linearbearing" -> "linear_bearing";
                case "distanthorizons" -> "distant_horizons"; case "sabledynlights" -> "lighting"; default -> id;
            };
            Component name = text("mod." + key); return name.getString().equals("create_sky_compat.settings.mod." + key) ? id : name.getString();
        });
    }
    private void buildDetails(Feature feature) {
        content.heading(featureName(feature));
        content.row(Component.translatable(SettingsCatalog.entry(feature).summaryKey()), text("running", state(feature)));
        content.row(text("current_setting", on(OptionalIntegrations.configured(feature))), text(OptionalIntegrations.defaulted(feature) ? "omitted" : "explicit"));
        if (draft != null) {
            content.row(text("next_start", on(draft.saved().enabled(feature))), text(draft.saved().values().containsKey(feature) ? "explicit" : "omitted"));
            if (draft.edited(feature)) content.row(text("draft", on(draft.value(feature))), text("restart"));
        }
        content.row(text("default", on(feature.defaultEnabled)), Component.empty());
        content.row(text("scope", text(feature == Feature.BLUEMAP ? "dedicated" : FeatureSelection.clientOnly(feature) ? "client" : "both")), Component.empty());
        for (var entry : FeatureSelection.dependencies(feature).entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            String versions = entry.getValue().endsWith(".") ? entry.getValue() + "x" : entry.getValue();
            if (entry.getKey().equals("questlog")) versions += " / 3.5.3";
            content.row(text("requires", dependencyName(entry.getKey()) + " " + versions), Component.empty());
        }
        if (FeatureSelection.prerequisite(feature) != null) content.row(text("prerequisite", featureName(FeatureSelection.prerequisite(feature))), Component.empty());
        var copy = valueButton(text("copy_key"), featureName(feature), 78, () -> minecraft.keyboardHandler.setClipboard(feature.key));
        content.row(text("key", feature.key), Component.empty(), copy);
        addFile(SettingsCatalog.FILES.getFirst());
    }
    private void buildStatus() {
        content.section("files", text("status_files"));
        content.row(text("status_intro"), Component.empty());
        folder();
        for (var file : SettingsCatalog.FILES) {
            if (file.group() == null) addFile(file);
            else content.row("file-link/" + file.filename(), Component.literal(file.filename()),
                    text("section_title", category(file.group().category), modName(file.group())),
                    valueButton(text("view_section"), Component.literal(file.filename()), 95, () -> navigate(section(file.group()))));
        }
    }
    private void addFile(SettingsCatalog.FileEntry file) {
        var path = FMLPaths.CONFIGDIR.get().resolve(file.filename()); boolean exists = Files.isRegularFile(path);
        var open = valueButton(text("open_file"), Component.literal(file.filename()), 78, () -> Util.getPlatform().openFile(path.toFile())); open.active = exists;
        Component help = Component.translatable(file.summaryKey());
        if (!exists) help = help.copy().append("\n").append(text("not_created"));
        Component label = file.group() == null || detail != null ? Component.literal(file.filename()) : Component.translatable(file.labelKey());
        content.row("file/" + file.filename(), label, help, open);
        if (file.filename().equals("create_sky_compat-common.toml") && exists) {
            ModConfig config = ModConfigs.getConfigSet(ModConfig.Type.COMMON).stream().filter(c -> c.getModId().equals(container.getModId()) && c.getFileName().equals(file.filename())).findFirst().orElse(null);
            if (config != null) content.row(text("mod.sky_villages"), Component.empty(), valueButton(text("edit"), modName(Group.VILLAGES), 95, () -> {
                remember(); minecraft.setScreen(new ConfigurationScreen.ConfigurationSectionScreen(this, config.getType(), config, modName(Group.VILLAGES)) {
                    @Override public void onClose() {
                        if (!NeighborConfig.validSavedRange()) {
                            minecraft.setScreen(new net.minecraft.client.gui.screens.AlertScreen(() -> minecraft.setScreen(this), text("village_range"), Component.empty())); return;
                        }
                        try { super.onClose(); }
                        catch (RuntimeException failure) { minecraft.setScreen(new net.minecraft.client.gui.screens.AlertScreen(() -> minecraft.setScreen(this), text("save_failed"), Component.literal(String.valueOf(failure.getMessage())))); }
                    }
                });
            }));
        }
    }
    private void folder() { content.row(Component.empty(), Component.empty(), button(text("open_folder"), 115, () -> Util.getPlatform().openFile(FMLPaths.CONFIGDIR.get().toFile()))); }

    private void saveDraft() {
        if (draft == null || !draft.dirty()) return;
        boolean generation = draft.changes().keySet().stream().anyMatch(feature -> SettingsCatalog.group(feature).category == Category.WORLD);
        if (generation) {
            remember(); minecraft.setScreen(new ConfirmScreen(answer -> { minecraft.setScreen(this); if (answer) writeDraft(); }, text("generation_title"), text("generation_message")));
        } else writeDraft();
    }
    private void writeDraft() {
        remember();
        try {
            var proposed = new java.util.EnumMap<Feature, Boolean>(Feature.class); proposed.putAll(draft.saved().values()); proposed.putAll(draft.changes());
            var versions = new java.util.LinkedHashMap<String, String>();
            net.neoforged.fml.loading.LoadingModList.get().getMods().forEach(mod -> versions.put(mod.getModId(), mod.getVersion().toString()));
            FeatureSelection.select(new FeatureSettings(proposed), versions, true);
            draft.save(); feedback = text("saved");
        } catch (StartupEdits.ChangedOnDisk stale) { feedback = text("stale"); }
        catch (Exception failure) { feedback = text("save_failed").append(" ").append(Component.literal(String.valueOf(failure.getMessage()))); }
        rebuildWidgets();
    }
    private void reload() {
        remember(); Runnable reload = () -> { load(); rebuildWidgets(); };
        if (draft != null && draft.dirty()) minecraft.setScreen(new ConfirmScreen(answer -> { minecraft.setScreen(this); if (answer) reload.run(); }, text("reload_title"), text("reload_message"), text("reload"), text("keep_editing")));
        else reload.run();
    }
    @Override public void tick() {
        if (language != net.minecraft.locale.Language.getInstance() || resourceRevision != ClientResourceRevision.revision()) {
            remember(); rebuildWidgets();
        }
    }
    @Override public void resize(Minecraft client, int width, int height) { remember(); super.resize(client, width, height); }
    @Override public boolean mouseClicked(double x, double y, int button) {
        boolean handled = super.mouseClicked(x, y, button);
        // Native dispatch assigns focus after the callback, which may have replaced its widgets.
        var focused = getFocused();
        if (minecraft.screen == this && focused != null && !children().contains(focused)) {
            if (focused instanceof SettingsPanel previous) {
                String key = previous.focusKey();
                if (detail != null && !previous.isNavigation() && previous.activeSection() != null) documentFocus = key;
                var replacement = previous.isNavigation() ? navigation : content;
                setFocused(replacement); replacement.restoreFocus(key);
            } else if (focused instanceof net.minecraft.client.gui.components.AbstractWidget previous) {
                var replacement = children().stream().filter(child -> child instanceof net.minecraft.client.gui.components.AbstractWidget widget
                        && widget.getMessage().equals(previous.getMessage())).findFirst();
                if (replacement.isPresent()) setFocused(replacement.get());
                else {
                    setFocused(content); content.restoreFocus(contentFocus);
                    if (content.getFocused() == null) content.focusVisibleControl();
                }
            }
        }
        return handled;
    }
    @Override public void onClose() {
        if (detail != null) {
            remember(); detail = null; contentFocus = documentFocus; rebuildWidgets(); return;
        }
        remember();
        if (draft != null && draft.dirty()) minecraft.setScreen(new ConfirmScreen(answer -> { if (answer) { draft.discard(); minecraft.setScreen(parent); } else minecraft.setScreen(this); }, text("discard_title"), text("discard_message"), text("discard"), text("keep_editing")));
        else minecraft.setScreen(parent);
    }
    @Override public void render(GuiGraphics graphics, int mx, int my, float tick) {
        updateSelection();
        renderBackground(graphics, mx, my, tick); save.active = draft != null && draft.dirty(); super.render(graphics, mx, my, tick);
        graphics.drawString(font, title, 12, 10, 0xffffff);
        graphics.drawWordWrap(font, text("local"), 12, 24, Math.max(100, width - 24), 0xc0c0c0);
        if (!notice.getString().isEmpty()) graphics.drawWordWrap(font, notice, 12, noticeY + 2, noticeWidth, 0xffffff);
        int available = Math.max(0, width - save.getWidth() - 112);
        if (available > 90) {
            Component hint = text(draft != null && draft.dirty() ? "modified" : "restart");
            int textHeight = font.split(hint, available).size() * font.lineHeight;
            int textY = save.getY() + (save.getHeight() - textHeight) / 2 + 1;
            graphics.drawWordWrap(font, hint, 100, textY, available, 0xc0c0c0);
        }
    }
}
