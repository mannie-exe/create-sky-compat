package design.inherent.skyweavecompat.shader;

import java.nio.file.Path;
import java.util.TreeMap;

/** Authoring entrypoint; uses exactly the same validation, handlers and archive writer as recipients. */
public final class ShaderCli {
    private ShaderCli() {}
    public static void main(String[] args) throws Exception {
        if (args.length < 1) throw new IllegalArgumentException("Usage: ShaderCli <workspace> [--check] [--rebuild]");
        Path root = Path.of(args[0]);
        var environment = new TreeMap<String, String>();
        // Authoring is an explicit qualification build, not a claim that these runtime mods are installed.
        for (var recipe : ShaderWorkspace.recipes(root)) if (recipe.enabled() && recipe.json().has("requires"))
            recipe.json().getAsJsonObject("requires").entrySet().forEach(e -> environment.put(e.getKey(), e.getValue().getAsString()));
        var states = ShaderWorkspace.assemble(root, environment, java.util.List.of(args).contains("--rebuild"), java.util.List.of(args).contains("--check"));
        states.forEach(s -> System.out.println(s.id() + ": " + s.state() + (s.detail().isEmpty() ? "" : " — " + s.detail())));
        if (states.stream().anyMatch(s -> s.state().equals("failed"))) throw new IllegalStateException("Shader build failed");
    }
}
