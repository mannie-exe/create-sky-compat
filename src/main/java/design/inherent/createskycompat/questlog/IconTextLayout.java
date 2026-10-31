package design.inherent.createskycompat.questlog;

/** Integer placement shared by a measured icon and its text block. */
public record IconTextLayout(int height, int iconY, int textY) {
    public static IconTextLayout centered(int iconHeight, int textHeight) {
        int height = Math.max(Math.max(0, iconHeight), Math.max(0, textHeight));
        return new IconTextLayout(height, (height - iconHeight) / 2, (height - textHeight) / 2);
    }
}
