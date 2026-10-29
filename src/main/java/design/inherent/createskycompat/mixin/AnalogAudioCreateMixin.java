package design.inherent.createskycompat.mixin;

import java.lang.reflect.Method;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Correct only the affected upstream lookup; retain its handler and setup lifecycle. */
@Pseudo
@Mixin(targets = "com.palm1.analogaudio.integration.CreateCompat", remap = false)
public abstract class AnalogAudioCreateMixin {
    @Redirect(method = "lambda$onCommonSetup$1", require = 1, allow = 1,
            at = @At(value = "INVOKE", target = "Ljava/lang/Class;getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;"))
    private static Method createSky$movementRegistryMethod(Class<?> implementation, String name,
            Class<?>[] parameters) throws ReflectiveOperationException {
        // Generic parameters erase to Object. The implementation is package-private,
        // so invoke through the public API rather than opening reflective access.
        return Class.forName("com.simibubi.create.api.registry.SimpleRegistry")
                .getMethod("register", Object.class, Object.class);
    }
}
