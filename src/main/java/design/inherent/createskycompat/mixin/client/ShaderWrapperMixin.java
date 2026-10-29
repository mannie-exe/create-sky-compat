// SPDX-License-Identifier: LGPL-3.0-only
package design.inherent.createskycompat.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.platform.GlStateManager;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shadows.ShadowRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import static org.lwjgl.opengl.GL11C.glGetInteger;
import static org.lwjgl.opengl.GL30C.*;

/** Wraps the complete methods, including Veil's existing Iris injections. */
@Mixin(targets = "foundry.veil.impl.client.render.shader.program.ShaderProgramImpl$Wrapper", remap = false)
public abstract class ShaderWrapperMixin {
    @Unique private int veilIrisFix$read = -1;
    @Unique private int veilIrisFix$draw = -1;

    @WrapMethod(method = "apply")
    private void veilIrisFix$apply(Operation<Void> original) {
        // Repeated apply calls retain the framebuffer from the first activation.
        if (veilIrisFix$draw == -1) {
            veilIrisFix$read = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
            veilIrisFix$draw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        }
        try {
            original.call();
            var pipeline = Iris.getPipelineManager().getPipeline().orElse(null);
            if (pipeline instanceof IrisRenderingPipeline iris
                    && iris.shouldOverrideShaders() && !ShadowRenderer.ACTIVE) {
                iris.bindDefault();
            } else {
                // Ponder and other offscreen GUIs must keep their own target.
                veilIrisFix$restore();
            }
        } catch (RuntimeException | Error failure) {
            veilIrisFix$restore();
            veilIrisFix$read = veilIrisFix$draw = -1;
            throw failure;
        }
    }

    @WrapMethod(method = "clear")
    private void veilIrisFix$clear(Operation<Void> original) {
        try {
            original.call();
        } finally {
            veilIrisFix$restore();
            veilIrisFix$read = veilIrisFix$draw = -1;
        }
    }

    @Unique
    private void veilIrisFix$restore() {
        if (veilIrisFix$draw == -1) return;
        GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER, veilIrisFix$read);
        GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, veilIrisFix$draw);
    }
}
