// SPDX-License-Identifier: LGPL-3.0-only
package design.inherent.createskycompat.rendering;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.opengl.GL30C.*;

/** Executes the transformed patch methods with controlled GL/Iris boundaries, not a game renderer. */
public final class FramebufferProbe {
    public interface Pipeline {}
    public static final class World implements Pipeline {
        public boolean override;
        public boolean shouldOverrideShaders() { return override; }
        public void bindDefault() { GL.read = 81; GL.draw = 82; }
    }
    public static final class Manager {
        public Optional<Pipeline> getPipeline() { return Optional.ofNullable(pipeline); }
    }
    public static final class Iris {
        public static Manager getPipelineManager() { return new Manager(); }
    }
    public static final class Shadows { public static boolean ACTIVE; }
    public static final class GL {
        public static int read, draw;
        public static int glGetInteger(int key) {
            if (key == GL_READ_FRAMEBUFFER_BINDING) return read;
            if (key == GL_DRAW_FRAMEBUFFER_BINDING) return draw;
            throw new AssertionError("Unexpected query " + key);
        }
        public static void _glBindFramebuffer(int target, int value) {
            if (target == GL_READ_FRAMEBUFFER) read = value;
            else if (target == GL_DRAW_FRAMEBUFFER) draw = value;
            else throw new AssertionError("Unexpected binding " + target);
        }
    }
    private static Pipeline pipeline;
    private static final Operation<Void> leaked = ignored -> { GL.read = 42; GL.draw = 43; return null; };
    private static final Operation<Void> noop = ignored -> null;
    private static void bound(int read, int draw) { assertEquals(read, GL.read); assertEquals(draw, GL.draw); }

    static void verify(byte[] transformed) throws Exception {
        var source = new ClassNode();
        new ClassReader(transformed).accept(source, 0);
        source.superName = "java/lang/Object";
        source.interfaces.clear(); source.innerClasses.clear(); source.nestHostClass = null;
        source.fields.removeIf(f -> !f.name.startsWith("veilIrisFix$"));
        source.methods.removeIf(m -> !m.name.contains("veilIrisFix$"));
        source.visibleAnnotations = null; source.invisibleAnnotations = null;
        for (var method : source.methods) {
            method.access = Opcodes.ACC_PUBLIC;
            method.visibleAnnotations = null; method.invisibleAnnotations = null;
        }
        var constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        for (var field : source.fields) {
            constructor.visitVarInsn(Opcodes.ALOAD, 0); constructor.visitInsn(Opcodes.ICONST_M1);
            constructor.visitFieldInsn(Opcodes.PUTFIELD, source.name, field.name, "I");
        }
        constructor.visitInsn(Opcodes.RETURN); constructor.visitMaxs(2, 1);
        source.methods.add(constructor);
        String prefix = "design/inherent/createskycompat/rendering/FramebufferProbe$";
        var names = Map.of(
            source.name, "ExtractedShader",
            "net/irisshaders/iris/Iris", prefix + "Iris",
            "net/irisshaders/iris/pipeline/PipelineManager", prefix + "Manager",
            "net/irisshaders/iris/pipeline/WorldRenderingPipeline", prefix + "Pipeline",
            "net/irisshaders/iris/pipeline/IrisRenderingPipeline", prefix + "World",
            "net/irisshaders/iris/shadows/ShadowRenderer", prefix + "Shadows",
            "com/mojang/blaze3d/platform/GlStateManager", prefix + "GL",
            "org/lwjgl/opengl/GL11C", prefix + "GL");
        var writer = new ClassWriter(0);
        source.accept(new ClassRemapper(writer, new SimpleRemapper(names)));
        class Loader extends ClassLoader { Class<?> define(byte[] bytes) { return defineClass("ExtractedShader", bytes, 0, bytes.length); } }
        Class<?> type = new Loader().define(writer.toByteArray());
        Method apply = java.util.Arrays.stream(type.getMethods()).filter(m -> m.getName().endsWith("veilIrisFix$apply")).findFirst().orElseThrow();
        Method clear = java.util.Arrays.stream(type.getMethods()).filter(m -> m.getName().endsWith("veilIrisFix$clear")).findFirst().orElseThrow();
        for (int mode = 0; mode < 5; mode++) {
            var world = new World(); world.override = mode >= 3;
            pipeline = mode == 0 ? null : mode == 1 ? new Pipeline() {} : world;
            Shadows.ACTIVE = mode == 3;
            var shader = type.getConstructor().newInstance();
            GL.read = 11; GL.draw = 12;
            clear.invoke(shader, noop); bound(11, 12);
            apply.invoke(shader, leaked); bound(mode == 4 ? 81 : 11, mode == 4 ? 82 : 12);
            apply.invoke(shader, leaked); // Repeated apply must not replace the saved target.
            clear.invoke(shader, noop); bound(11, 12);
            clear.invoke(shader, noop); bound(11, 12);
            Operation<Void> fail = ignored -> { GL.read = 99; GL.draw = 98; throw new IllegalStateException("injected failure"); };
            assertThrows(InvocationTargetException.class, () -> apply.invoke(shader, fail)); bound(11, 12);
            apply.invoke(shader, leaked);
            assertThrows(InvocationTargetException.class, () -> clear.invoke(shader, fail)); bound(11, 12);
            GL.read = 21; GL.draw = 22;
            apply.invoke(shader, leaked); clear.invoke(shader, noop); bound(21, 22);
        }
    }
}
