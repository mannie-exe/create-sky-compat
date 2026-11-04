package design.inherent.createskycompat.client;

import com.mojang.blaze3d.vertex.MeshData;
import net.irisshaders.batchedentityrendering.impl.BlendingStateHolder;
import net.irisshaders.batchedentityrendering.impl.TransparencyType;
import net.irisshaders.batchedentityrendering.impl.WrappableRenderType;
import net.minecraft.client.renderer.RenderType;
import java.util.Optional;

/** Preserves native states and Iris sorting without being unwrapped by wrapExactlyOnce. */
public final class WorldTextRenderType extends RenderType implements WrappableRenderType, BlendingStateHolder {
    private final RenderType original;

    public WorldTextRenderType(RenderType original) {
        super("create_sky_compat:world_text", original.format(), original.mode(), original.bufferSize(),
                original.affectsCrumbling(), original.sortOnUpload, original::setupRenderState, original::clearRenderState);
        this.original = original;
    }

    @Override public void setupRenderState() { WorldTextScope.render(super::setupRenderState); }
    @Override public void draw(MeshData mesh) { WorldTextScope.render(() -> original.draw(mesh)); }
    @Override public RenderType unwrap() { return original; }
    @Override public Optional<RenderType> outline() { return original.outline(); }
    @Override public boolean isOutline() { return original.isOutline(); }
    @Override public boolean canConsolidateConsecutiveGeometry() { return original.canConsolidateConsecutiveGeometry(); }
    @Override public TransparencyType getTransparencyType() { return ((BlendingStateHolder) original).getTransparencyType(); }
    @Override public void setTransparencyType(TransparencyType type) { ((BlendingStateHolder) original).setTransparencyType(type); }
}
