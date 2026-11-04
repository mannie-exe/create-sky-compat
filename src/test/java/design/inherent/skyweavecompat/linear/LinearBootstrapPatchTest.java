package design.inherent.skyweavecompat.linear;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

class LinearBootstrapPatchTest {
    private static ClassNode released() throws Exception {
        try (var in = LinearBootstrapPatchTest.class.getResourceAsStream("/com/bearing/linearbearing/LinearBearing.class")) {
            assertNotNull(in);
            var node = new ClassNode(); new ClassReader(in.readAllBytes()).accept(node, 0); return node;
        }
    }

    @Test void removesClientLinkageAndPreservesSafeConstructorRegistrations() throws Exception {
        var node = released();
        var original = node.methods.stream().filter(m -> m.name.equals("<init>")).findFirst().orElseThrow();
        int count = original.instructions.size();
        LinearBootstrapPatch.apply(node);
        assertEquals(count - 8, original.instructions.size());
        for (var instruction : original.instructions) {
            if (instruction instanceof MethodInsnNode call)
                assertNotEquals("com/bearing/linearbearing/LinearBearingClient", call.owner);
            if (instruction instanceof InvokeDynamicInsnNode lambda)
                for (var argument : lambda.bsmArgs)
                    if (argument instanceof Handle handle)
                        assertFalse(handle.getOwner().contains("ClientModHandler"));
        }
        long listeners = java.util.Arrays.stream(original.instructions.toArray())
                .filter(i -> i instanceof MethodInsnNode m && m.name.equals("addListener")).count();
        assertEquals(2, listeners, "data generation and creative-tab registration must remain");
        for (String owner : java.util.List.of("com/bearing/linearbearing/registrate/ModBlocks",
                "com/bearing/linearbearing/registrate/ModComponents"))
            assertTrue(java.util.Arrays.stream(original.instructions.toArray())
                    .anyMatch(i -> i instanceof MethodInsnNode m && m.owner.equals(owner) && m.name.equals("register")));
        var writer = new ClassWriter(0); node.accept(writer);
        assertDoesNotThrow(() -> new ClassReader(writer.toByteArray()));
    }

    @Test void changedBootstrapFailsBeforePartiallyEditingTheConstructor() throws Exception {
        var node = released();
        var constructor = node.methods.stream().filter(m -> m.name.equals("<init>")).findFirst().orElseThrow();
        for (var instruction : constructor.instructions)
            if (instruction instanceof MethodInsnNode call && call.name.equals("registerClient")) call.name = "changed";
        int count = constructor.instructions.size();
        assertThrows(IllegalStateException.class, () -> LinearBootstrapPatch.apply(node));
        assertEquals(count, constructor.instructions.size());
    }
}
