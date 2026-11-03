package design.inherent.createskycompat.client;

import design.inherent.createskycompat.rendering.ReverseVertices;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.registries.BuiltInRegistries;
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
        ArrayList<BakedQuad> result = null;
        for (BakedQuad quad : quads) {
            if (quad instanceof ReverseQuad || hasReverse(quads, quad)) continue;
            if (result == null) result = new ArrayList<>(quads);
            result.add(new ReverseQuad(quad));
        }
        return result == null ? quads : result;
    }

    private static boolean hasReverse(List<BakedQuad> quads, BakedQuad source) {
        for (BakedQuad other : quads) {
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

    private static final class ReverseQuad extends BakedQuad {
        ReverseQuad(BakedQuad source) {
            super(ReverseVertices.copy(source.getVertices()), source.getTintIndex(),
                    source.getDirection().getOpposite(), source.getSprite(), source.isShade(),
                    source.hasAmbientOcclusion());
        }
    }
}
