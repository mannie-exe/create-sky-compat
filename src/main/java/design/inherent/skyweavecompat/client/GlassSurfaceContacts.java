package design.inherent.skyweavecompat.client;

import design.inherent.skyweavecompat.rendering.SurfaceRegions.Rect;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/** Immutable neighboring face geometry for one model-data evaluation, never a world cache. */
public record GlassSurfaceContacts(List<List<Rect>> sides) {
    public static final ModelProperty<GlassSurfaceContacts> PROPERTY = new ModelProperty<>();
    private static final ThreadLocal<Boolean> SAMPLING = new ThreadLocal<>();
    // Sodium's 20-bit position encoding spans 32 blocks. Also absorbs native quarter-turn noise.
    static final float POSITION_TOLERANCE = 1f / 32768;
    public GlassSurfaceContacts {
        if (sides.size() != 6) throw new IllegalArgumentException("Expected six contact groups");
        sides = sides.stream().map(List::copyOf).toList();
    }
    public List<Rect> side(Direction direction) { return sides.get(direction.ordinal()); }
    public static boolean sampling() { return Boolean.TRUE.equals(SAMPLING.get()); }

    public static GlassSurfaceContacts capture(BlockAndTintGetter view, BlockPos pos) {
        if (sampling()) return null;
        SAMPLING.set(true);
        try {
            var sides = new ArrayList<List<Rect>>(6);
            boolean any = false;
            for (Direction side : Direction.values()) {
                var regions = new ArrayList<Rect>();
                BlockPos neighborPos = pos.relative(side);
                BlockState neighbor = view.getBlockState(neighborPos);
                // Static glass is the admitted scope, not arbitrary block-entity model data.
                if (!neighbor.isAir() && !neighbor.hasBlockEntity()) {
                    var model = Minecraft.getInstance().getBlockRenderer().getBlockModel(neighbor);
                    var data = model.getModelData(view, neighborPos, neighbor, ModelData.EMPTY);
                    long seed = neighbor.getSeed(neighborPos);
                    var random = RandomSource.create(seed);
                    boolean visible = Block.shouldRenderFace(neighbor, view, neighborPos, side.getOpposite(), pos);
                    for (var layer : model.getRenderTypes(neighbor, random, data)) {
                        for (int group = 0; group < (visible ? 2 : 1); group++) {
                            random.setSeed(seed);
                            var quads = model.getQuads(neighbor, group == 0 ? null : side.getOpposite(), random, data, layer);
                            for (BakedQuad quad : quads) {
                                if (quad.getDirection() != side.getOpposite()) continue;
                                var surface = surface(quad);
                                float boundary = side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 0 : 1;
                                if (surface != null && Math.abs(surface.plane() - boundary) <= POSITION_TOLERANCE
                                        && !regions.contains(surface.bounds())) regions.add(surface.bounds());
                                if (regions.size() > 128) return null; // Outside the admitted bounded model scope.
                            }
                        }
                    }
                }
                any |= !regions.isEmpty(); sides.add(List.copyOf(regions));
            }
            return any ? new GlassSurfaceContacts(sides) : null;
        } finally {
            SAMPLING.remove();
        }
    }

    public record Surface(int axis, int u, int v, float plane, Rect bounds) {}

    static Surface surface(BakedQuad quad) {
        int[] vertices = quad.getVertices();
        if (vertices.length != 32) return null;
        int axis = quad.getDirection().getAxis().ordinal();
        int u = (axis + 1) % 3, v = (axis + 2) % 3;
        float plane = Float.intBitsToFloat(vertices[axis]);
        if (!Float.isFinite(plane)) return null;
        float u0 = Float.POSITIVE_INFINITY, v0 = u0, u1 = Float.NEGATIVE_INFINITY, v1 = u1;
        for (int i = 0; i < 4; i++) {
            float coordinate = Float.intBitsToFloat(vertices[i * 8 + axis]);
            if (!Float.isFinite(coordinate) || Math.abs(coordinate - plane) > POSITION_TOLERANCE) return null;
            float a = Float.intBitsToFloat(vertices[i * 8 + u]), b = Float.intBitsToFloat(vertices[i * 8 + v]);
            if (!Float.isFinite(a) || !Float.isFinite(b)) return null;
            u0 = Math.min(u0, a); u1 = Math.max(u1, a); v0 = Math.min(v0, b); v1 = Math.max(v1, b);
        }
        if (u0 >= u1 || v0 >= v1) return null;
        int corners = 0;
        for (int i = 0; i < 4; i++) {
            float a = Float.intBitsToFloat(vertices[i * 8 + u]), b = Float.intBitsToFloat(vertices[i * 8 + v]);
            if (Math.min(Math.abs(a - u0), Math.abs(a - u1)) > POSITION_TOLERANCE
                    || Math.min(Math.abs(b - v0), Math.abs(b - v1)) > POSITION_TOLERANCE) return null;
            int corner = (Math.abs(a - u0) <= POSITION_TOLERANCE ? 0 : 1)
                    | (Math.abs(b - v0) <= POSITION_TOLERANCE ? 0 : 2);
            if ((corners & 1 << corner) != 0) return null;
            corners |= 1 << corner;
        }
        return new Surface(axis, u, v, plane, new Rect(u0, v0, u1, v1));
    }
}
