package design.inherent.skyweavecompat.audio;

import com.palm1.analoglib.client.audio.engine.EmitterState;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/** Game-thread registry of Analog Audio emitters, excluding other AnalogLib consumers. */
public final class RadioSessions {
    private static final int LIMIT = 512;
    private static final Map<Object, Session> SOURCES = new LinkedHashMap<>();
    private static final Map<Key, PlaybackTimeline> CLOCKS = new LinkedHashMap<>();
    private static ClientLevel world;

    private RadioSessions() { }

    public static void touch(Object identity, String uuid, String url, long start, long duration, boolean looping) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != world) { clear(); world = level; }
        if (level == null) return;
        Session source = SOURCES.get(identity);
        if (source == null || source.start != start || !Objects.equals(source.uuid, uuid) || !Objects.equals(source.url, url)) {
            if (source != null) remove(identity);
            if (SOURCES.size() >= LIMIT) remove(SOURCES.keySet().iterator().next());
            source = new Session(uuid, url, start, CLOCKS.computeIfAbsent(new Key(uuid, url, start), key -> new PlaybackTimeline()), level.getGameTime());
        }
        source = new Session(uuid, url, start, source.clock, level.getGameTime());
        SOURCES.put(identity, source);
        source.clock.update(level.getGameTime(), start, duration, looping);
    }

    public static PlaybackTimeline find(String uuid, long start, List<EmitterState> emitters) {
        if (Minecraft.getInstance().level != world) return null;
        for (EmitterState emitter : emitters) {
            Session source = SOURCES.get(emitter.identity);
            if (source != null && source.start == start && Objects.equals(source.uuid, uuid)
                    && Objects.equals(source.url, emitter.url)
                    && world.getGameTime() - source.tick >= 0 && world.getGameTime() - source.tick <= 2) return source.clock;
        }
        return null;
    }

    public static boolean registered(Object identity) { return SOURCES.containsKey(identity); }

    public static void remove(Object identity) {
        Session source = SOURCES.remove(identity);
        if (source != null && SOURCES.values().stream().noneMatch(other -> other.clock == source.clock)) {
            source.clock.invalidate();
            CLOCKS.remove(new Key(source.uuid, source.url, source.start));
        }
    }

    public static void clear() {
        SOURCES.values().forEach(source -> source.clock.invalidate());
        SOURCES.clear();
        CLOCKS.clear();
        world = null;
    }

    private record Key(String uuid, String url, long start) { }
    private record Session(String uuid, String url, long start, PlaybackTimeline clock, long tick) { }
}
