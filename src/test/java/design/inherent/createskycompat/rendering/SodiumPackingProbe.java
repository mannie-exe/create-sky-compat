package design.inherent.createskycompat.rendering;

import net.caffeinemc.mods.sodium.client.model.light.data.LightDataAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import static org.junit.jupiter.api.Assertions.*;

/** Execute the transformed handler; replace only SDL's lookup, which needs a running client. */
public final class SodiumPackingProbe {
    private static int dynamic;
    private SodiumPackingProbe() {}
    public static int lookup(BlockAndTintGetter level, BlockPos position, int current) {
        assertEquals(new BlockPos(3, 7, -2), position);
        return dynamic;
    }

    static void verify(ClassNode sodium) throws Exception {
        var handler = sodium.methods.stream().filter(m -> m.name.endsWith("$createSky$dynamicLight"))
                .findFirst().orElseThrow();
        String target = "design/inherent/createskycompat/rendering/SodiumPackingExecution";
        var clone = new ClassNode(); clone.version = Opcodes.V21; clone.access = Opcodes.ACC_PUBLIC;
        clone.name = target; clone.superName = "java/lang/Object";
        clone.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "level", "Lnet/minecraft/world/level/BlockAndTintGetter;", null, null));
        var constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
        constructor.instructions.add(new InsnNode(Opcodes.RETURN)); clone.methods.add(constructor);
        var apply = new MethodNode(Opcodes.ACC_PUBLIC, "apply", handler.desc, null, null);
        handler.accept(apply);
        for (var instruction : apply.instructions) {
            if (instruction instanceof FieldInsnNode field && field.name.equals("level")) field.owner = target;
            if (instruction instanceof MethodInsnNode call && call.owner.equals("fr0nick/sabledynlights/util/WorldPosResolver")) {
                call.owner = Type.getInternalName(SodiumPackingProbe.class); call.name = "lookup";
            }
        }
        clone.methods.add(apply);
        var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); clone.accept(writer);
        Class<?> execution = new ClassLoader(SodiumPackingProbe.class.getClassLoader()) {
            Class<?> define(byte[] bytes) { return defineClass(null, bytes, 0, bytes.length); }
        }.define(writer.toByteArray());
        Object instance = execution.getConstructor().newInstance();
        var invoke = execution.getMethod("apply", int.class, int.class, int.class, CallbackInfoReturnable.class);
        for (boolean flags : new boolean[]{false, true}) {
            int other = LightDataAccess.packSL(13) | LightDataAccess.packLU(7) | LightDataAccess.packAO(.625f)
                    | LightDataAccess.packEM(flags) | LightDataAccess.packOP(flags)
                    | LightDataAccess.packFO(flags) | LightDataAccess.packFC(flags);
            for (int old = 0; old < 16; old++) for (int next = 0; next < 16; next++) {
                dynamic = next;
                var result = new CallbackInfoReturnable<Integer>("packing", true, other | LightDataAccess.packBL(old));
                invoke.invoke(instance, 3, 7, -2, result);
                assertEquals(Math.max(old, next), LightDataAccess.unpackBL(result.getReturnValueI()));
                assertEquals(other, result.getReturnValueI() & ~LightDataAccess.packBL(15));
            }
        }
    }
}
