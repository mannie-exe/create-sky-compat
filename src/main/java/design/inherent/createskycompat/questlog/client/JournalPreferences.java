package design.inherent.createskycompat.questlog.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Presentation preferences are independent from immutable optional-hook selection. */
public final class JournalPreferences {
    public enum Notifications implements net.neoforged.neoforge.common.TranslatableEnum {
        OFF, COMPLETION, PROGRESS_AND_COMPLETION;
        @Override public net.minecraft.network.chat.Component getTranslatedName() {
            return net.minecraft.network.chat.Component.translatable("create_sky_compat.journal.notifications." + name().toLowerCase(java.util.Locale.ROOT));
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
    public static void toggleImages() { set(ITEM_IMAGES, !ITEM_IMAGES.get()); }
    public static void toggleSymbols() { set(CONDITION_SYMBOLS, !CONDITION_SYMBOLS.get()); }
    public static void cycleNotifications() {
        var options = Notifications.values(); set(NOTIFICATIONS, options[(NOTIFICATIONS.get().ordinal() + 1) % options.length]);
    }
    private static <T> void set(ModConfigSpec.ConfigValue<T> value, T next) {
        T previous = value.get();
        try { value.set(next); SPEC.save(); }
        catch (RuntimeException failure) { value.set(previous); throw failure; }
    }
    private JournalPreferences() {}
}
