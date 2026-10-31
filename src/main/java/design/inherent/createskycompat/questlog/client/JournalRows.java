package design.inherent.createskycompat.questlog.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.infernalstudios.questlog.client.gui.components.InfoEntry;
import org.infernalstudios.questlog.client.gui.components.ScrollableComponent;
import org.infernalstudios.questlog.client.gui.components.scrollable.Scrollable;
import org.infernalstudios.questlog.client.gui.screen.QuestDetails;
import org.infernalstudios.questlog.core.quests.display.RewardDisplayData;
import org.infernalstudios.questlog.core.quests.objectives.Objective;
import org.infernalstudios.questlog.core.quests.objectives.item.ItemObtainObjective;
import org.infernalstudios.questlog.core.quests.objectives.block.BlockInteractObjective;
import org.infernalstudios.questlog.core.quests.objectives.misc.AdvancementObjective;
import org.infernalstudios.questlog.core.quests.objectives.logic.OrObjective;
import org.lwjgl.glfw.GLFW;

/** Measured native display data, shared by painting, pointer selection and keyboard focus. */
public final class JournalRows implements Scrollable, GuiEventListener, NarratableEntry {
    private final QuestDetails screen;
    private final boolean rewards;
    private final List<Row> rows = new ArrayList<>();
    private ScrollableComponent pane;
    private boolean focused;
    private int selected = -1, extent;
    private Object language;

    public JournalRows(QuestDetails screen, boolean rewards) {
        this.screen = screen; this.rewards = rewards;
        if (rewards) for (var data : screen.getDisplay().getRewardDisplayData()) rows.add(new Row(null, data, data.getIndentLevel()));
        else for (var objective : screen.quest.objectives) add(objective, 0);
    }
    private void add(Objective objective, int indent) {
        if (objective.isHidden()) return;
        if (objective.getDisplay() != null) rows.add(new Row(objective, null, indent));
        for (var child : objective.getChildren()) add(child, indent + 1);
    }
    @Override public void setScrollableComponent(ScrollableComponent pane) { this.pane = pane; }
    @Override public int getHeight() { measure(); return extent; }

    private void measure() {
        if (pane == null) return;
        boolean languageChanged = language != Language.getInstance();
        language = Language.getInstance();
        var font = Minecraft.getInstance().font;
        int available = Math.max(1, pane.width - pane.getScrollbarWidth() - 2);
        int top = 0;
        for (var row : rows) {
            Component name = row.name(), status = row.status();
            int iconWidth = JournalPreferences.ITEM_IMAGES.get() && row.icon != null ? row.icon.width() + 4 : 0;
            int symbolWidth = JournalPreferences.CONDITION_SYMBOLS.get() && row.kind() != null ? 10 : 0;
            int indent = Math.min(row.indent * 10, Math.max(0, available - 60));
            int textX = indent + iconWidth;
            int wrap = Math.max(1, available - textX);
            if (languageChanged || wrap != row.wrap || symbolWidth != row.symbolWidth
                    || !name.equals(row.name) || !status.equals(row.status)) {
                row.name = name.copy(); row.status = status.copy(); row.wrap = wrap; row.symbolWidth = symbolWidth;
                row.nameLines = font.split(name, wrap);
                row.statusLines = font.split(status, Math.max(1, wrap - symbolWidth));
            }
            row.textX = textX; row.iconX = indent; row.top = top;
            row.height = Math.max(iconWidth > 0 ? row.icon.height() : 0,
                    (row.nameLines.size() + row.statusLines.size()) * font.lineHeight + 3) + 9;
            top += row.height;
        }
        extent = rows.isEmpty() ? font.lineHeight * 2 : top;
    }

    @Override public void render(GuiGraphics graphics, int mx, int my, float partialTick) {
        measure();
        var font = Minecraft.getInstance().font;
        int x = (int) pane.getXOffset(), y = (int) pane.getYOffset();
        if (rows.isEmpty()) {
            graphics.drawWordWrap(font, text(rewards ? "no_rewards" : "no_conditions"), x, y,
                    Math.max(1, pane.width - pane.getScrollbarWidth()), screen.getPalette().textColor());
            return;
        }
        for (int index = 0; index < rows.size(); index++) {
            Row row = rows.get(index);
            if (row.top + row.height < pane.getScrollAmount() || row.top > pane.getScrollAmount() + pane.height) continue;
            int top = y + row.top;
            if (focused && index == selected)
                graphics.hLine(x + row.textX, x + pane.width - pane.getScrollbarWidth() - 2,
                        top + row.height - 3, screen.getPalette().progressTextColor() | 0xff000000);
            if (JournalPreferences.ITEM_IMAGES.get() && row.icon != null) row.icon.blit(graphics, x + row.iconX, top + 2);
            int lineY = top;
            for (var line : row.nameLines) {
                graphics.drawString(font, line, x + row.textX, lineY, screen.getPalette().textColor(), false);
                lineY += font.lineHeight;
            }
            lineY += 3;
            int color = row.completed() ? screen.getPalette().completedTextColor() : screen.getPalette().progressTextColor();
            if (row.symbolWidth > 0) symbol(graphics, row.kind(), x + row.textX, lineY + 1, color | 0xff000000);
            for (var line : row.statusLines) {
                graphics.drawString(font, line, x + row.textX + row.symbolWidth, lineY, color, false);
                lineY += font.lineHeight;
            }
            if (mx >= 0 && mx < pane.width - pane.getScrollbarWidth() && my >= row.top && my < row.top + row.height)
                screen.pendingTooltip = row.name.copy().append(" · ").append(row.status);
        }
    }

