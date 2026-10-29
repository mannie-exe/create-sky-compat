// SPDX-License-Identifier: LGPL-3.0-only
package design.inherent.createskycompat.rendering;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.service.MixinService;
import static org.junit.jupiter.api.Assertions.*;

public final class PatchTest {
    private static final String TARGET = "foundry.veil.impl.client.render.shader.program.ShaderProgramImpl$Wrapper";
    @Test void wrapsReleasedVeilIncludingItsOriginalIrisInjections() throws Exception {
        MixinBootstrap.init();
        var service = (TestMixinService) MixinService.getService();
        var transformer = service.transformer();
        MixinEnvironment.getCurrentEnvironment().setActiveTransformer(transformer);
        MixinExtrasBootstrap.init();
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        // Load the original shipped mixin directly, without the game's full Veil plugin.
        Mixins.addConfiguration("veil-original-test.mixins.json");
        Mixins.addConfiguration("framebuffer-test.mixins.json");
        Mixins.addConfiguration("rendering-test.mixins.json");
        byte[] original;
        try (var in = service.getResourceAsStream(TARGET.replace('.', '/') + ".class")) {
            original = in.readAllBytes();
        }
        byte[] transformed = transformer.transformClassBytes(TARGET, TARGET, original);
        var node = new ClassNode();
        new ClassReader(transformed).accept(node, 0);
        var apply = method(node, "apply");
        var clear = method(node, "clear");
        assertTrue(calls(apply, "veilIrisFix$apply") > 0, "outer apply must invoke our wrapper");
        assertTrue(calls(clear, "veilIrisFix$clear") > 0, "outer clear must invoke our wrapper");
        assertTrue(node.methods.stream().anyMatch(m -> calls(m, "veil$bindSimpleFramebuffer") > 0),
                "released Veil injection must be present inside the wrapped method");
        assertTrue(node.methods.stream().anyMatch(m -> calls(m, "bindDefault") > 0));
        assertEquals(2, calls(method(node, "veilIrisFix$restore"), "_glBindFramebuffer"));
        assertFalse(method(node, "veilIrisFix$clear").tryCatchBlocks.isEmpty(), "restoration must run on clear failure");
        FramebufferProbe.verify(transformed);
        String balloon = "dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.effect.ClientBalloonEffectRenderer";
        var balloonNode = transformed(service, transformer, balloon);
        assertEquals(1, calls(method(balloonNode, "onRenderLevelStage"), "createSky$skipShadowComposite"));
        ShadowPassProbe.verify(balloonNode);
        var model = transformed(service, transformer, "net.minecraft.client.renderer.block.ModelBlockRenderer");
        assertEquals(2, model.methods.stream().filter(m -> calls(m, "createSky$material") > 0).count(),
                "both AO and flat NeoForge methods must be wrapped");
        var single = transformed(service, transformer, "dev.ryanhcode.sable.neoforge.platform.SableSubLevelRenderPlatformImpl");
        assertEquals(1, calls(method(single, "getRenderLayers"), "createSky$matchingModelData"));
        Files.createDirectories(Path.of("build/verification"));
        Files.write(Path.of("build/verification/Wrapper-patched.class"), transformed);
    }
    private static ClassNode transformed(TestMixinService service,
            org.spongepowered.asm.mixin.transformer.IMixinTransformer transformer, String name) throws Exception {
        try (var input = service.getResourceAsStream(name.replace('.', '/') + ".class")) {
            assertNotNull(input, "Released target missing: " + name);
            byte[] transformed = transformer.transformClassBytes(name, name, input.readAllBytes());
            var node = new ClassNode();
            new ClassReader(transformed).accept(node, 0);
            Files.createDirectories(Path.of("build/verification"));
            Files.write(Path.of("build/verification/" + name.substring(name.lastIndexOf('.') + 1) + "-patched.class"), transformed);
            return node;
        }
    }
    private static MethodNode method(ClassNode node, String name) {
        return node.methods.stream().filter(m -> m.name.equals(name) || m.name.endsWith("$" + name))
            .findFirst().orElseThrow(() -> new AssertionError("Missing " + name + ": " + node.methods.stream().map(m -> m.name).toList()));
    }
    private static long calls(MethodNode method, String suffix) {
        return Arrays.stream(method.instructions.toArray()).filter(i -> i instanceof MethodInsnNode m && m.name.endsWith(suffix)).count();
    }
}
