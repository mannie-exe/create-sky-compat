package linearreview;

import com.enxv.aerouniversaljoint.content.HydraulicConnectionHeadBlockEntity;
import com.google.gson.*;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.constraint.*;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
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
import org.joml.Quaterniond;
import org.joml.Vector3d;
import java.nio.file.*;
import java.util.*;

/** Real assembly/move/reopen checks with normal, reversed and rotated/off-center links. */
@Mod("linear_review_probe")
public final class LinkageLifecycleProbe {
    private boolean started;
    private int tick;
    private final JsonObject report = new JsonObject();
    private final BlockPos[] a = new BlockPos[3], b = new BlockPos[3];
    private final ServerSubLevel[] bodies = new ServerSubLevel[3];
    private ServerSubLevel parent;
    public LinkageLifecycleProbe() {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e)->started=true);
        NeoForge.EVENT_BUS.addListener(this::tick);
    }
    private HydraulicConnectionHeadBlockEntity head(ServerLevel level, BlockPos pos) {
        return (HydraulicConnectionHeadBlockEntity)level.getBlockEntity(pos);
    }
    private void tick(ServerTickEvent.Post event) {
        if(!started)return;
        try {
            var level=event.getServer().overworld();tick++;
            for(int x:new int[]{100,140,180})level.setChunkForced(x>>4,0,true);
            boolean restore=Boolean.getBoolean("linear.review.restore");
            if(tick==1) {
                if(restore) {
                    var saved=JsonParser.parseString(Files.readString(Path.of(System.getProperty("linear.review.previous")))).getAsJsonObject();
                    for(int i=0;i<3;i++) {
                        a[i]=BlockPos.of(saved.get("a"+i).getAsLong()); b[i]=BlockPos.of(saved.get("b"+i).getAsLong());
                        // Accessing the saved plot is intentional fixture loading, not adapter force-loading.
                        level.getChunkAt(a[i]);level.getChunkAt(b[i]);
                    }
                } else {
                    var block=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("aeronautics_utility_objects:hydraulic_connection_head"));
                    for(int i=0;i<3;i++) {
                        a[i]=new BlockPos(100+i*40,150,0);var moving=a[i].east(5);
                        level.setBlock(a[i],block.defaultBlockState().setValue(BlockStateProperties.FACING,Direction.EAST),3);
                        level.setBlock(moving,block.defaultBlockState().setValue(BlockStateProperties.FACING,Direction.WEST),3);
                        level.setBlock(moving.above(),Blocks.STONE.defaultBlockState(),3);
                        var positions=List.of(moving,moving.above());
                        bodies[i]=SubLevelAssemblyHelper.assembleBlocks(level,moving,positions,BoundingBox3i.from(positions));
                        b[i]=bodies[i].getPlot().getCenterBlock();
                        if(i<2) {
                            var result=i==0?head(level,a[i]).createMutualLink(head(level,b[i]))
                                    :head(level,b[i]).createMutualLink(head(level,a[i]),false,true);
                            require(head(level,a[i]).hasLink(),"initial link failed "+result);
                            hold(level,bodies[i],b[i]);
                        } else {
                            parent=SubLevelAssemblyHelper.assembleBlocks(level,a[i],List.of(a[i]),BoundingBox3i.from(List.of(a[i])));
                            a[i]=parent.getPlot().getCenterBlock();
                            var origin=new Vector3d(parent.logicalPose().position());
                            var rotation=new Quaterniond().rotationY(Math.PI/2);
                            var offset=new Vector3d(bodies[i].logicalPose().position()).sub(origin);
                            rotation.transform(offset);
                            RigidBodyHandle.of(parent).teleport(origin,rotation);
                            RigidBodyHandle.of(bodies[i]).teleport(offset.add(origin),rotation);
                        }
                    }
                }
            }
            if(!restore && tick==3) {
                var result=head(level,a[2]).createMutualLink(head(level,b[2]));
                require(head(level,a[2]).hasLink(),"rotated link failed "+result);
                hold(level,parent,a[2]);hold(level,bodies[2],b[2]);
                report.addProperty("rotatedOffCenterShipLink",true);
            }
            if(!restore && tick==10) {
                // Move an already linked world endpoint into a second actual body.
                var old=a[0];
                var body=SubLevelAssemblyHelper.assembleBlocks(level,old,List.of(old),BoundingBox3i.from(List.of(old)));
                a[0]=body.getPlot().getCenterBlock();hold(level,body,a[0]);
                report.addProperty("linkedEndpointAssembly",true);
            }
            if(tick==40) {
                for(int i=0;i<3;i++)checkPair(level,i);
                report.addProperty(restore?"coldReopenPairs":"livePairs",3);
                if(!restore) {
                    for(int i=0;i<3;i++) {
                        report.addProperty("a"+i,a[i].asLong());report.addProperty("b"+i,b[i].asLong());
                        report.addProperty("assembledMass"+i,((Number)bodies[i].getMassTracker().getClass().getMethod("getMass").invoke(bodies[i].getMassTracker())).doubleValue());
                    }
                    var weights=new JsonArray();
                    for(var block:BuiltInRegistries.BLOCK) {
                        var id=BuiltInRegistries.BLOCK.getKey(block);
                        if(!id.getNamespace().equals("aeronautics_utility_objects") && !Set.of("minecraft:stone","create:mechanical_bearing").contains(id.toString()))continue;
                        var state=block.defaultBlockState();var row=new JsonObject();row.addProperty("block",id.toString());
                        row.addProperty("mass",PhysicsBlockPropertyHelper.getMass(level,new BlockPos(0,150,0),state));weights.add(row);
                    }
                    report.add("blockMasses",weights);
                } else {
                    level.destroyBlock(b[0],false);
                }
            }
            if(!restore && tick==55) {
                var pipeline=SubLevelContainer.getContainer(level).physicsSystem().getPipeline();
                for(var body:bodies) {
                    pipeline.wakeUp(body);
                    RigidBodyHandle.of(body).applyAngularImpulse(new Vector3d(0,.01,0));
                }
                report.addProperty("wakeRequestedAfterIdle",true);
            }
            if(tick==70) {
                if(restore) {
                    require(!head(level,a[0]).hasLink(),"removed endpoint left a live link");
                    checkPair(level,1);checkPair(level,2);
                    report.addProperty("endpointRemovalIsolated",true);
                } else for(int i=0;i<3;i++)checkPair(level,i);
                int bodyCount=0;for(var body:SubLevelContainer.getContainer(level).getAllSubLevels())if(!body.isRemoved())bodyCount++;
                require(bodyCount==5,"unexpected body duplication/removal: "+bodyCount);
                report.addProperty("bodyCount",bodyCount);
                Files.writeString(Path.of(System.getProperty("linear.review.output")),new GsonBuilder().setPrettyPrinting().create().toJson(report));
                event.getServer().halt(false);
            }
        } catch(Throwable failure) {
            failure.printStackTrace();
            try {Files.writeString(Path.of(System.getProperty("linear.review.output")+".failure"),failure.toString());}catch(Exception ignored){}
            event.getServer().halt(false);
        }
    }
    private void checkPair(ServerLevel level,int i) throws Exception {
        var own=head(level,a[i]);var other=head(level,b[i]);
        require(own!=null&&other!=null&&own.hasLink()&&other.hasLink()&&own.references(other)&&other.references(own),"broken mutual references "+i);
        var field=HydraulicConnectionHeadBlockEntity.class.getDeclaredField("constraintHandle");field.setAccessible(true);
        int live=0;
        for(var entity:List.of(own,other)) {
            var handle=(PhysicsConstraintHandle)field.get(entity);
            if(handle!=null&&handle.isValid())live++;
        }
        require(live==1,"expected one live rod constraint, found "+live+" in pair "+i);
    }
    private static void hold(ServerLevel level,ServerSubLevel body,BlockPos plot) {
        var world=body.logicalPose().transformPosition(plot.getCenter());
        SubLevelContainer.getContainer(level).physicsSystem().getPipeline().addConstraint(body,null,
                new FixedConstraintConfiguration(new Vector3d(plot.getX()+.5,plot.getY()+.5,plot.getZ()+.5),
                        new Vector3d(world.x,world.y,world.z),new Quaterniond(body.logicalPose().orientation()).conjugate()));
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