    private static void symbol(GuiGraphics g, String kind, int x, int y, int color) {
        if (kind.equals("either")) {
            g.vLine(x + 1, y, y + 7, color); g.hLine(x + 1, x + 6, y + 1, color); g.hLine(x + 1, x + 6, y + 6, color);
        } else if (kind.equals("interact")) {
            g.hLine(x, x + 6, y + 3, color); g.fill(x + 4, y + 1, x + 6, y + 6, color);
        } else {
            g.hLine(x, x + 6, y, color); g.hLine(x, x + 6, y + 6, color);
            g.vLine(x, y, y + 6, color); g.vLine(x + 6, y, y + 6, color);
            if (kind.equals("advancement")) g.fill(x + 2, y + 2, x + 5, y + 5, color);
        }
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        measure();
        for (int i = 0; i < rows.size(); i++) if (y >= rows.get(i).top && y < rows.get(i).top + rows.get(i).height) {
            selected = i; activate(rows.get(i)); return true;
        }
        return false;
    }
    private void activate(Row row) {
        if (rewards && screen.quest.isCompleted() && !screen.quest.isRewarded() && row.reward != null)
            new InfoEntry(screen, row.reward, 0, 0, screen.getDisplay()).handleChoiceClick();
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (!focused || rows.isEmpty()) return false;
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE) {
            if (selected >= 0) activate(rows.get(selected)); return true;
        }
        if (key != GLFW.GLFW_KEY_UP && key != GLFW.GLFW_KEY_DOWN) return false;
        selected = Math.clamp(selected + (key == GLFW.GLFW_KEY_UP ? -1 : 1), 0, rows.size() - 1);
        measure(); Row row = rows.get(selected);
        if (row.top < pane.getScrollAmount()) pane.setScrollAmount(row.top);
        else if (row.top + row.height > pane.getScrollAmount() + pane.height)
            pane.setScrollAmount(row.top + row.height - pane.height);
        return true;
    }
    @Override public boolean isFocused() { return focused; }
    @Override public void setFocused(boolean focused) { this.focused = focused; }
    @Override public NarrationPriority narrationPriority() { return focused ? NarrationPriority.FOCUSED : NarrationPriority.NONE; }
    @Override public void updateNarration(NarrationElementOutput output) {
        if (selected >= 0) output.add(NarratedElementType.TITLE, rows.get(selected).name().copy().append(". ").append(rows.get(selected).status()));
    }
    public static Component text(String key) { return Component.translatable("create_sky_compat.journal." + key); }

    private final class Row {
        final Objective objective;
        final RewardDisplayData reward;
        final JournalIcon icon;
        final int indent;
        Component name, status;
        List<FormattedCharSequence> nameLines = List.of(), statusLines = List.of();
        int wrap = -1, top, height, textX, iconX, symbolWidth;
        Row(Objective objective, RewardDisplayData reward, int indent) {
            this.objective = objective; this.reward = reward; this.indent = indent;
            icon = JournalIcon.of(reward != null ? reward.getIcon() : objective.getDisplay().getIcon());
        }
        Component name() { return reward != null ? reward.getName() : objective.getDisplay().getName(); }
        boolean completed() { return reward != null ? reward.hasRewarded() : objective.isCompleted(); }
        String kind() {
            if (objective instanceof ItemObtainObjective) return "inventory";
            if (objective instanceof BlockInteractObjective) return "interact";
            if (objective instanceof AdvancementObjective) return "advancement";
            if (objective instanceof OrObjective) return "either";
            return null;
        }
        Component status() {
            if (reward != null) {
                if (reward.getReward() != null && reward.getReward().getContainer() != null)
                    return text(reward.getReward().getContainer().hasRewarded()
                            ? reward.getReward().isSelected() ? "selected_claimed" : "not_selected"
                            : screen.quest.isCompleted() ? "select_reward" : "preview");
                return reward.hasRewarded() ? screen.getDisplay().getCollectedText()
                        : !screen.quest.isCompleted() ? text("after_completion") : screen.getDisplay().getUncollectedText();
            }
            Component progress = objective.getDisplay().getProgress();
            return kind() == null ? progress : text(kind()).copy().append(" · ").append(progress);
        }
    }
}
