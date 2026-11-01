package linearreview;

import com.enxv.aerouniversaljoint.content.*;
import com.google.gson.*;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3d;
import java.nio.file.*;
import java.util.*;

/** Finite native observations of supplied-free actuation and separately driven conversion. */
@Mod("linear_review_probe")
public final class LinkagePhysicsProbe {
    private boolean started;
    private int ticks;
    private final JsonObject report = new JsonObject();
    private final HydraulicConnectionHeadBlockEntity[] heads = new HydraulicConnectionHeadBlockEntity[2];
    private final ServerSubLevel[] bodies = new ServerSubLevel[2];
    private final BlockPos[] plots = new BlockPos[2];
    private final double[] startX = new double[2], peakExtension = new double[2];
    private DampingStressBearingBlockEntity converter;
    private ServerSubLevel drivenRotor;
    private double peakRpm, peakCapacity, peakAngularVelocity, peakSu;
    public LinkagePhysicsProbe() {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> started=true);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener((dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent e) -> {
            if(drivenRotor!=null && !drivenRotor.isRemoved()) {
                var handle=RigidBodyHandle.of(drivenRotor);
                if(handle.isValid()) {
                    var change=new Vector3d(0,2,0).sub(handle.getAngularVelocity());
                    handle.addLinearAndAngularVelocity(new Vector3d(),change);
                }
            }
        });
    }
    private void tick(ServerTickEvent.Post event) {
        if (!started) return;
        try {
            var level = event.getServer().overworld(); ticks++;
            for(int x : new int[]{100,200,400}) level.setChunkForced(x >> 4,0,true);
            if(ticks==1) {
                for (int i=0;i<2;i++) {
                    var at = new BlockPos(100+i*100,150,0); var other = at.east(5);
                    var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("aeronautics_utility_objects:hydraulic_connection_head"));
                    level.setBlock(at,block.defaultBlockState().setValue(BlockStateProperties.FACING,Direction.EAST),3);
                    level.setBlock(other,block.defaultBlockState().setValue(BlockStateProperties.FACING,Direction.WEST),3);
                    bodies[i]=SubLevelAssemblyHelper.assembleBlocks(level,other,List.of(other),BoundingBox3i.from(List.of(other)));
                    plots[i]=bodies[i].getPlot().getCenterBlock();
                    heads[i]=(HydraulicConnectionHeadBlockEntity)level.getBlockEntity(at);
                    var peer=(HydraulicConnectionHeadBlockEntity)level.getBlockEntity(plots[i]);
                    var result=heads[i].createMutualLink(peer,false,i==1);
                    if(!heads[i].hasLink()) throw new AssertionError("link failed "+result);
                    startX[i]=bodies[i].logicalPose().transformPosition(plots[i].getCenter()).x;
                    if(i==0) heads[i].setSettingsAndMirror(0,false,80,64);
                    else heads[i].setGiantHydraulicSettingsAndMirror(24,false,80,350,10,150);
                }
                var at=new BlockPos(400,150,0);
                var block=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("aeronautics_utility_objects:damping_stress_bearing"));
                level.setBlock(at,block.defaultBlockState().setValue(BlockStateProperties.FACING,Direction.UP),3);
                level.setBlock(at.above(),Blocks.STONE.defaultBlockState(),3);
                converter=(DampingStressBearingBlockEntity)level.getBlockEntity(at);
                converter.assemble();
                if(!converter.isAssembled()) throw new AssertionError("converter did not assemble");
            }
            if(ticks>5) {
                for(int i=0;i<2;i++) peakExtension[i]=Math.max(peakExtension[i],bodies[i].logicalPose().transformPosition(plots[i].getCenter()).x-startX[i]);
                var rotor=SubLevelContainer.getContainer(level).getSubLevel(converter.getSubLevelID());
                if(rotor instanceof ServerSubLevel body) {
                    drivenRotor=body;
                    var handle=RigidBodyHandle.of(body);
                    peakAngularVelocity=Math.max(peakAngularVelocity,handle.getAngularVelocity().length());
                }
                peakRpm=Math.max(peakRpm,Math.abs(converter.getOutputKinetics().getGeneratedSpeed()));
                var output=converter.getOutputKinetics();
                peakCapacity=Math.max(peakCapacity,output.calculateAddedStressCapacity());
                peakSu=Math.max(peakSu,output.calculateAddedStressCapacity()*Math.abs(output.getTheoreticalSpeed()));
            }
            if(ticks==100) {
                if(bodies[0].isRemoved() || bodies[1].isRemoved() || peakExtension[0]>20 || peakExtension[1]>20)
                    throw new AssertionError("invalid fixture body/coordinate state");
                report.addProperty("normalActuatorPeakExtension",peakExtension[0]);
                report.addProperty("giantActuatorPeakExtension",peakExtension[1]);
                report.addProperty("converterStillAssembled",converter.isAssembled());
                report.addProperty("converterRotorPeakAngularVelocity",peakAngularVelocity);
                report.addProperty("externallyDrivenConverterPeakRpm",peakRpm);
                report.addProperty("externallyDrivenConverterPeakCapacityPerRpm",peakCapacity);
                report.addProperty("externallyDrivenConverterPeakSu",peakSu);
                report.addProperty("normalLinkStillPresent",heads[0].hasLink());
                report.addProperty("giantLinkStillPresent",heads[1].hasLink());
                report.addProperty("externalSupplyBlocks",0);
                report.addProperty("closedLoopTested",false);
                report.addProperty("converterDrive","fixture-imposed 2 rad/s before each physics tick");
                Files.writeString(Path.of(System.getProperty("linear.review.output")),new GsonBuilder().setPrettyPrinting().create().toJson(report));
                event.getServer().halt(false);
            }
        } catch(Throwable failure) {
            failure.printStackTrace();
            try {Files.writeString(Path.of(System.getProperty("linear.review.output")+".failure"),failure.toString());}catch(Exception ignored){}
            event.getServer().halt(false);
        }
    }
}
