package design.inherent.createskycompat.questlog.client;

import design.inherent.createskycompat.questlog.ScrollGeometry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.network.chat.Component;
import org.infernalstudios.questlog.client.gui.QuestlogGuiSet;
import org.infernalstudios.questlog.client.gui.components.ScrollableComponent;
import org.infernalstudios.questlog.client.gui.components.scrollable.Scrollable;
import org.lwjgl.glfw.GLFW;

/** Questlog content and skin with a fully internal hit gutter and proportional thumb. */
public final class JournalScrollPane extends ScrollableComponent {
    private final int x, y;
    private final QuestlogGuiSet skin;
    private final Component label;
    private boolean focused, dragging;
    private double dragY;
    private ScrollGeometry dragOrigin;

    public JournalScrollPane(int x, int y, int width, int height, Scrollable content, QuestlogGuiSet skin, Component label) {
        super(x, y, Math.max(1, width), Math.max(1, height), content, skin.scrollbar);
        this.x = x; this.y = y; this.skin = skin; this.label = label;
    }

    private ScrollGeometry geometry() { return ScrollGeometry.of(height, scrollable.getHeight(), getScrollAmount()); }
    @Override public int getScrollbarWidth() { return 12; }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!net.minecraft.client.Minecraft.getInstance().isWindowActive()) dragging = false;
        var geometry = geometry();
        setScrollAmount(geometry.offset());
        graphics.enableScissor(x, y, x + width - getScrollbarWidth(), y + height);
        scrollable.renderBackground(graphics, mouseX - x, (int) (mouseY - y + getScrollAmount()), partialTick);
        scrollable.render(graphics, mouseX - x, (int) (mouseY - y + getScrollAmount()), partialTick);
        graphics.disableScissor();
        if (geometry.maximum() <= 0) return;
        // The original sprite contains 12 pixels of transparent padding above/below.
        // Clip and scale only its central visible strip; keep the resource-pack texture.
        int rail = x + width - 6;
        graphics.enableScissor(x + width - 12, y, x + width, y + height);
        for (int line = y; line < y + height; line++) skin.scrollbar.background().blit(graphics, rail - 8, line);
        int start = y + (int) Math.round(geometry.thumbStart());
        graphics.pose().pushPose();
        graphics.pose().translate(rail - 14, start, 0);
        graphics.pose().scale(1, (float) geometry.thumbLength() / 12, 1);
        skin.scrollbar.bar().blit(graphics, 0, -12);
        graphics.pose().popPose();
        graphics.disableScissor();
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (!isMouseOver(mx, my)) return false;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && mx >= x + width - getScrollbarWidth() && canScroll()) {
            var g = geometry();
            double local = my - y;
            if (local < g.thumbStart() || local >= g.thumbStart() + g.thumbLength())
                setScrollAmount(g.offset() + (local < g.thumbStart() ? -height : height));
            dragOrigin = geometry(); dragY = my; dragging = true;
            return true;
        }
        return mx < x + width - getScrollbarWidth() && scrollable instanceof GuiEventListener listener
                && listener.mouseClicked(mx - x, my - y + getScrollAmount(), button);
    }

    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !dragging) return false;
        setScrollAmount(dragOrigin.drag(my - dragY)); return true;
    }
    @Override public boolean mouseReleased(double mx, double my, int button) {
        boolean wasDragging = dragging; dragging = false; return wasDragging;
    }
    @Override public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (!isMouseOver(mx, my) || !canScroll()) return false;
        setScrollAmount(getScrollAmount() - dy * 12); return true;
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (!focused) return false;
        if (scrollable instanceof GuiEventListener listener && listener.keyPressed(key, scan, modifiers)) return true;
        double amount = switch (key) {
            case GLFW.GLFW_KEY_HOME -> 0;
            case GLFW.GLFW_KEY_END -> geometry().maximum();
            case GLFW.GLFW_KEY_PAGE_UP -> getScrollAmount() - height;
            case GLFW.GLFW_KEY_PAGE_DOWN -> getScrollAmount() + height;
            case GLFW.GLFW_KEY_UP -> getScrollAmount() - 12;
            case GLFW.GLFW_KEY_DOWN -> getScrollAmount() + 12;
            default -> Double.NaN;
        };
        if (Double.isNaN(amount)) return false;
        setScrollAmount(amount); return true;
    }
    @Override public boolean isFocused() { return focused; }
    @Override public void setFocused(boolean focused) {
        this.focused = focused;
        if (!focused) dragging = false;
        if (scrollable instanceof GuiEventListener listener) listener.setFocused(focused);
    }
    @Override public NarrationPriority narrationPriority() { return focused ? NarrationPriority.FOCUSED : NarrationPriority.NONE; }
    @Override public void updateNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, label);
        if (scrollable instanceof net.minecraft.client.gui.narration.NarratableEntry entry) entry.updateNarration(output);
    }
}
