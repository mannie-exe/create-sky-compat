package design.inherent.skyweavecompat.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.ChatFormatting;

/** Text-only secondary action; the uniform font applies only to its information glyph. */
final class SettingsInformationButton extends PlainTextButton {
    private static final ResourceLocation FONT = ResourceLocation.withDefaultNamespace("uniform");
    private final Component subject;

    SettingsInformationButton(Component subject, Runnable action) {
        super(0, 0, 20, 20, Component.literal("i").withStyle(style -> style.withFont(FONT)), button -> action.run(), Minecraft.getInstance().font);
        this.subject = subject;
        setTooltip(Tooltip.create(ConfigurationOverview.text("details_of", subject)));
    }
    @Override public void renderWidget(GuiGraphics graphics, int x, int y, float tick) {
        var font = Minecraft.getInstance().font;
        Component label = isHoveredOrFocused() ? getMessage().copy().withStyle(ChatFormatting.UNDERLINE) : getMessage();
        graphics.drawCenteredString(font, label, getX() + getWidth() / 2, getY() + (getHeight() - font.lineHeight) / 2,
                isHoveredOrFocused() ? SettingsPanel.ACTIVE_TEXT : SettingsPanel.TITLE_TEXT);
    }
    @Override protected MutableComponent createNarrationMessage() {
        return Component.translatable("gui.narrate.button", ConfigurationOverview.text("details_of", subject));
    }
}
