package design.inherent.createskycompat.mixin.client;

import java.util.List;
import design.inherent.createskycompat.questlog.IconTextLayout;
import design.inherent.createskycompat.questlog.client.JournalButton;
import design.inherent.createskycompat.questlog.client.JournalIcon;
import design.inherent.createskycompat.questlog.client.JournalSettingsScreen;
import design.inherent.createskycompat.questlog.client.JournalPreferences;
import design.inherent.createskycompat.questlog.client.JournalRows;
import design.inherent.createskycompat.questlog.client.JournalScrollPane;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.infernalstudios.questlog.QuestlogClientEvents;
import org.infernalstudios.questlog.client.gui.QuestlogGuiSet;
import org.infernalstudios.questlog.client.gui.components.QuestlogButton;
import org.infernalstudios.questlog.client.gui.components.ScrollableComponent;
import org.infernalstudios.questlog.client.gui.components.scrollable.ScrollableText;
import org.infernalstudios.questlog.client.gui.screen.QuestDetails;
import org.infernalstudios.questlog.core.quests.Quest;
import org.infernalstudios.questlog.core.quests.rewards.ChoiceReward;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replace the coupled fixed layout, retaining native links, actions, sounds and quest lifecycle. */
@Mixin(value = QuestDetails.class, remap = false)
public abstract class QuestDetailsMixin extends Screen {
    @Shadow @Final public Quest quest;
    @Shadow private ScrollableComponent description;
    @Shadow private ScrollableComponent info;
    @Shadow private static boolean showDetails;
    @Shadow private QuestlogButton backButton;
    @Shadow private void handlePrimaryAction() {}
    @Shadow private boolean needsRead() { throw new AssertionError(); }

    @Unique private int createSky$x, createSky$y, createSky$leftWidth, createSky$rightWidth, createSky$height, createSky$contentY;
    @Unique private boolean createSky$compact, createSky$showInfo = true;
    @Unique private boolean createSky$initialized, createSky$detailsAllowed;
    @Unique private int createSky$view; // 0 description (narrow only), 1 conditions, 2 rewards
    @Unique private QuestlogGuiSet createSky$skin;
    @Unique private List<FormattedCharSequence> createSky$title = List.of();
    @Unique private final double[] createSky$scroll = new double[3];
    @Unique private boolean createSky$visitingSettings;
    @Unique private Object createSky$language;
    @Unique private long createSky$resources;

