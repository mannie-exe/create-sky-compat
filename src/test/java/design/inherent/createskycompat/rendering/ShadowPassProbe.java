package design.inherent.createskycompat.rendering;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import static org.junit.jupiter.api.Assertions.*;

/** Executes the handler extracted from the real transformed target with a controlled Iris API. */
final class ShadowPassProbe {
    public static final class Api {
        static boolean shadow;
        public static Api getInstance() { return new Api(); }
        public boolean isRenderingShadowPass() { return shadow; }
    }

    static void verify(ClassNode target) throws Exception {
        verify(target, "createSky$skipShadowComposite");
    }
    static void verify(ClassNode target, String handler) throws Exception {
        var originalMethod = target.methods.stream().filter(m -> m.name.endsWith(handler)).findFirst().orElseThrow();
        var method = new MethodNode(Opcodes.ASM9, originalMethod.access, originalMethod.name, originalMethod.desc, null, null);
        originalMethod.accept(method);
        boolean instance = (method.access & Opcodes.ACC_STATIC) == 0;
        method.name = "check";
        method.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
        method.visibleAnnotations = null;
        method.invisibleAnnotations = null;
        String original = "net/irisshaders/iris/api/v0/IrisApi";
        String stub = Api.class.getName().replace('.', '/');
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof FrameNode) method.instructions.remove(instruction);
            if (instance && instruction instanceof VarInsnNode variable) {
                assertTrue(variable.var > 0, "the guard must not depend on its renderer instance");
                variable.var--;
            }
            if (instruction instanceof MethodInsnNode call && call.owner.equals(original)) {
                call.owner = stub;
                call.desc = call.desc.replace(original, stub);
                call.itf = false;
                if (call.getOpcode() == Opcodes.INVOKEINTERFACE) call.setOpcode(Opcodes.INVOKEVIRTUAL);
            }
        }
        var generated = new ClassNode();
        generated.version = Opcodes.V21;
        generated.access = Opcodes.ACC_PUBLIC;
        generated.name = "ExtractedShadowGuard";
        generated.superName = "java/lang/Object";
        generated.methods.add(method);
        var writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        generated.accept(writer);
        class Loader extends ClassLoader {
            Class<?> define(byte[] bytes) { return defineClass("ExtractedShadowGuard", bytes, 0, bytes.length); }
        }
        var check = new Loader().define(writer.toByteArray()).getMethod("check", CallbackInfo.class);
        for (boolean shadow : new boolean[]{false, true, false}) {
            Api.shadow = shadow;
            var callback = new CallbackInfo("balloon", true);
            check.invoke(null, callback);
            assertEquals(shadow, callback.isCancelled(), "only the shadow pass must be cancelled: " + handler);
        }
    }
}
