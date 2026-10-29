// SPDX-License-Identifier: LGPL-3.0-only
package design.inherent.createskycompat.rendering;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.service.MixinService;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/** Separate JVM so Mixin starts in the physical SERVER environment. */
public final class ServerProbe {
    public static void main(String[] args) throws Exception {
        MixinBootstrap.init();
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.SERVER);
        Mixins.addConfiguration("framebuffer-test.mixins.json");
        TestMixinService service = (TestMixinService) MixinService.getService();
        byte[] original;
        try (var in = service.getResourceAsStream("foundry.veil.impl.client.render.shader.program.ShaderProgramImpl$Wrapper".replace('.', '/') + ".class")) {
            original = in.readAllBytes();
        }
        byte[] result = service.transformer().transformClassBytes("foundry.veil.impl.client.render.shader.program.ShaderProgramImpl$Wrapper", "foundry.veil.impl.client.render.shader.program.ShaderProgramImpl$Wrapper", original);
        assertArrayEquals(original, result, "client mixin must not run on a dedicated server");
        System.out.println("PASS: server environment leaves Veil byte-identical");
    }
}