    protected QuestDetailsMixin(Component title) { super(title); }
    @Override public void resize(net.minecraft.client.Minecraft client, int width, int height) {
        createSky$saveScroll();
        super.resize(client, width, height);
    }
    @Unique private QuestDetails createSky$self() { return (QuestDetails) (Object) this; }
    @Unique private Component createSky$text(String key) { return JournalRows.text(key); }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true, require = 1)
    private void createSky$layout(CallbackInfo ci) {
        var display = quest.getDisplay();
        boolean returningFromSettings = createSky$visitingSettings;
        createSky$visitingSettings = false;
        createSky$detailsAllowed = createSky$allowsDetails();
        if (!createSky$initialized) {
            createSky$initialized = true;
            createSky$showInfo = createSky$detailsAllowed && (display.isDetailsOpenByDefault() || showDetails
                    || quest.isCompleted() && !quest.rewards.isEmpty());
        }
        if (!createSky$detailsAllowed) { createSky$showInfo = false; createSky$view = 0; }
        QuestlogClientEvents.mostRecentNotificationQuest = null;
        createSky$language = net.minecraft.locale.Language.getInstance();
        createSky$resources = design.inherent.createskycompat.questlog.client.JournalReloads.revision();
        int available = Math.max(96, width - 16);
        int preferredLeft = Math.max(275, display.getLeftPanelWidth());
        int preferredRight = Math.max(220, display.getRightPanelWidth());
        createSky$compact = available < preferredLeft + preferredRight + 6;
        createSky$leftWidth = Math.min(preferredLeft, available);
        createSky$rightWidth = createSky$compact ? createSky$leftWidth : preferredRight;
        createSky$height = Math.max(80, Math.min(Math.max(display.getPanelHeight(), height / 2), Math.min(320, height - 100)));
        int totalWidth = !createSky$compact && createSky$showInfo ? createSky$leftWidth + createSky$rightWidth + 6 : createSky$leftWidth;
        createSky$x = (width - totalWidth) / 2;
        createSky$y = Math.max(4, (height - createSky$height - 66) / 2);
        var original = display.getGuiSet();
        createSky$skin = new QuestlogGuiSet(original.backgroundLoc, original.rightPanelLoc, original.peripheralLoc,
                createSky$leftWidth, createSky$rightWidth, createSky$height);
        var icon = JournalPreferences.ITEM_IMAGES.get() ? JournalIcon.of(display.getIcon()) : null;
        createSky$title = font.split(display.getTitle(), Math.max(1, createSky$leftWidth - 40 - (icon == null ? 0 : icon.width() + 4)));
        int maxTitleLines = Math.max(1, Math.min(4, (createSky$height / 3 - 16) / font.lineHeight));
        boolean overflowTitle = createSky$title.size() > maxTitleLines;
        if (overflowTitle) createSky$title = createSky$title.subList(0, maxTitleLines);
        int heading = 16 + Math.max(icon == null ? 0 : icon.height(), createSky$title.size() * font.lineHeight) + 16;
        createSky$contentY = Math.min(heading, createSky$height - 25);
        if (!createSky$compact && createSky$view == 0 && createSky$detailsAllowed) createSky$view = 1;
        showDetails = createSky$showInfo && (!createSky$compact || createSky$view != 0);
        int contentHeight = Math.max(1, createSky$height - createSky$contentY - 16);
        description = null; info = null;
        if (!createSky$compact || createSky$view == 0 || !createSky$showInfo) {
            description = new JournalScrollPane(createSky$x + 18, createSky$y + createSky$contentY,
                    createSky$leftWidth - 36, contentHeight,
                    new ScrollableText(font, overflowTitle ? display.getTitle().copy().append("\n\n").append(display.getDescription(quest))
                            : display.getDescription(quest), createSky$self().getPalette().textColor()),
                    createSky$skin, createSky$text("description"));
            description.setScrollAmount(createSky$scroll[0]); addWidget(description);
        }
        if (showDetails) {
            int x = createSky$compact ? createSky$x : createSky$x + createSky$leftWidth + 6;
            info = new JournalScrollPane(x + 18, createSky$y + createSky$contentY,
                    createSky$rightWidth - 36, contentHeight, new JournalRows(createSky$self(), createSky$view == 2),
                    createSky$skin, createSky$text(createSky$view == 2 ? "rewards" : "conditions"));
            info.setScrollAmount(createSky$scroll[Math.max(1, createSky$view)]); addWidget(info);
        }
        int tabY = createSky$y + createSky$height + 2;
        int tabX = createSky$x;
        if (createSky$compact && createSky$detailsAllowed) tabX = createSky$tab(tabX, tabY, "description", 0);
        if (createSky$detailsAllowed) {
            tabX = createSky$tab(tabX, tabY, "conditions", 1);
            tabX = createSky$tab(tabX, tabY, "rewards", 2);
        }
        if (!createSky$compact && createSky$detailsAllowed && !display.isDetailsButtonDisabled()) createSky$button(tabX, tabY, createSky$text(createSky$showInfo ? "hide_details" : "show_details"), () -> {
            createSky$saveScroll(); createSky$showInfo = !createSky$showInfo; rebuildWidgets();
        });
        int actionY = tabY + 22;
        createSky$button(createSky$x, actionY, display.getBackButtonText(), () -> minecraft.setScreen(createSky$self().getPreviousScreen()));
        backButton = createSky$button(createSky$x + totalWidth - 100, actionY, createSky$text("claim"), this::handlePrimaryAction);
        var settings = new JournalButton(createSky$x + totalWidth - 88, Math.max(0, createSky$y - 17), 88,
                createSky$text("settings"), createSky$skin, () -> {
                    createSky$saveScroll(); createSky$visitingSettings = true;
                    minecraft.setScreen(new JournalSettingsScreen(createSky$self(), createSky$skin));
                });
        addRenderableWidget(settings);
        if (returningFromSettings) setFocused(settings);
        createSky$actions();
        ci.cancel();
    }

    @Unique private boolean createSky$allowsDetails() {
        if (quest.objectives.isEmpty() && quest.rewards.isEmpty()) return false;
        return !quest.getDisplay().isDetailsButtonDisabled() || quest.getDisplay().isDetailsOpenByDefault()
                || quest.isCompleted() && !quest.rewards.isEmpty();
    }

    @Unique private int createSky$tab(int x, int y, String label, int view) {
        var button = createSky$button(x, y, createSky$text(label), () -> {
            createSky$saveScroll(); createSky$view = view; createSky$showInfo = true; rebuildWidgets();
        });
        button.active = !createSky$showInfo || createSky$view != view;
        if (!button.active) button.setMessage(button.getMessage().copy().withStyle(net.minecraft.ChatFormatting.UNDERLINE));
        return x + button.getExpectedWidth() + 4;
    }
    @Unique private QuestlogButton createSky$button(int x, int y, Component label, org.infernalstudios.questlog.util.Callable action) {
        var palette = createSky$self().getPalette();
        var button = new QuestlogButton(x, y, palette.textColor(), palette.hoveredTextColor(), label, action, createSky$skin);
        return addRenderableWidget(button);
    }
    @Unique private void createSky$saveScroll() {
        if (description != null) createSky$scroll[0] = description.getScrollAmount();
        if (info != null) createSky$scroll[Math.max(1, createSky$view)] = info.getScrollAmount();
    }
    @Unique private void createSky$actions() {
        if (backButton == null) return;
        boolean pending = quest.isCompleted() && !quest.isRewarded();
        boolean reset = quest.isCompleted() && quest.isRewarded() && quest.isRepeatable();
        boolean read = needsRead();
        backButton.visible = pending || reset || read;
        backButton.active = pending ? quest.isTriggered() && quest.rewards.stream().allMatch(r -> r.hasRewarded()
                || !(r instanceof ChoiceReward choice) || choice.canClaim()) : reset || read;
        backButton.setMessage(pending ? backButton.active ? quest.getDisplay().getCollectButtonText()
                        : Component.translatable("questlog.reward.make_choices")
                : Component.translatable(read ? "questlog.button.read" : "questlog.reward.reset"));
    }
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 1)
    private void createSky$tick(CallbackInfo ci) {
        if (createSky$detailsAllowed != createSky$allowsDetails()) {
            createSky$saveScroll(); createSky$showInfo = true; rebuildWidgets();
        }
        if (createSky$language != net.minecraft.locale.Language.getInstance()
                || createSky$resources != design.inherent.createskycompat.questlog.client.JournalReloads.revision()) {
            createSky$saveScroll(); rebuildWidgets();
        }
        createSky$actions(); ci.cancel();
    }
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true, require = 1)
    private void createSky$background(GuiGraphics g, int mx, int my, float tick, CallbackInfo ci) {
        super.renderBackground(g, mx, my, tick);
        createSky$skin.detailBackgroundLeft.blit(g, createSky$x, createSky$y);
        if (showDetails && !createSky$compact) createSky$skin.detailBackgroundRight.blit(g, createSky$x + createSky$leftWidth + 6, createSky$y);
        var d = quest.getDisplay();
        if (d.getOverlayTexture() != null) g.blit(d.getOverlayTexture(), createSky$x + d.getOverlayXOffset(),
                createSky$y + d.getOverlayYOffset(), 0, 0, d.getOverlayWidth(), d.getOverlayHeight(), d.getOverlayWidth(), d.getOverlayHeight());
        ci.cancel();
    }
    @Inject(method = "renderTitle", at = @At("HEAD"), cancellable = true, require = 1)
    private void createSky$title(GuiGraphics g, CallbackInfo ci) {
        int top = createSky$y + 14;
        int y = top;
        var icon = JournalPreferences.ITEM_IMAGES.get() ? JournalIcon.of(quest.getDisplay().getIcon()) : null;
        int textX = createSky$x + 20;
        var alignment = IconTextLayout.centered(icon == null ? 0 : icon.height(), createSky$title.size() * font.lineHeight);
        if (icon != null) { icon.blit(g, textX, top + alignment.iconY()); textX += icon.width() + 4; }
        y = top + alignment.textY();
        for (var line : createSky$title) {
            g.drawString(font, line, textX, y, createSky$self().getPalette().titleColor(), false); y += font.lineHeight;
        }
        createSky$skin.smallHR.blit(g, createSky$x + (createSky$leftWidth - 252) / 2, createSky$y + createSky$contentY - 12);
        if (showDetails && !createSky$compact)
            createSky$skin.panelHR.blit(g, createSky$x + createSky$leftWidth + 6 + (createSky$rightWidth - 140) / 2,
                    createSky$y + createSky$contentY - 12);
        if (showDetails && !createSky$compact)
            g.drawWordWrap(font, createSky$text(createSky$view == 2 ? "rewards" : "conditions"),
                    createSky$x + createSky$leftWidth + 24, createSky$y + 14,
                    createSky$rightWidth - 36, createSky$self().getPalette().titleColor());
        ci.cancel();
    }
    @Inject(method = "renderInfo", at = @At("HEAD"), cancellable = true, require = 1)
    private void createSky$info(GuiGraphics g, int mx, int my, float tick, CallbackInfo ci) {
        if (info != null) info.render(g, mx, my, tick); ci.cancel();
    }
    @Redirect(method = "removed", at = @At(value = "INVOKE", target = "Lorg/infernalstudios/questlog/core/quests/Quest;isRepeatable()Z"), require = 1)
    private boolean createSky$keepCycleWhileInSettings(Quest closing) {
        return !createSky$visitingSettings && closing.isRepeatable();
    }
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;II)V"), require = 1)
    private void createSky$tooltip(GuiGraphics g, net.minecraft.client.gui.Font font, Component text, int x, int y) {
        g.renderTooltip(font, font.split(text, Math.max(40, Math.min(300, width - 32))), x, y);
    }
}
