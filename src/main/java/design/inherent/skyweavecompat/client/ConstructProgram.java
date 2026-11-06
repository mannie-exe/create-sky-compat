package design.inherent.skyweavecompat.client;

/** Uniform locations are owned by the actual Iris shader instance, never by a reused GL ID. */
public interface ConstructProgram {
    ConstructUniforms skyweave$constructUniforms();
}
