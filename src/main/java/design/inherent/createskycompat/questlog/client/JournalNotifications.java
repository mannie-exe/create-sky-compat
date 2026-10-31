package design.inherent.createskycompat.questlog.client;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import design.inherent.createskycompat.questlog.ProgressNoticeState;
import design.inherent.createskycompat.questlog.ProgressNoticeState.Notice;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.infernalstudios.questlog.QuestlogClient;
import org.infernalstudios.questlog.QuestlogClientEvents;
import org.infernalstudios.questlog.core.DefinitionUtil;
import org.infernalstudios.questlog.core.quests.Quest;
import org.infernalstudios.questlog.core.quests.objectives.Objective;
import org.infernalstudios.questlog.core.quests.objectives.logic.NotObjective;

/** Main-client-thread coordinator; observes synchronized objectives, never scans the player's inventory. */
public final class JournalNotifications {
    private record Pending(Quest quest, Notice notice, long at) {}
    private static final Map<ResourceLocation, ProgressNoticeState> states = new HashMap<>();
    private static final LinkedHashMap<ResourceLocation, Pending> pending = new LinkedHashMap<>();
    private static boolean ready;
    private static long tick;
    private static Object connection;
    private JournalNotifications() {}

    public static void clear() {
        states.clear(); pending.clear(); ready = false; tick = 0;
        JournalToast.invalidateSession(); connection = null;
    }
    public static void baseline() {
        JournalToast.invalidateSession();
        pending.clear(); states.clear();
        for (var quest : QuestlogClient.getLocal().getAllQuests()) states.put(quest.getId(), state(quest));
        ready = true; connection = Minecraft.getInstance().getConnection();
    }
    private static boolean progressed(Objective objective) {
        if (objective.isHidden() || objective instanceof NotObjective) return false;
        if (!objective.getChildren().isEmpty()) return objective.getChildren().stream().anyMatch(JournalNotifications::progressed);
        return objective.getUnits() > 0;
    }
    private static boolean progressed(Quest quest) { return quest.objectives.stream().anyMatch(JournalNotifications::progressed); }
    private static ProgressNoticeState state(Quest quest) {
        return new ProgressNoticeState(progressed(quest), quest.isCompleted(), quest.serialize().getBoolean("completed"));
    }
    public static void update(ResourceLocation id) {
        if (!ready) return;
        var quest = QuestlogClient.getLocal().getQuest(id);
        if (quest == null) { remove(id); return; }
        var previous = states.get(id);
        if (previous == null) { states.put(id, state(quest)); return; }
        Notice notice = previous.update(progressed(quest), quest.isCompleted(), quest.serialize().getBoolean("completed"));
        enqueue(quest, notice);
    }
    public static void complete(Quest quest) {
        if (!ready || quest == null) return;
        var previous = states.get(quest.getId());
        if (previous != null) enqueue(quest, previous.complete());
    }
    public static void remove(ResourceLocation id) { states.remove(id); pending.remove(id); }
    private static boolean progressAllowed(Quest quest) {
        var definition = DefinitionUtil.getCachedQuest(quest.getId());
        if (definition != null && definition.has("create_sky_compat") && definition.get("create_sky_compat").isJsonObject()) {
            var options = definition.getAsJsonObject("create_sky_compat");
            var enabled = options.get("toast_on_progress");
            if (enabled != null && enabled.isJsonPrimitive() && enabled.getAsJsonPrimitive().isBoolean()) return enabled.getAsBoolean();
        }
        return quest.getDisplay().shouldToastOnComplete();
    }
    private static void enqueue(Quest quest, Notice notice) {
        if (notice == Notice.NONE || !quest.isTriggered() || quest.getDisplay().isHidden()) return;
        var mode = JournalPreferences.NOTIFICATIONS.get();
        if (mode == JournalPreferences.Notifications.OFF) return;
        if (notice == Notice.STARTED && (mode != JournalPreferences.Notifications.PROGRESS_AND_COMPLETION || !progressAllowed(quest))) return;
        if (notice == Notice.COMPLETED && !quest.getDisplay().shouldToastOnComplete()) return;
        var toast = Minecraft.getInstance().getToasts().getToast(JournalToast.class, quest.getId());
        if (toast != null && notice == Notice.COMPLETED) { toast.complete(quest); pending.remove(quest.getId()); return; }
        var current = pending.get(quest.getId());
        if (current != null && current.notice == Notice.COMPLETED) return;
        if (current == null && pending.size() >= 32) {
            var expendable = pending.entrySet().stream().filter(e -> e.getValue().notice == Notice.STARTED)
                    .map(Map.Entry::getKey).findFirst().orElse(pending.keySet().iterator().next());
            pending.remove(expendable);
        }
        pending.put(quest.getId(), new Pending(quest, notice, current == null ? tick + 8 : current.at));
    }
    public static void tick() {
        var mc = Minecraft.getInstance();
        if (ready && (mc.player == null || mc.getConnection() != connection)) { clear(); return; }
        tick++;
        if (JournalPreferences.NOTIFICATIONS.get() == JournalPreferences.Notifications.OFF) { pending.clear(); return; }
        // Feed at most one toast each tick. The bounded journal queue coalesces before Minecraft owns it.
        var iterator = pending.entrySet().iterator();
        while (iterator.hasNext()) {
            var item = iterator.next().getValue();
            if (item.at > tick) continue;
            if (item.notice == Notice.STARTED && JournalPreferences.NOTIFICATIONS.get() != JournalPreferences.Notifications.PROGRESS_AND_COMPLETION) {
                iterator.remove(); continue;
            }
            mc.getToasts().addToast(new JournalToast(item.quest, item.notice == Notice.COMPLETED));
            QuestlogClientEvents.mostRecentNotificationQuest = item.quest;
            iterator.remove(); break;
        }
    }
}
