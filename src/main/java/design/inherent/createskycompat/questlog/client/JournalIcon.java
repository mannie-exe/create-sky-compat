package design.inherent.createskycompat.questlog.client;

import net.minecraft.client.gui.GuiGraphics;
import org.infernalstudios.questlog.util.texture.Blittable;

/** Bound authored imagery uniformly while keeping ordinary item rendering unchanged. */
public record JournalIcon(Blittable source, float scale, int width, int height) {
    public static JournalIcon of(Blittable source) {
        if (source == null) return null;
        float scale = Math.min(1f, 32f / Math.max(1, Math.max(source.width(), source.height())));
        return new JournalIcon(source, scale, Math.max(1, Math.round(source.width() * scale)), Math.max(1, Math.round(source.height() * scale)));
    }
    public void blit(GuiGraphics graphics, int x, int y) {
        graphics.pose().pushPose(); graphics.pose().translate(x, y, 0); graphics.pose().scale(scale, scale, 1);
        source.blit(graphics, 0, 0); graphics.pose().popPose();
    }
}
