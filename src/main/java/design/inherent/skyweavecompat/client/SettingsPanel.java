package design.inherent.skyweavecompat.client;

import com.mojang.blaze3d.vertex.Tesselator;
import java.util.ArrayList;
import java.util.List;
import design.inherent.skyweavecompat.configuration.SectionNavigation;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.widget.ScrollPanel;

/** Measured configuration rows; native widgets retain their own keyboard and narration behavior. */
final class SettingsPanel extends ScrollPanel {
    static final int ACTIVE_TEXT = ChatFormatting.YELLOW.getColor();
    static final int TITLE_TEXT = ChatFormatting.WHITE.getColor();
    static final int DESCRIPTION_TEXT = ChatFormatting.GRAY.getColor();
    static NavigationButton navigationButton(Component label, int width, boolean category, boolean selected, Runnable action) {
        return new NavigationButton(label, width, category, selected, action);
    }
    static NavigationButton subsectionButton(Component label, int width, Runnable action) {
        var button = new NavigationButton(label, width, false, false, action); button.indent = 24; return button;
    }
    static NavigationButton sectionButton(Component label, int width, Runnable action) {
        var button = new NavigationButton(label, width, true, false, action); button.indent = 18; return button;
    }
    static final class NavigationButton extends net.minecraft.client.gui.components.Button {
        private final boolean category;
        private boolean selected;
        private int indent;
        NavigationButton(Component label, int width, boolean category, boolean selected, Runnable action) {
            super(0, 0, width, 20, label, b -> action.run(), DEFAULT_NARRATION); this.category = category; this.selected = selected; indent = category ? 4 : 12;
        }
        int labelWidth() { return getWidth() - indent - 4; }
        void selected(boolean value) { selected = value; }
        @Override protected void renderWidget(GuiGraphics graphics, int mx, int my, float tick) {
            if (selected) { graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xff888888); graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0xff3d3d3d); }
            else if (isHoveredOrFocused()) graphics.fill(getX(), getY(), getX() + width, getY() + height, 0x88555555);
            renderString(graphics, Minecraft.getInstance().font, selected ? ACTIVE_TEXT : category ? DESCRIPTION_TEXT : TITLE_TEXT);
        }
        @Override public void renderString(GuiGraphics graphics, net.minecraft.client.gui.Font font, int color) {
            var lines = font.split(getMessage(), labelWidth());
            int y = getY() + (getHeight() - lines.size() * font.lineHeight) / 2;
            for (var line : lines) { graphics.drawString(font, line, getX() + indent, y, color); y += font.lineHeight; }
        }
        @Override public void updateWidgetNarration(NarrationElementOutput output) {
            super.updateWidgetNarration(output);
            if (selected) output.add(NarratedElementType.POSITION, Component.translatable("skyweave_compat.settings.selected_section"));
        }
    }
    private record Row(String key, Component label, Component summary, List<AbstractWidget> controls, boolean heading, String anchor, boolean field, int boundary) {}
    private record Positioned(AbstractWidget widget, int x, int y, String key) {}
    private record Layout(Row row, int x, int y, int height, int textWidth, int summaryY) {}
    private record Outline(int top, int bottom, int labelWidth) {}
    private final List<Outline> outlines = new ArrayList<>();
    void beginGroup(String id, Component label) { rows.add(new Row(id, label, Component.empty(), List.of(), true, id, false, 1)); }
    void endGroup() { rows.add(new Row(null, Component.empty(), Component.empty(), List.of(), true, null, false, -1)); }
    private final List<Row> rows = new ArrayList<>();
    private final List<Positioned> positions = new ArrayList<>();
    private final List<Layout> layout = new ArrayList<>();
    private final Minecraft client;
    private final boolean navigation;
    private int extent;
    private String highlighted;
    private SectionNavigation sections = new SectionNavigation();
    SettingsPanel(Minecraft client, int x, int y, int width, int height, boolean navigation) {
        super(client, width, height, y, x, 6);
        this.client = client; this.navigation = navigation;
    }
    void heading(Component label) { rows.add(new Row(null, label, Component.empty(), List.of(), true, null, false, 0)); }
    void section(String id, Component label) { rows.add(new Row(null, label, Component.empty(), List.of(), true, id, false, 0)); }
    void row(Component label, Component summary, AbstractWidget... controls) {
        row(null, label, summary, controls);
    }
    void row(String key, Component label, Component summary, AbstractWidget... controls) { rows.add(new Row(key, label, summary, List.of(controls), false, null, false, 0)); }
    void anchorRow(String key, Component label, Component summary, AbstractWidget... controls) {
        rows.add(new Row(key, label, summary, List.of(controls), false, key, false, 0));
    }
    void field(String key, Component label, Component summary, AbstractWidget... controls) {
        rows.add(new Row(key, label, summary, List.of(controls), false, key, true, 0));
    }
    void summary(String key, Component summary) {
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i);
            if (key.equals(row.key)) { rows.set(i, new Row(row.key, row.label, summary, row.controls, row.heading, row.anchor, row.field, row.boundary)); break; }
        }
        var bookmark = bookmark(); measure(); restore(bookmark);
    }
    void finish(float scroll) { measure(); scrollDistance = Math.clamp(scroll, 0, Math.max(0, extent - height + border)); position(); }
    float scroll() { return scrollDistance; }
    boolean isNavigation() { return navigation; }
    void highlight(String id) { highlighted = id; }
    String highlighted() { return highlighted; }
    private float maximumScroll() { return Math.max(0, extent - height + border); }
    void jump(String section) { scrollDistance = sections.jump(section, maximumScroll()); position(); }
    String activeSection() { return sections.active(scrollDistance, maximumScroll()); }
    SectionNavigation.Bookmark bookmark() { return sections.remember(scrollDistance, maximumScroll()); }
    void restore(SectionNavigation.Bookmark bookmark) { scrollDistance = sections.restore(bookmark, maximumScroll()); position(); }
    String focusKey() { return positions.stream().filter(p -> p.widget == getFocused()).map(Positioned::key).filter(java.util.Objects::nonNull).findFirst().orElse(null); }
    void restoreFocus(String key) { if (key != null) positions.stream().filter(p -> key.equals(p.key)).findFirst().ifPresent(p -> { setFocused(p.widget); p.widget.setFocused(true); }); }
    void focusVisibleControl() {
        position();
        positions.stream().map(Positioned::widget).filter(widget -> widget.active && widget.visible
                && widget.getY() >= top + border && widget.getY() + widget.getHeight() <= bottom - border)
                .findFirst().ifPresent(widget -> { setFocused(widget); widget.setFocused(true); });
    }
    private int usableWidth() { return Math.max(30, width - 2 * border - 8); }
    private void measure() {
        int y = 0, wrap = usableWidth(), frameTop = -1, frameLabelWidth = 0;
        positions.clear(); layout.clear(); outlines.clear(); sections = new SectionNavigation();
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i);
            if (row.boundary == 1) {
                if (frameTop >= 0) throw new IllegalStateException("Nested settings groups are unsupported");
                y += 4; frameTop = y + 6 + client.font.lineHeight / 2;
                frameLabelWidth = Math.min(wrap - 16, client.font.width(row.label));
            }
            if (row.boundary == -1) {
                if (frameTop < 0) throw new IllegalStateException("Unmatched settings group");
                y += 2; outlines.add(new Outline(frameTop, y, frameLabelWidth)); frameTop = -1; y += 14; continue;
            }
            int inset = frameTop >= 0 ? 8 : 0, available = wrap - inset * 2;
            if (row.field && available >= 492 && i + 1 < rows.size() && rows.get(i + 1).field) {
                int column = (available - 12) / 2;
                int first = measureRow(row, inset, y, column);
                int second = measureRow(rows.get(++i), inset + column + 12, y, column);
                y += Math.max(first, second) + 12;
            } else {
                int measured = measureRow(row, inset, y, available);
                y += measured + (row.heading || measured == 0 ? 0 : navigation ? 4 : 12);
            }
        }
        if (frameTop >= 0) throw new IllegalStateException("Unclosed settings group");
        extent = Math.max(1, y);
    }
    private int measureRow(Row row, int left, int y, int wrap) {
        if (row.anchor != null) sections.add(row.anchor, y);
        if (row.heading) {
            int height = 12 + client.font.split(row.label, wrap).size() * client.font.lineHeight;
            layout.add(new Layout(row, left, y + 6, height, wrap, 0)); return height;
        }
        if (navigation && row.controls.size() == 1) {
            var button = row.controls.getFirst(); button.setWidth(wrap);
            button.setHeight(Math.max(20, client.font.split(row.label, button instanceof NavigationButton nav ? nav.labelWidth() : wrap - 8).size() * client.font.lineHeight + 10));
            positions.add(new Positioned(button, left, y, row.key));
            layout.add(new Layout(row, left, y, button.getHeight(), wrap, 0)); return button.getHeight();
        }
        int controls = row.controls.stream().mapToInt(AbstractWidget::getWidth).sum() + Math.max(0, row.controls.size() - 1) * 4;
        boolean stacked = row.field || wrap - controls - 8 < 100;
        int textWidth = stacked ? wrap : wrap - controls - 8;
        int labelHeight = row.label.getString().isEmpty() ? 0 : client.font.split(row.label, textWidth).size() * client.font.lineHeight;
        int summaryY = labelHeight + (row.summary.getString().isEmpty() ? 0 : 4);
        int textHeight = summaryY + (row.summary.getString().isEmpty() ? 0 : client.font.split(row.summary, textWidth).size() * client.font.lineHeight);
        int controlHeight = row.controls.stream().mapToInt(AbstractWidget::getHeight).max().orElse(0);
        int height = stacked && controlHeight > 0 ? textHeight + 4 + controlHeight : Math.max(textHeight, controlHeight);
        int x = left + (stacked ? 0 : wrap - controls);
        int actionY = stacked ? y + textHeight + 4 : y + (height - controlHeight) / 2;
        int index = 0;
        for (var widget : row.controls) { positions.add(new Positioned(widget, x, actionY, row.key == null ? null : row.key + "/" + index++)); x += widget.getWidth() + 4; }
        layout.add(new Layout(row, left, y, height, textWidth, summaryY)); return height;
    }
    private void position() {
        for (var at : positions) at.widget.setPosition(left + border + at.x, top + border + at.y - (int) scrollDistance);
    }
    @Override protected int getContentHeight() { return extent; }
    @Override protected void drawBackground(GuiGraphics graphics, Tesselator tess, float tick) {
        graphics.fill(left, top, right, bottom, navigation ? 0xec202020 : 0xec101010);
    }
    @Override protected void drawPanel(GuiGraphics graphics, int entryRight, int relativeY, Tesselator tess, int mx, int my) {
        position();
        for (var outline : outlines) {
            int x = left + border, edge = x + usableWidth() - 1;
            int y = relativeY + outline.top, end = relativeY + outline.bottom;
            if (end < top || y >= bottom) continue;
            int color = 0xff000000 | ChatFormatting.DARK_GRAY.getColor();
            graphics.vLine(x, y, end, color); graphics.vLine(edge, y, end, color);
            graphics.hLine(x, x + 4, y, color);
            if (x + outline.labelWidth + 12 < edge) graphics.hLine(x + outline.labelWidth + 12, edge, y, color);
            graphics.hLine(x, edge, end, color);
        }
        for (var at : layout) {
            int y = relativeY + at.y;
            if (y + at.height < top || y >= bottom) continue;
            var row = at.row;
            if (navigation && !row.heading && row.controls.size() == 1) row.controls.getFirst().render(graphics, mx, my, 0);
            else {
                int color = row.anchor != null && row.anchor.equals(highlighted) ? ACTIVE_TEXT : row.heading ? DESCRIPTION_TEXT : TITLE_TEXT;
                graphics.drawWordWrap(client.font, row.label, left + border + at.x, y, at.textWidth, color);
                if (!row.summary.getString().isEmpty()) graphics.drawWordWrap(client.font, row.summary, left + border + at.x, y + at.summaryY, at.textWidth, DESCRIPTION_TEXT);
                for (var widget : row.controls) if (widget.getY() + widget.getHeight() > top && widget.getY() < bottom) widget.render(graphics, mx, my, 0);
            }
        }
    }
    @Override public List<? extends GuiEventListener> children() { return positions.stream().map(Positioned::widget).toList(); }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (!isMouseOver(x, y) || extent <= height - border && x >= right - 8) return false;
        position(); return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (!isMouseOver(x, y) || extent <= height - border) return false;
        boolean result = super.mouseScrolled(x, y, dx, dy); position(); return result;
    }
    @Override public void setFocused(GuiEventListener listener) {
        super.setFocused(listener);
        if (listener instanceof AbstractWidget widget) {
            if (widget.getY() < top + border) scrollDistance -= top + border - widget.getY();
            else if (widget.getY() + widget.getHeight() > bottom - border) scrollDistance += widget.getY() + widget.getHeight() - bottom + border;
            scrollDistance = Math.clamp(scrollDistance, 0, Math.max(0, extent - height + border)); position();
        }
    }
    @Override public void setFocused(boolean focused) {
        if (!focused) { if (getFocused() != null) getFocused().setFocused(false); super.setFocused((GuiEventListener) null); }
    }
    @Override public ComponentPath nextFocusPath(net.minecraft.client.gui.navigation.FocusNavigationEvent event) {
        position(); return super.nextFocusPath(event);
    }
    @Override public NarrationPriority narrationPriority() { return getFocused() == null ? NarrationPriority.NONE : NarrationPriority.FOCUSED; }
    @Override public void updateNarration(NarrationElementOutput output) {
        if (getFocused() instanceof AbstractWidget widget) widget.updateNarration(output);
        else output.add(NarratedElementType.TITLE, Component.translatable("skyweave_compat.configuration.title"));
    }
}
