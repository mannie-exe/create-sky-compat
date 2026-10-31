package design.inherent.createskycompat.questlog.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.infernalstudios.questlog.client.gui.QuestlogGuiSet;
import org.infernalstudios.questlog.util.texture.NineSliceTexture;

/** Native peripheral artwork with measured text and a visible keyboard-focus state. */
public final class JournalButton extends AbstractButton {
    private final Runnable action;
    private final java.util.List<net.minecraft.util.FormattedCharSequence> lines;
    private boolean selected;
    private final NineSliceTexture normal, highlighted;
    public JournalButton(int x, int y, int maximumWidth, Component label, QuestlogGuiSet skin, Runnable action) {
        super(x, y, Math.max(32, Math.min(maximumWidth, Minecraft.getInstance().font.width(label) + 16)), 18, label);
        this.action = action;
        lines = Minecraft.getInstance().font.split(label, Math.max(1, width - 16));
        height = Math.max(18, lines.size() * Minecraft.getInstance().font.lineHeight + 8);
        normal = new NineSliceTexture(skin.peripheralLoc, width, height, 12, 105, 88, 18, 256, 256, 4, 4);
        highlighted = new NineSliceTexture(skin.peripheralLoc, width, height, 122, 105, 88, 18, 256, 256, 4, 4);
    }
    public void selected(boolean value) { selected = value; }
    @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float tick) {
        var font = Minecraft.getInstance().font;
        boolean highlight = selected || isHoveredOrFocused();
        (highlight ? highlighted : normal).blit(graphics, getX(), getY());
        int y = getY() + (height - lines.size() * font.lineHeight) / 2 + 1;
        int color = org.infernalstudios.questlog.Questlog.getConfig().colors.textColor;
        for (var line : lines) {
            graphics.drawString(font, line, getX() + (width - font.width(line)) / 2, y, color, false);
            y += font.lineHeight;
        }
    }
    @Override public void onPress() { action.run(); }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
}
