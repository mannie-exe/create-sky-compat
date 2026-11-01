package design.inherent.createskycompat.recovery;

/** Optional persisted metadata on the existing YiGD recovery record. */
public interface GraveRecoveryState {
    RecoveryAnchor createSky$anchor();
    void createSky$anchor(RecoveryAnchor anchor);
}
