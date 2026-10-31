package design.inherent.createskycompat.questlog.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Presentation preferences are independent from immutable optional-hook selection. */
public final class JournalPreferences {
    public enum Notifications implements net.neoforged.neoforge.common.TranslatableEnum {
        OFF, COMPLETION, PROGRESS_AND_COMPLETION;
        @Override public net.minecraft.network.chat.Component getTranslatedName() {
            return JournalRows.text("notifications." + name().toLowerCase(java.util.Locale.ROOT));
        }
    }
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ITEM_IMAGES, CONDITION_SYMBOLS;
    public static final ModConfigSpec.EnumValue<Notifications> NOTIFICATIONS;
    static {
        var builder = new ModConfigSpec.Builder();
        ITEM_IMAGES = builder.comment("Show actual item and block images in journal rows.")
                .translation("create_sky_compat.journal.images").define("itemImages", true);
        CONDITION_SYMBOLS = builder.comment("Show a compact condition symbol alongside its explicit text label.")
                .translation("create_sky_compat.journal.symbols").define("conditionSymbols", true);
        NOTIFICATIONS = builder.comment("First objective progress and completion; never prerequisite unlocks.")
                .translation("create_sky_compat.journal.notifications").defineEnum("notifications", Notifications.PROGRESS_AND_COMPLETION);
        SPEC = builder.build();
    }
    private JournalPreferences() {}
}
