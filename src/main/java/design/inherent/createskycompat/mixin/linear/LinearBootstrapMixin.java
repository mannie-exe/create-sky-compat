package design.inherent.createskycompat.mixin.linear;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** The plugin removes client method references before the server links their lambdas. */
@Pseudo
@Mixin(targets = "com.bearing.linearbearing.LinearBearing", remap = false)
public abstract class LinearBootstrapMixin {}
