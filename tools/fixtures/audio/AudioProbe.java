package fixture;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.palm1.analogaudio.block.entity.RadioBlockEntity;
import com.palm1.analogaudio.item.CassetteData;
import com.palm1.analogaudio.registry.*;
import java.nio.file.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Loopback two-client fixture, using native cassette insertion and radio controls. */
@Mod("audio_review_probe")
public final class AudioProbe {
    private static final BlockPos RADIO = new BlockPos(0,-60,0);
    public AudioProbe() {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> {
            var level = event.getServer().overworld();
            for (int x=-4;x<=4;x++) for (int z=-4;z<=4;z++) level.setBlockAndUpdate(new BlockPos(x,-61,z),Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(RADIO,ModBlocks.RADIO.get().defaultBlockState());
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> event.getDispatcher().register(
                Commands.literal("audiofixture").requires(source -> source.hasPermission(4))
                .then(Commands.argument("action",StringArgumentType.word())
                .then(Commands.argument("player",StringArgumentType.word())
                .then(Commands.argument("variant",StringArgumentType.word()).executes(context -> {
                    var level = context.getSource().getServer().overworld();
                    var player = context.getSource().getServer().getPlayerList().getPlayerByName(StringArgumentType.getString(context,"player"));
                    if (player == null) throw new IllegalStateException("Fixture player absent");
                    var radio = (RadioBlockEntity)level.getBlockEntity(RADIO);
                    String variant=StringArgumentType.getString(context,"variant");
                    switch (StringArgumentType.getString(context,"action")) {
                        case "cassette" -> {
                            radio.setItem(0,ItemStack.EMPTY);
                            String url = variant.equals("online") ? "https://www.youtube.com/watch?v=jNQXAC9IVRw"
                                    : "http://127.0.0.1:18849/stagger.wav?round="+variant;
                            long duration = variant.equals("loop") ? 10000 : variant.equals("online") ? 19000 : 120000;
                            var stack = new ItemStack(ModItems.CASSETTE_TAPE.get());
                            stack.set(ModDataComponents.CASSETTE_DATA.get(), new CassetteData("fixture-"+variant,url,
                                    "Audio fixture",0x55bb77,.5f,duration,""));
                            player.getInventory().setItem(0,stack);
                            player.getInventory().selected=0;
                            player.inventoryMenu.broadcastChanges();
                            player.teleportTo(level,.5,-60,3.5,0,0);
                        }
                        case "pause" -> radio.setSettings(radio.getVolume(),radio.isLooping(),false,false);
                        case "resume" -> radio.setSettings(radio.getVolume(),radio.isLooping(),true,false);
                        case "loop" -> radio.setSettings(radio.getVolume(),true,radio.isPlaying(),false);
                        case "near" -> player.teleportTo(level,.5,-60,3.5,0,0);
                        case "tail" -> player.teleportTo(level,100.5,-60,0,0,0);
                        case "far" -> player.teleportTo(level,180.5,-60,0,0,0);
                        default -> throw new IllegalArgumentException("Unknown audio fixture operation");
                    }
                    return 1;
                }))))));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
            var level=event.getServer().overworld();
            if (level.getGameTime()%20!=0) return;
            var radio=(RadioBlockEntity)level.getBlockEntity(RADIO);
            if (radio==null) return;
            try { Files.writeString(Path.of("audio-server.json"),"{\"tick\":"+level.getGameTime()+",\"start\":"+radio.getStartTime()+",\"playing\":"+radio.isPlaying()+"}"); }
            catch(java.io.IOException failure) { throw new IllegalStateException(failure); }
        });
    }
}
