package design.inherent.skyweavecompat.client;

import design.inherent.skyweavecompat.rendering.ReverseVertices;
import design.inherent.skyweavecompat.rendering.SurfaceRegions;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Copies final model output, retaining the caller's native neighbor-cull group. */
public final class GlassBackfaces {
    private static final String RECHISELED_GLASS =
            "com.supermartijn642.rechiseled.blocks.RechiseledGlassPillarBlock";

    private GlassBackfaces() {}

    public static boolean fusionTarget(BlockState state) {
        if (state == null) return false;
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id.getNamespace().equals("minecraft")) {
            String path = id.getPath();
            return path.equals("glass") || path.equals("glass_pane")
                    || path.endsWith("_stained_glass") || path.endsWith("_stained_glass_pane");
        }
        // Metadata selection loads Fusion only. Do not resolve optional Rechiseled classes here.
        for (Class<?> type = state.getBlock().getClass(); type != null; type = type.getSuperclass())
            if (RECHISELED_GLASS.equals(type.getName())) return true;
        return false;
    }

    public static List<BakedQuad> append(List<BakedQuad> quads) {
        return append(quads, null);
    }

    public static List<BakedQuad> append(List<BakedQuad> quads, GlassSurfaceContacts contacts) {
        if (GlassSurfaceContacts.sampling()) return quads;
        ArrayList<BakedQuad> result = null;
        for (BakedQuad quad : quads) {
            if (quad instanceof ReverseQuad || hasReverse(quads, quad)) continue;
            if (result == null) result = new ArrayList<>(quads);
            var surface = contacts == null ? null : GlassSurfaceContacts.surface(quad);
            float boundary = quad.getDirection().getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0;
            var regions = surface != null && Math.abs(surface.plane() - boundary) <= GlassSurfaceContacts.POSITION_TOLERANCE
                    ? contacts.side(quad.getDirection()) : List.<SurfaceRegions.Rect>of();
            if (regions.isEmpty()) result.add(new ReverseQuad(quad, ReverseVertices.copy(quad.getVertices())));
            else for (var part : SurfaceRegions.partition(surface.bounds(), regions))
                result.add(new ReverseQuad(quad, reversePart(quad, surface, part)));
        }
        return result == null ? quads : result;
    }

    private static boolean hasReverse(List<BakedQuad> quads, BakedQuad source) {
        for (BakedQuad other : quads) {
            if (other instanceof ReverseQuad reverse && reverse.source == source) return true;
            if (other.getDirection() != source.getDirection().getOpposite()) continue;
            int[] a = source.getVertices(), b = other.getVertices();
            boolean same = true;
            for (int vertex = 0; vertex < 4 && same; vertex++) {
                boolean found = false;
                for (int candidate = 0; candidate < 4; candidate++) {
                    int i = vertex * 8, j = candidate * 8;
                    if (a[i] == b[j] && a[i + 1] == b[j + 1] && a[i + 2] == b[j + 2]) {
                        found = true;
                        break;
                    }
                }
                same = found;
            }
            if (same) return true;
        }
        return false;
    }

    // Contact-only render skin: one sixteenth of a 16px texel, 128 Sodium position steps.
    // Only the shared reverse area moves; original and exposed faces retain their positions.
    private static final float CONTACT_DEPTH = 1f / 256;

    private static int[] reversePart(BakedQuad quad, GlassSurfaceContacts.Surface surface, SurfaceRegions.Part part) {
        int[] original = quad.getVertices();
        int[] result = original.clone();
        var bounds = surface.bounds(); var clipped = part.bounds();
        float shift = part.shared() ? -quad.getDirection().getAxisDirection().getStep() * CONTACT_DEPTH : 0;
        if (clipped.equals(bounds)) {
            if (shift != 0) for (int vertex = 0; vertex < 4; vertex++) {
                int offset = vertex * 8 + surface.axis();
                result[offset] = Float.floatToIntBits(Float.intBitsToFloat(original[offset]) + shift);
            }
            return ReverseVertices.copy(result);
        }
        double[] weights = new double[4];
        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * 8;
            float oldU = Float.intBitsToFloat(original[offset + surface.u()]);
            float oldV = Float.intBitsToFloat(original[offset + surface.v()]);
            float u = Math.abs(oldU - bounds.u0()) <= GlassSurfaceContacts.POSITION_TOLERANCE ? clipped.u0() : clipped.u1();
            float v = Math.abs(oldV - bounds.v0()) <= GlassSurfaceContacts.POSITION_TOLERANCE ? clipped.v0() : clipped.v1();
            result[offset + surface.u()] = Float.floatToIntBits(u);
            result[offset + surface.v()] = Float.floatToIntBits(v);
            result[offset + surface.axis()] = Float.floatToIntBits(surface.plane() + shift);
            if (!clipped.equals(bounds)) {
                float tu = (u - bounds.u0()) / (bounds.u1() - bounds.u0());
                float tv = (v - bounds.v0()) / (bounds.v1() - bounds.v0());
                for (int corner = 0; corner < 4; corner++) weights[corner] = weight(original, corner, surface, tu, tv);
                for (int attribute = 3; attribute < 8; attribute++) {
                    if (attribute == 4 || attribute == 5) {
                        double value = 0;
                        for (int corner = 0; corner < 4; corner++)
                            value += weights[corner] * Float.intBitsToFloat(original[corner * 8 + attribute]);
                        result[offset + attribute] = Float.floatToIntBits((float) value);
                    } else {
                        // Color, packed light and packed normals retain their individual channels.
                        int value = 0, bits = attribute == 6 ? 16 : 8, mask = (1 << bits) - 1;
                        for (int channel = 0; channel < 32; channel += bits) {
                            double component = 0;
                            for (int corner = 0; corner < 4; corner++) {
                                int raw = original[corner * 8 + attribute] >>> channel;
                                component += weights[corner] * (attribute == 7 && channel < 24 ? (byte) raw : raw & mask);
                            }
                            value |= ((int) Math.round(component) & mask) << channel;
                        }
                        result[offset + attribute] = value;
                    }
                }
            }
        }
        return ReverseVertices.copy(result);
    }

    private static double weight(int[] vertices, int corner, GlassSurfaceContacts.Surface surface, float u, float v) {
        float a = Float.intBitsToFloat(vertices[corner * 8 + surface.u()]);
        float b = Float.intBitsToFloat(vertices[corner * 8 + surface.v()]);
        return (Math.abs(a - surface.bounds().u0()) <= GlassSurfaceContacts.POSITION_TOLERANCE ? 1 - u : u)
                * (Math.abs(b - surface.bounds().v0()) <= GlassSurfaceContacts.POSITION_TOLERANCE ? 1 - v : v);
    }

    private static final class ReverseQuad extends BakedQuad {
        private final BakedQuad source;
        ReverseQuad(BakedQuad source, int[] vertices) {
            super(vertices, source.getTintIndex(),
                    source.getDirection().getOpposite(), source.getSprite(), source.isShade(),
                    source.hasAmbientOcclusion());
            this.source = source;
        }
    }
}
