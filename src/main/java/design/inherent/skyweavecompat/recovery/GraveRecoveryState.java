package design.inherent.skyweavecompat.recovery;

/** Optional persisted metadata on the existing YiGD recovery record. */
public interface GraveRecoveryState {
    RecoveryAnchor skyweave$anchor();
    void skyweave$anchor(RecoveryAnchor anchor);
}
