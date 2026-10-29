// SPDX-License-Identifier: LGPL-3.0-only
package design.inherent.createskycompat.rendering;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.*;

/** Runs the real Mixin transformer against the released Veil class without starting Minecraft. */
public final class TestMixinService extends MixinServiceAbstract
        implements IClassProvider, IClassBytecodeProvider {
    public String getName() { return "Veil patch verification"; }
    public boolean isValid() { return true; }
    public MixinEnvironment.Phase getInitialPhase() { return MixinEnvironment.Phase.DEFAULT; }
    public IClassProvider getClassProvider() { return this; }
    public IClassBytecodeProvider getBytecodeProvider() { return this; }
    public ITransformerProvider getTransformerProvider() { return null; }
    public IClassTracker getClassTracker() { return null; }
    public IMixinAuditTrail getAuditTrail() { return null; }
    public Collection<String> getPlatformAgents() { return List.of(); }
    public IContainerHandle getPrimaryContainer() { return new ContainerHandleVirtual("veil-patch-tests"); }
    public InputStream getResourceAsStream(String name) {
        return getClass().getClassLoader().getResourceAsStream(name);
    }
    public URL[] getClassPath() { return new URL[0]; }
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return findClass(name, false);
    }
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, getClass().getClassLoader());
    }
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return findClass(name, initialize);
    }
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
        return getClassNode(name, true);
    }
    public ClassNode getClassNode(String name, boolean transform) throws ClassNotFoundException, IOException {
        return getClassNode(name, transform, 0);
    }
    public ClassNode getClassNode(String name, boolean transform, int flags)
            throws ClassNotFoundException, IOException {
        try (InputStream stream = getResourceAsStream(name.replace('.', '/') + ".class")) {
            if (stream == null) throw new ClassNotFoundException(name);
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, flags);
            return node;
        }
    }
    public IMixinTransformer transformer() {
        return getInternal(IMixinTransformerFactory.class).createTransformer();
    }
}
