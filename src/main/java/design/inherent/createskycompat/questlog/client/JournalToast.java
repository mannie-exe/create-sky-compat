package design.inherent.createskycompat.questlog.client;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.infernalstudios.questlog.core.quests.Quest;

/** Native queue and Questlog frame; only the background interior receives a subtle lighter wash. */
public final class JournalToast implements Toast {
    private static long session;
    private final long generation = session;
    private Quest quest;
    private boolean completed, renewed;
    private long shownAt;
    private List<FormattedCharSequence> titleLines = List.of(), nameLines = List.of();
    private Object language;
    private long resources = -1;
    public JournalToast(Quest quest, boolean completed) { this.quest = quest; this.completed = completed; }
    public static void invalidateSession() { session++; }
    public void complete(Quest quest) { this.quest = quest; completed = true; renewed = true; language = null; }
    @Override public Object getToken() { return quest.getId(); }
    @Override public Visibility render(GuiGraphics g, ToastComponent owner, long elapsed) {
        if (generation != session || JournalPreferences.NOTIFICATIONS.get() == JournalPreferences.Notifications.OFF
                || !completed && JournalPreferences.NOTIFICATIONS.get() != JournalPreferences.Notifications.PROGRESS_AND_COMPLETION) return Visibility.HIDE;
        if (renewed) { shownAt = elapsed; renewed = false; }
        var font = owner.getMinecraft().font;
        if (language != net.minecraft.locale.Language.getInstance() || resources != JournalReloads.revision()) {
            language = net.minecraft.locale.Language.getInstance(); resources = JournalReloads.revision();
            Component title = JournalRows.text(completed ? quest.isRewarded() || quest.rewards.isEmpty() ? "completed" : "reward_ready" : "started");
            titleLines = font.split(title, 122); nameLines = font.split(quest.getDisplay().getTitle(), 122);
            if (titleLines.isEmpty()) titleLines = List.of(FormattedCharSequence.EMPTY);
            if (nameLines.isEmpty()) nameLines = List.of(FormattedCharSequence.EMPTY);
        }
        quest.getDisplay().getGuiSet().toast.blit(g, -13, -9);
        // Preserve the frame and its shading. Text and icons are drawn afterward, untinted.
        g.fill(3, 3, 153, 29, 0x33ffffff);
        var palette = org.infernalstudios.questlog.Questlog.getConfig().colors;
        int count = titleLines.size() + nameLines.size();
        long time = elapsed - shownAt;
        List<FormattedCharSequence> lines;
        int color;
        if (count <= 2) {
            g.drawString(font, titleLines.getFirst(), 30, 6, palette.toastTitleColor, false);
            g.drawString(font, nameLines.getFirst(), 30, 17, palette.toastDescriptionColor, false);
        } else {
            // Two-line pages keep the native frame/slot height and never paint outside it.
            long headingDuration = ((titleLines.size() + 1) / 2) * 1500L;
            boolean heading = time < headingDuration;
            lines = heading ? titleLines : nameLines;
            color = heading ? palette.toastTitleColor : palette.toastDescriptionColor;
            int page = heading ? (int) (time / 1500) * 2 : (int) Math.max(0, (time - headingDuration) / 1800) * 2;
            page = Math.min(page, Math.max(0, ((lines.size() - 1) / 2) * 2));
            for (int i = page; i < Math.min(lines.size(), page + 2); i++)
                g.drawString(font, lines.get(i), 30, 6 + (i - page) * 11, color, false);
        }
        var icon = quest.getDisplay().getIcon();
        if (JournalPreferences.ITEM_IMAGES.get() && icon != null) icon.blit(g, 8, 8);
        long duration = Math.max(5000, ((titleLines.size() + 1) / 2) * 1500L + ((nameLines.size() + 1) / 2) * 1800L);
        return time >= duration ? Visibility.HIDE : Visibility.SHOW;
    }
}
