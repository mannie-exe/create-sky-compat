package design.inherent.skyweavecompat.mixin.client;

import java.util.List;
import design.inherent.skyweavecompat.questlog.IconTextLayout;
import design.inherent.skyweavecompat.questlog.client.JournalButton;
import design.inherent.skyweavecompat.questlog.client.JournalIcon;
import design.inherent.skyweavecompat.questlog.client.JournalSettingsScreen;
import design.inherent.skyweavecompat.questlog.client.JournalPreferences;
import design.inherent.skyweavecompat.questlog.client.JournalRows;
import design.inherent.skyweavecompat.questlog.client.JournalScrollPane;
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

    @Unique private int skyweave$x, skyweave$y, skyweave$leftWidth, skyweave$rightWidth, skyweave$height, skyweave$contentY;
    @Unique private boolean skyweave$compact, skyweave$showInfo = true;
    @Unique private boolean skyweave$initialized, skyweave$detailsAllowed;
    @Unique private int skyweave$view; // 0 description (narrow only), 1 conditions, 2 rewards
    @Unique private QuestlogGuiSet skyweave$skin;
    @Unique private List<FormattedCharSequence> skyweave$title = List.of();
    @Unique private final double[] skyweave$scroll = new double[3];
    @Unique private boolean skyweave$visitingSettings;
    @Unique private Object skyweave$language;
    @Unique private long skyweave$resources;

    protected QuestDetailsMixin(Component title) { super(title); }
    @Override public void resize(net.minecraft.client.Minecraft client, int width, int height) {
        skyweave$saveScroll();
        super.resize(client, width, height);
    }
    @Unique private QuestDetails skyweave$self() { return (QuestDetails) (Object) this; }
    @Unique private Component skyweave$text(String key) { return JournalRows.text(key); }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true, require = 1)
    private void skyweave$layout(CallbackInfo ci) {
        var display = quest.getDisplay();
        boolean returningFromSettings = skyweave$visitingSettings;
        skyweave$visitingSettings = false;
        skyweave$detailsAllowed = skyweave$allowsDetails();
        if (!skyweave$initialized) {
            skyweave$initialized = true;
            skyweave$showInfo = skyweave$detailsAllowed && (display.isDetailsOpenByDefault() || showDetails
                    || quest.isCompleted() && !quest.rewards.isEmpty());
        }
        if (!skyweave$detailsAllowed) { skyweave$showInfo = false; skyweave$view = 0; }
        QuestlogClientEvents.mostRecentNotificationQuest = null;
        skyweave$language = net.minecraft.locale.Language.getInstance();
        skyweave$resources = design.inherent.skyweavecompat.client.ClientResourceRevision.revision();
        int available = Math.max(96, width - 16);
        int preferredLeft = Math.max(275, display.getLeftPanelWidth());
        int preferredRight = Math.max(220, display.getRightPanelWidth());
        skyweave$compact = available < preferredLeft + preferredRight + 6;
        skyweave$leftWidth = Math.min(preferredLeft, available);
        skyweave$rightWidth = skyweave$compact ? skyweave$leftWidth : preferredRight;
        skyweave$height = Math.max(80, Math.min(Math.max(display.getPanelHeight(), height / 2), Math.min(320, height - 100)));
        int totalWidth = !skyweave$compact && skyweave$showInfo ? skyweave$leftWidth + skyweave$rightWidth + 6 : skyweave$leftWidth;
        skyweave$x = (width - totalWidth) / 2;
        skyweave$y = Math.max(4, (height - skyweave$height - 66) / 2);
        var original = display.getGuiSet();
        skyweave$skin = new QuestlogGuiSet(original.backgroundLoc, original.rightPanelLoc, original.peripheralLoc,
                skyweave$leftWidth, skyweave$rightWidth, skyweave$height);
        var icon = JournalPreferences.ITEM_IMAGES.get() ? JournalIcon.of(display.getIcon()) : null;
        skyweave$title = font.split(display.getTitle(), Math.max(1, skyweave$leftWidth - 40 - (icon == null ? 0 : icon.width() + 4)));
        int maxTitleLines = Math.max(1, Math.min(4, (skyweave$height / 3 - 16) / font.lineHeight));
        boolean overflowTitle = skyweave$title.size() > maxTitleLines;
        if (overflowTitle) skyweave$title = skyweave$title.subList(0, maxTitleLines);
        int heading = 16 + Math.max(icon == null ? 0 : icon.height(), skyweave$title.size() * font.lineHeight) + 16;
        skyweave$contentY = Math.min(heading, skyweave$height - 25);
        if (!skyweave$compact && skyweave$view == 0 && skyweave$detailsAllowed) skyweave$view = 1;
        showDetails = skyweave$showInfo && (!skyweave$compact || skyweave$view != 0);
        int contentHeight = Math.max(1, skyweave$height - skyweave$contentY - 16);
        description = null; info = null;
        if (!skyweave$compact || skyweave$view == 0 || !skyweave$showInfo) {
            description = new JournalScrollPane(skyweave$x + 18, skyweave$y + skyweave$contentY,
                    skyweave$leftWidth - 36, contentHeight,
                    new ScrollableText(font, overflowTitle ? display.getTitle().copy().append("\n\n").append(display.getDescription(quest))
                            : display.getDescription(quest), skyweave$self().getPalette().textColor()),
                    skyweave$skin, skyweave$text("description"));
            description.setScrollAmount(skyweave$scroll[0]); addWidget(description);
        }
        if (showDetails) {
            int x = skyweave$compact ? skyweave$x : skyweave$x + skyweave$leftWidth + 6;
            info = new JournalScrollPane(x + 18, skyweave$y + skyweave$contentY,
                    skyweave$rightWidth - 36, contentHeight, new JournalRows(skyweave$self(), skyweave$view == 2),
                    skyweave$skin, skyweave$text(skyweave$view == 2 ? "rewards" : "conditions"));
            info.setScrollAmount(skyweave$scroll[Math.max(1, skyweave$view)]); addWidget(info);
        }
        int tabY = skyweave$y + skyweave$height + 2;
        int tabX = skyweave$x;
        if (skyweave$compact && skyweave$detailsAllowed) tabX = skyweave$tab(tabX, tabY, "description", 0);
        if (skyweave$detailsAllowed) {
            tabX = skyweave$tab(tabX, tabY, "conditions", 1);
            tabX = skyweave$tab(tabX, tabY, "rewards", 2);
        }
        if (!skyweave$compact && skyweave$detailsAllowed && !display.isDetailsButtonDisabled()) skyweave$button(tabX, tabY, skyweave$text(skyweave$showInfo ? "hide_details" : "show_details"), () -> {
            skyweave$saveScroll(); skyweave$showInfo = !skyweave$showInfo; rebuildWidgets();
        });
        int actionY = tabY + 22;
        skyweave$button(skyweave$x, actionY, display.getBackButtonText(), () -> minecraft.setScreen(skyweave$self().getPreviousScreen()));
        backButton = skyweave$button(skyweave$x + totalWidth - 100, actionY, skyweave$text("claim"), this::handlePrimaryAction);
        var settings = new JournalButton(skyweave$x + totalWidth - 88, Math.max(0, skyweave$y - 17), 88,
                skyweave$text("settings"), skyweave$skin, () -> {
                    skyweave$saveScroll(); skyweave$visitingSettings = true;
                    minecraft.setScreen(new JournalSettingsScreen(skyweave$self(), skyweave$skin));
                });
        addRenderableWidget(settings);
        if (returningFromSettings) setFocused(settings);
        skyweave$actions();
        ci.cancel();
    }

    @Unique private boolean skyweave$allowsDetails() {
        if (quest.objectives.isEmpty() && quest.rewards.isEmpty()) return false;
        return !quest.getDisplay().isDetailsButtonDisabled() || quest.getDisplay().isDetailsOpenByDefault()
                || quest.isCompleted() && !quest.rewards.isEmpty();
    }

    @Unique private int skyweave$tab(int x, int y, String label, int view) {
        var button = skyweave$button(x, y, skyweave$text(label), () -> {
            skyweave$saveScroll(); skyweave$view = view; skyweave$showInfo = true; rebuildWidgets();
        });
        button.active = !skyweave$showInfo || skyweave$view != view;
        if (!button.active) button.setMessage(button.getMessage().copy().withStyle(net.minecraft.ChatFormatting.UNDERLINE));
        return x + button.getExpectedWidth() + 4;
    }
    @Unique private QuestlogButton skyweave$button(int x, int y, Component label, org.infernalstudios.questlog.util.Callable action) {
        var palette = skyweave$self().getPalette();
        var button = new QuestlogButton(x, y, palette.textColor(), palette.hoveredTextColor(), label, action, skyweave$skin);
        return addRenderableWidget(button);
    }
    @Unique private void skyweave$saveScroll() {
        if (description != null) skyweave$scroll[0] = description.getScrollAmount();
        if (info != null) skyweave$scroll[Math.max(1, skyweave$view)] = info.getScrollAmount();
    }
    @Unique private void skyweave$actions() {
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
    private void skyweave$tick(CallbackInfo ci) {
        if (skyweave$detailsAllowed != skyweave$allowsDetails()) {
            skyweave$saveScroll(); skyweave$showInfo = true; rebuildWidgets();
        }
        if (skyweave$language != net.minecraft.locale.Language.getInstance()
                || skyweave$resources != design.inherent.skyweavecompat.client.ClientResourceRevision.revision()) {
            skyweave$saveScroll(); rebuildWidgets();
        }
        skyweave$actions(); ci.cancel();
    }
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true, require = 1)
    private void skyweave$background(GuiGraphics g, int mx, int my, float tick, CallbackInfo ci) {
        super.renderBackground(g, mx, my, tick);
        skyweave$skin.detailBackgroundLeft.blit(g, skyweave$x, skyweave$y);
        if (showDetails && !skyweave$compact) skyweave$skin.detailBackgroundRight.blit(g, skyweave$x + skyweave$leftWidth + 6, skyweave$y);
        var d = quest.getDisplay();
        if (d.getOverlayTexture() != null) g.blit(d.getOverlayTexture(), skyweave$x + d.getOverlayXOffset(),
                skyweave$y + d.getOverlayYOffset(), 0, 0, d.getOverlayWidth(), d.getOverlayHeight(), d.getOverlayWidth(), d.getOverlayHeight());
        ci.cancel();
    }
    @Inject(method = "renderTitle", at = @At("HEAD"), cancellable = true, require = 1)
    private void skyweave$title(GuiGraphics g, CallbackInfo ci) {
        int top = skyweave$y + 14;
        int y = top;
        var icon = JournalPreferences.ITEM_IMAGES.get() ? JournalIcon.of(quest.getDisplay().getIcon()) : null;
        int textX = skyweave$x + 20;
        var alignment = IconTextLayout.centered(icon == null ? 0 : icon.height(), skyweave$title.size() * font.lineHeight);
        if (icon != null) { icon.blit(g, textX, top + alignment.iconY()); textX += icon.width() + 4; }
        y = top + alignment.textY();
        for (var line : skyweave$title) {
            g.drawString(font, line, textX, y, skyweave$self().getPalette().titleColor(), false); y += font.lineHeight;
        }
        skyweave$skin.smallHR.blit(g, skyweave$x + (skyweave$leftWidth - 252) / 2, skyweave$y + skyweave$contentY - 12);
        if (showDetails && !skyweave$compact)
            skyweave$skin.panelHR.blit(g, skyweave$x + skyweave$leftWidth + 6 + (skyweave$rightWidth - 140) / 2,
                    skyweave$y + skyweave$contentY - 12);
        if (showDetails && !skyweave$compact)
            g.drawWordWrap(font, skyweave$text(skyweave$view == 2 ? "rewards" : "conditions"),
                    skyweave$x + skyweave$leftWidth + 24, skyweave$y + 14,
                    skyweave$rightWidth - 36, skyweave$self().getPalette().titleColor());
        ci.cancel();
    }
    @Inject(method = "renderInfo", at = @At("HEAD"), cancellable = true, require = 1)
    private void skyweave$info(GuiGraphics g, int mx, int my, float tick, CallbackInfo ci) {
        if (info != null) info.render(g, mx, my, tick); ci.cancel();
    }
    @Redirect(method = "removed", at = @At(value = "INVOKE", target = "Lorg/infernalstudios/questlog/core/quests/Quest;isRepeatable()Z"), require = 1)
    private boolean skyweave$keepCycleWhileInSettings(Quest closing) {
        return !skyweave$visitingSettings && closing.isRepeatable();
    }
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;II)V"), require = 1)
    private void skyweave$tooltip(GuiGraphics g, net.minecraft.client.gui.Font font, Component text, int x, int y) {
        g.renderTooltip(font, font.split(text, Math.max(40, Math.min(300, width - 32))), x, y);
    }
}
