package design.inherent.createskycompat.terrain;

import design.inherent.createskycompat.mixin.OreVeinifierAccessor;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.*;

/** Native vein composition with reference-depth sampling and stateless cell interpolation. */
public final class IslandVeins {
    private final NoiseChunk.BlockStateFiller filler;

    public IslandVeins(RandomState random, NoiseGeneratorSettings settings) {
        DensityFunction.Visitor visitor = new DensityFunction.Visitor() {
            @Override public DensityFunction.NoiseHolder visitNoise(DensityFunction.NoiseHolder holder) {
                return new DensityFunction.NoiseHolder(holder.noiseData(),
                        random.getOrCreateNoise(holder.noiseData().unwrapKey().orElseThrow()));
            }
            @Override public DensityFunction apply(DensityFunction function) {
                if (function instanceof DensityFunctions.MarkerOrMarked marker
                        && ((Object)marker.type()).toString().equals("Interpolated")) {
                    return new Interpolated(marker.wrapped(), settings.noiseSettings().getCellWidth(),
                            settings.noiseSettings().getCellHeight());
                }
                return function;
            }
        };
        // Runtime routers may already be C2ME-compiled. Bind retained definitions rather
        // than replacing cache nodes inside someone else's compiled density graph.
        var router = settings.noiseRouter();
        filler = OreVeinifierAccessor.createSky$create(router.veinToggle().mapAll(visitor),
                router.veinRidged().mapAll(visitor), router.veinGap().mapAll(visitor), random.oreRandom());
    }

    public BlockState sample(int x, int worldY, int z, GeologicalFrame frame) {
        int y = frame.toReference(worldY);
        if (y < -60 || y > 50) return null;
        return filler.calculate(new DensityFunction.SinglePointContext(x, y, z));
    }

    /** Each thread retains one cell per noise; queries never depend on traversal order. */
    private static final class Interpolated implements DensityFunction.SimpleFunction {
        private final DensityFunction source;
        private final int width, height;
        private final ThreadLocal<Cell> last = new ThreadLocal<>();
        private record Cell(int x, int y, int z, double[] corners) {}

        private Interpolated(DensityFunction source, int width, int height) {
            this.source = source; this.width = width; this.height = height;
        }
        @Override public double compute(FunctionContext point) {
            int x = Math.floorDiv(point.blockX(), width) * width;
            int y = Math.floorDiv(point.blockY(), height) * height;
            int z = Math.floorDiv(point.blockZ(), width) * width;
            var cell = last.get();
            if (cell == null || cell.x != x || cell.y != y || cell.z != z) {
                double[] corners = new double[8];
                for (int dx = 0; dx < 2; dx++) for (int dy = 0; dy < 2; dy++) for (int dz = 0; dz < 2; dz++)
                    corners[dx * 4 + dy * 2 + dz] = source.compute(
                            new SinglePointContext(x + dx * width, y + dy * height, z + dz * width));
                cell = new Cell(x, y, z, corners); last.set(cell);
            }
            double fx = (point.blockX() - x) / (double)width;
            double fy = (point.blockY() - y) / (double)height;
            double fz = (point.blockZ() - z) / (double)width;
            var c = cell.corners;
            // Match NoiseChunk's Y, then X, then Z interpolation order.
            return Mth.lerp(fz, Mth.lerp(fx, Mth.lerp(fy, c[0], c[2]), Mth.lerp(fy, c[4], c[6])),
                    Mth.lerp(fx, Mth.lerp(fy, c[1], c[3]), Mth.lerp(fy, c[5], c[7])));
        }
        @Override public double minValue() { return source.minValue(); }
        @Override public double maxValue() { return source.maxValue(); }
        @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            throw new UnsupportedOperationException("Runtime vein interpolation is not a datapack density function");
        }
    }
}
