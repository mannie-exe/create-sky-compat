package design.inherent.skyweavecompat.configuration;

import java.util.ArrayList;
import java.util.List;

/** Preflight the complete pending set; report partial outcomes without pretending files are one transaction. */
public final class SettingsCommit {
    @FunctionalInterface public interface Operation { void run() throws Exception; }
    public record Write(String filename, Operation check, Operation save) {}
    public record Result(List<String> written, String failed, Exception failure) {
        public Result { written = List.copyOf(written); }
        public boolean success() { return failure == null; }
    }
    public static Result save(List<Write> pending) {
        for (var write : pending) try { write.check.run(); }
        catch (Exception failure) { return new Result(List.of(), write.filename, failure); }
        var written = new ArrayList<String>();
        for (var write : pending) try { write.save.run(); written.add(write.filename); }
        catch (Exception failure) { return new Result(written, write.filename, failure); }
        return new Result(written, null, null);
    }
    private SettingsCommit() {}
}
