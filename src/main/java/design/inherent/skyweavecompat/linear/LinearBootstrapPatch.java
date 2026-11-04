package design.inherent.skyweavecompat.linear;

import java.util.ArrayList;
import java.util.Set;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Removes only the three client registrations in the pinned dedicated constructor. */
public final class LinearBootstrapPatch {
    private static final String BUS = "net/neoforged/bus/api/IEventBus";
    private static final String HANDLER = "com/bearing/linearbearing/registrate/ClientModHandler";
    private LinearBootstrapPatch() {}

    public static void apply(ClassNode target) {
        if (!target.name.equals("com/bearing/linearbearing/LinearBearing"))
            throw new IllegalArgumentException("Unexpected Linear Bearing bootstrap target");
        var constructor = target.methods.stream().filter(m -> m.name.equals("<init>")
                && m.desc.equals("(L" + BUS + ";)V")).findFirst().orElseThrow();
        var removals = new ArrayList<AbstractInsnNode>();
        var listeners = new java.util.HashSet<String>();
        int initializer = 0;
        for (var instruction : constructor.instructions) {
            if (!(instruction instanceof MethodInsnNode call)) continue;
            if (call.owner.equals(BUS) && call.name.equals("addListener")
                    && call.desc.equals("(Ljava/util/function/Consumer;)V")) {
                var before = previousCode(call);
                if (!(before instanceof InvokeDynamicInsnNode lambda)) continue;
                for (var argument : lambda.bsmArgs) {
                    if (!(argument instanceof Handle handle) || !handle.getOwner().equals(HANDLER)) continue;
                    if (!Set.of("onClientSetup", "onModelBake").contains(handle.getName())
                            || !listeners.add(handle.getName()))
                        throw new IllegalStateException("Unexpected Linear Bearing client listener");
                    var bus = previousCode(lambda);
                    requireBus(bus);
                    removals.add(bus); removals.add(lambda); removals.add(call);
                }
            } else if (call.owner.equals("com/bearing/linearbearing/LinearBearingClient")
                    && call.name.equals("registerClient") && call.desc.equals("(L" + BUS + ";)V")) {
                var bus = previousCode(call); requireBus(bus);
                removals.add(bus); removals.add(call); initializer++;
            }
        }
        if (listeners.size() != 2 || initializer != 1)
            throw new IllegalStateException("Linear Bearing 1.3.5 client registrations changed; review bootstrap adapter");
        removals.forEach(constructor.instructions::remove);
    }

    private static void requireBus(AbstractInsnNode instruction) {
        if (!(instruction instanceof VarInsnNode variable) || variable.getOpcode() != Opcodes.ALOAD || variable.var != 1)
            throw new IllegalStateException("Unexpected Linear Bearing event-bus argument");
    }

    private static AbstractInsnNode previousCode(AbstractInsnNode instruction) {
        var previous = instruction.getPrevious();
        while (previous != null && previous.getOpcode() < 0) previous = previous.getPrevious();
        return previous;
    }
}
