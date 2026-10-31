package design.inherent.createskycompat.client;

import dev.djefrey.colorwheel.ColorwheelBufferBuilder;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;

/** Loaded only by the selected Colorwheel adaptation; plain Iris mesh emitters remain supported. */
public final class ColorwheelMaterials {
    private ColorwheelMaterials() {}

    public static final BlockMaterialScope.Writer WRITER = new BlockMaterialScope.Writer() {
        public void begin(BlockSensitiveBufferBuilder buffer, BlockMaterialScope.Material material) {
            if (buffer instanceof ColorwheelBufferBuilder colorwheel)
                colorwheel.clrwlBeginBlock(material.id(), (byte) 0, material.emission(), true,
                        material.x(), material.y(), material.z());
            else material.begin(buffer);
        }

        public void end(BlockSensitiveBufferBuilder buffer) {
            if (buffer instanceof ColorwheelBufferBuilder colorwheel) colorwheel.clrwlEndBlock();
            else buffer.endBlock();
        }
    };
}
