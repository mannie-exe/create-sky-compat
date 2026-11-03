package design.inherent.createskycompat.rendering;

/** Reverses BLOCK-format vertices without changing their position/UV association. */
public final class ReverseVertices {
    private ReverseVertices() {}

    public static int[] copy(int[] vertices) {
        if (vertices.length != 32) throw new IllegalArgumentException("Expected four BLOCK vertices");
        int[] result = new int[vertices.length];
        for (int target = 0; target < 4; target++) {
            int source = (4 - target) % 4;
            System.arraycopy(vertices, source * 8, result, target * 8, 8);
            int normal = vertices[source * 8 + 7];
            result[target * 8 + 7] = normal & 0xff000000
                    | (-(byte) normal & 255)
                    | (-(byte) (normal >>> 8) & 255) << 8
                    | (-(byte) (normal >>> 16) & 255) << 16;
        }
        return result;
    }
}
