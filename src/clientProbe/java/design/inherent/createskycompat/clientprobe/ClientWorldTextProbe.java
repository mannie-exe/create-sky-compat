package design.inherent.createskycompat.clientprobe;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import design.inherent.createskycompat.client.WorldTextScope;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Ordinary world-font control and bounded native pipeline-lifetime observations. */
final class ClientWorldTextProbe {
    private boolean control;
    private int controlFrames;
    private List<ShaderInstance> previous = List.of();

    ClientWorldTextProbe() { NeoForge.EVENT_BUS.addListener(this::render); }
    void control(boolean enabled) { control = enabled; }

    private void render(RenderLevelStageEvent event) {
        if (!control || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        var pose = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        controlFrames++;
        pose.pushPose();
        try {
            pose.translate(-camera.x, 289 - camera.y, -camera.z);
            pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            pose.scale(0.1f, -0.1f, 0.1f);
            var label = Component.literal("CONTROL").withStyle(ChatFormatting.RED);
            mc.font.drawInBatch(label, -mc.font.width(label) / 2f, 0, 0xffffffff, false,
                    pose.last().pose(), mc.renderBuffers().bufferSource(), Font.DisplayMode.NORMAL,
                    0xff000000, LightTexture.FULL_BRIGHT);
        } finally { pose.popPose(); }
    }

    JsonObject snapshot(boolean remember) throws Exception {
        var report = new JsonObject();
        report.addProperty("outsideScope", WorldTextScope.active());
        report.addProperty("unicode", Minecraft.getInstance().options.forceUnicodeFont().get());
        report.addProperty("controlFrames", controlFrames);
        report.addProperty("textEnabled", design.inherent.createskycompat.OptionalIntegrations.enabled(design.inherent.createskycompat.Feature.TEXT_DISPLAY_LIGHTING));
        report.addProperty("nametagEnabled", design.inherent.createskycompat.OptionalIntegrations.enabled(design.inherent.createskycompat.Feature.NAMETAG_LIGHTING));
        var server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) report.addProperty("textDisplayCount", server.submit(() -> server.overworld()
                .getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class,
                        new net.minecraft.world.phys.AABB(-20, 280, -20, 20, 300, 20),
                        entity -> entity.getTags().contains("world_text_review")).size()).get(5, java.util.concurrent.TimeUnit.SECONDS));
        var pipeline = Iris.getPipelineManager().getPipeline().orElse(null);
        report.addProperty("pipeline", pipeline == null ? "none" : pipeline.getClass().getName());
        report.addProperty("pipelineIdentity", System.identityHashCode(pipeline));
        if (!previous.isEmpty()) {
            var id = com.mojang.blaze3d.shaders.Program.class.getDeclaredMethod("getId"); id.setAccessible(true);
            boolean closed = true;
            for (var shader : previous) closed &= (int) id.invoke(shader.getVertexProgram()) == -1 && (int) id.invoke(shader.getFragmentProgram()) == -1;
            report.addProperty("previousProgramsClosed", closed);
        }
        if (pipeline instanceof IrisRenderingPipeline iris) {
            report.addProperty("ordinaryTextProgram", iris.getShaderMap().getShader(ShaderKey.TEXT).getClass().getName());
            try {
                var shaders = pipeline.getClass().getDeclaredField("createSky$textShaders"); shaders.setAccessible(true);
                var types = pipeline.getClass().getDeclaredField("createSky$textTypes"); types.setAccessible(true);
                var programs = (Map<?, ?>) shaders.get(pipeline);
                var keys = new JsonArray();
                programs.keySet().forEach(key -> keys.add(key.toString()));
                report.add("programKeys", keys);
                var values = programs.values();
                report.addProperty("programCount", values.size());
                report.addProperty("wrapperCount", ((Map<?, ?>) types.get(pipeline)).size());
                if (remember) {
                    previous = new ArrayList<>();
                    for (Object shader : values) previous.add((ShaderInstance) shader);
                }
            } catch (NoSuchFieldException absent) { report.addProperty("backendSelected", false); }
        }
        return report;
    }
}
