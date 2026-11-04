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
        Mixins.addConfiguration("sdl-original-test.mixins.json");
        Mixins.addConfiguration("iris-nametag-test.mixins.json");
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
        var display = transformed(service, transformer, "net.minecraft.client.renderer.entity.DisplayRenderer$TextDisplayRenderer");
        assertEquals(1, display.methods.stream().filter(m -> m.name.equals("renderInner"))
                .mapToLong(m -> calls(m, "createSky$skipTextDisplayShadow")).sum(), "inject the concrete overload once, not its bridge");
        ShadowPassProbe.verify(display, "createSky$skipTextDisplayShadow");
        assertEquals(1, display.methods.stream().filter(m -> m.name.equals("renderInner"))
                .mapToLong(m -> calls(m, "createSky$textDisplayLighting")).sum());
        var entity = transformed(service, transformer, "net.minecraft.client.renderer.entity.EntityRenderer");
        assertEquals(1, calls(method(entity, "renderNameTag"), "createSky$skipNametagShadow"));
        assertEquals(0, calls(method(entity, "render"), "createSky$skipNametagShadow"));
        var nameInstructions = Arrays.asList(method(entity, "renderNameTag").instructions.toArray());
        int guard = -1, material = -1;
        for (int i = 0; i < nameInstructions.size(); i++) {
            if (nameInstructions.get(i) instanceof MethodInsnNode call) {
                if (call.name.endsWith("createSky$skipNametagShadow")) guard = i;
                if (call.name.endsWith("setNameTagId")) material = i;
            }
        }
        assertTrue(guard >= 0 && material > guard, "cancel before Iris installs nametag material state");
        ShadowPassProbe.verify(entity, "createSky$skipNametagShadow");
        assertEquals(1, calls(method(entity, "renderNameTag"), "createSky$nametagLighting"));
        assertEquals(0, calls(method(entity, "render"), "createSky$nametagLighting"));
        var textMap = transformed(service, transformer, "net.irisshaders.iris.pipeline.programs.ShaderMap");
        assertEquals(1, calls(method(textMap, "getShader"), "createSky$textShader"));
        var textPipeline = transformed(service, transformer, "net.irisshaders.iris.pipeline.IrisRenderingPipeline");
        assertEquals(1, calls(method(textPipeline, "destroy"), "createSky$releaseTextReferences"));
        assertTrue(textPipeline.interfaces.contains("design/inherent/createskycompat/client/WorldTextLighting$Pipeline"));
        for (String type : java.util.List.of("BlockDisplayRenderer", "ItemDisplayRenderer")) {
            var control = transformed(service, transformer, "net.minecraft.client.renderer.entity.DisplayRenderer$" + type);
            assertTrue(control.methods.stream().noneMatch(m -> m.name.contains("skipTextDisplayShadow")));
        }
        var emission = transformed(service, transformer, "net.minecraft.world.level.lighting.BlockLightEngine");
        assertEquals(1, calls(method(emission, "getEmission"), "createSky$clientEmission"));
        var dynamic = transformed(service, transformer, "net.minecraft.world.level.lighting.LightEngine");
        assertEquals(1, calls(method(dynamic, "getLightValue"), "createSky$clientLight"));
        assertFalse(method(emission, "createSky$clientEmission").tryCatchBlocks.isEmpty());
        assertFalse(method(dynamic, "createSky$clientLight").tryCatchBlocks.isEmpty());
        var virtual = transformed(service, transformer, "fr0nick.sabledynlights.virtual.VirtualLightManager");
        assertEquals(1, calls(method(virtual, "getVirtualEmission"), "createSky$excludeClientLight"));
        var lamb = transformed(service, transformer, "fr0nick.sabledynlights.lamb.LambDynLightsDelegate");
        assertEquals(1, calls(method(lamb, "getDynamicLightLevel"), "createSky$excludeClientLight"));
        var view = transformed(service, transformer, "fr0nick.sabledynlights.util.WorldPosResolver");
        assertEquals(1, calls(method(view, "getCombinedWorldLight"), "createSky$serverLight"));
        var sources = transformed(service, transformer, "fr0nick.sabledynlights.lamb.create.ContraptionEntityEventHandler");
        assertEquals(1, Arrays.stream(sources.methods.stream().filter(m -> m.name.startsWith("createSky$trackedContraptions_"))
                .findFirst().orElseThrow().instructions.toArray())
                .filter(i -> i instanceof FieldInsnNode f && f.name.equals("contraptionEntities")).count());
        var sableState = transformed(service, transformer, "fr0nick.sabledynlights.lamb.sable.SableSubLevelEventHandler");
        assertEquals(2, sableState.methods.stream().filter(m -> m.name.startsWith("createSky$last")).count());
        var lifecycle = transformed(service, transformer, "fr0nick.sabledynlights.LambModEventHandler");
        assertEquals(1, calls(method(lifecycle, "onLevelUnload"), "createSky$unload"));
        assertEquals(1, calls(method(lifecycle, "onLoggingOut"), "createSky$disconnect"));
        assertEquals(1, calls(method(lifecycle, "createSky$clearSources"), "onContraptionEntityLeave"));
        assertEquals(1, calls(method(lifecycle, "createSky$clearSources"), "clear"));
        var sodium = transformed(service, transformer,
                "net.caffeinemc.mods.sodium.client.model.light.data.LightDataAccess");
        assertEquals(1, calls(method(sodium, "compute"), "createSky$dynamicLight"));
        assertEquals(0, calls(method(sodium, "getLightmap"), "createSky$dynamicLight"),
                "the static decoder has no world position");
        assertEquals(1, calls(method(sodium, "createSky$dynamicLight"), "getCombinedWorldLight"));
        assertEquals(2, Arrays.stream(method(sodium, "createSky$dynamicLight").instructions.toArray())
                .filter(i -> i instanceof MethodInsnNode m && m.name.equals("packBL")).count());
        SodiumPackingProbe.verify(sodium);
        var model = transformed(service, transformer, "net.minecraft.client.renderer.block.ModelBlockRenderer");
        assertEquals(2, model.methods.stream().filter(m -> calls(m, "createSky$material") > 0).count(),
                "both AO and flat NeoForge methods must be wrapped");
        var colorwheel = transformed(service, transformer, "dev.djefrey.colorwheel.compile.ClrwlProgram");
        assertEquals(1, calls(method(colorwheel, "postLink"), "createSky$availableSamplers"));
        assertEquals(9, calls(method(colorwheel, "postLink"), "createSky$unusedOitSampler"));
        var materials = transformed(service, transformer, "design.inherent.createskycompat.client.BlockMaterialScope");
        assertEquals(1, calls(method(materials, "render"), "createSky$colorwheelMaterial"));
        assertEquals(1, calls(method(materials, "createSky$colorwheelMaterial"), "renderWith"));
        var single = transformed(service, transformer, "dev.ryanhcode.sable.neoforge.platform.SableSubLevelRenderPlatformImpl");
        assertEquals(1, calls(method(single, "getRenderLayers"), "createSky$matchingModelData"));
        var dispatcher = transformed(service, transformer, "dev.ryanhcode.sable.sublevel.render.dispatcher.VanillaSubLevelRenderDispatcher");
        assertEquals(1, calls(method(dispatcher, "renderSectionLayer"), "createSky$flushCurrentPass"));
        assertEquals(1, calls(method(dispatcher, "createSky$flushCurrentPass"), "isShaderPackInUse"));
        assertEquals(1, calls(method(dispatcher, "createSky$flushCurrentPass"), "renderAfterSections"));
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
