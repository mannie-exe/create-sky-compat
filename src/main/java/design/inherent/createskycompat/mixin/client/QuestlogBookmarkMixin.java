package design.inherent.createskycompat.mixin.client;

import design.inherent.createskycompat.questlog.client.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.infernalstudios.questlog.Questlog;
import org.infernalstudios.questlog.client.gui.QuestlogGuiSet;
import org.infernalstudios.questlog.client.gui.screen.QuestlogScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = QuestlogScreen.class, remap = false)
public abstract class QuestlogBookmarkMixin extends Screen {
    @Unique private boolean createSky$visitingSettings;
    protected QuestlogBookmarkMixin(Component title) { super(title); }
    @Inject(method = "refreshList", at = @At("TAIL"), require = 1)
    private void createSky$settingsBookmark(CallbackInfo ci) {
        int left = (width - 275) / 2 + Questlog.getConfig().gui.mainPanelX;
        int top = (height - 166) / 2 + Questlog.getConfig().gui.mainPanelY;
        var bookmark = new JournalButton(0, 0, Math.min(88, Math.max(32, left + 26)), JournalRows.text("settings"), QuestlogGuiSet.DEFAULT,
                () -> { createSky$visitingSettings = true; minecraft.setScreen(new JournalSettingsScreen(this, QuestlogGuiSet.DEFAULT)); });
        bookmark.setX(Math.clamp(left + 34 - bookmark.getWidth(), 4, Math.max(4, width - bookmark.getWidth() - 4)));
        bookmark.setY(Math.max(0, top - bookmark.getHeight() + 1));
        addRenderableWidget(bookmark);
        if (createSky$visitingSettings) { setFocused(bookmark); createSky$visitingSettings = false; }
    }
}
