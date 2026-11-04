package design.inherent.skyweavecompat.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

/** Reentrant query context; public so a transformed foreign block can access it. */
public record SignalRead(BlockGetter level, BlockPos pos) {}
