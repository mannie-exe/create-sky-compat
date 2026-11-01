package design.inherent.createskycompat.mixin.client;

import com.ggrgg.createredstonelinkgui.client.screen.AbstractLinkConfigScreen;
import design.inherent.createskycompat.redstone.GuiRelocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Upstream stores its visible tooltip separately from the empty widget message. */
@Mixin(targets = "com.ggrgg.createredstonelinkgui.client.screen.AbstractLinkConfigScreen$ImageButton", remap = false)
public abstract class GuiRelocationScreenMixin extends Button {
    @Shadow @Final @Mutable private Component tooltip;
    protected GuiRelocationScreenMixin(int x, int y, int width, int height, Component label, OnPress press, CreateNarration narration) {
        super(x, y, width, height, label, press, narration);
    }
    @Inject(method = "<init>", at = @At("RETURN"), require = 1, allow = 1)
    private void createSky$availability(CallbackInfo ci) {
        var mc = Minecraft.getInstance();
        if (tooltip.getContents() instanceof TranslatableContents text && text.getKey().equals("gui.createredstonelinkgui.relocate")
                && mc.level != null && mc.screen instanceof AbstractLinkConfigScreen<?> screen
                && !GuiRelocation.movable(mc.level, screen.getMenu().getPos())) {
            active = false;
            tooltip = Component.translatable("message.create_sky_compat.redstone.move_scope");
        }
    }
}
