package linearreview;

import com.google.gson.*;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import design.inherent.createskycompat.linear.TransmissionEndpoint;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.constraint.*;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.content.blocks.spring.SpringBlockEntity;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import java.nio.file.*;
import java.util.*;

@Mod("linear_review_probe")
public final class TransmissionProbe {
 private boolean started; private int tick; private ServerSubLevel ship, gyroShip, springShip;
 private BlockPos shipPort, gyro, springPort; private double peakGyro;
 private final JsonObject report = new JsonObject();
 public TransmissionProbe(){NeoForge.EVENT_BUS.addListener((ServerStartedEvent e)->started=true);NeoForge.EVENT_BUS.addListener(this::tick);}
 private static BlockState state(String id,Direction face){return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id)).defaultBlockState().setValue(BlockStateProperties.FACING,face);}
 private static KineticBlockEntity kinetic(ServerLevel level,BlockPos at){return (KineticBlockEntity)level.getBlockEntity(at);}
 private static void motor(ServerLevel level,BlockPos at,Direction face,int speed){level.setBlock(at,state("create:creative_motor",face),3);var motor=(CreativeMotorBlockEntity)level.getBlockEntity(at);motor.generatedSpeed.setValue(speed);motor.tick();motor.updateGeneratedRotation();}
 private static void pair(ServerLevel level,int x,boolean spring){
  var a=new BlockPos(x,150,0);var b=a.east(spring?5:3);String id=spring?"linearbearing:torsional_anchor":"linearbearing:magnetic_port";
  level.setBlock(a,state(id,Direction.EAST),3);level.setBlock(b,state(id,Direction.WEST),3);
  if(spring){level.setBlock(a.east(),state("simulated:spring",Direction.EAST),3);level.setBlock(b.west(),state("simulated:spring",Direction.WEST),3);var sa=(SpringBlockEntity)level.getBlockEntity(a.east());var sb=(SpringBlockEntity)level.getBlockEntity(b.west());sa.setPartnerPos(sb.getBlockPos(),null);sb.setPartnerPos(sa.getBlockPos(),null);}
  motor(level,a.west(),Direction.EAST,32);level.setBlock(b.east(),state("create:encased_fan",Direction.EAST),3);
 }
 private static void checkPair(ServerLevel level,int x,boolean spring){
  var a=kinetic(level,new BlockPos(x,150,0));var b=kinetic(level,new BlockPos(x+(spring?5:3),150,0));var load=kinetic(level,b.getBlockPos().east());float expected=spring?-32:32;
  if(a.getSpeed()!=32||b.getSpeed()!=expected||load.getSpeed()!=expected||a.getGeneratedSpeed()!=0||b.calculateAddedStressCapacity()!=0||!Objects.equals(a.network,b.network)||a.getOrCreateNetwork().sources.size()!=1)throw new AssertionError("Pair "+x+" "+a.getSpeed()+" / "+b.getSpeed()+" / "+load.getSpeed());
 }
 private static void hold(ServerLevel level,ServerSubLevel body,BlockPos anchor){var world=Sable.HELPER.projectOutOfSubLevel(level,anchor.getCenter());SubLevelContainer.getContainer(level).physicsSystem().getPipeline().addConstraint(body,null,new FixedConstraintConfiguration(new Vector3d(anchor.getX()+.5,anchor.getY()+.5,anchor.getZ()+.5),new Vector3d(world.x,world.y,world.z),new Quaterniond(body.logicalPose().orientation()).conjugate()));}
 private void tick(ServerTickEvent.Post event){if(!started)return;try{
  var level=event.getServer().overworld();tick++;boolean restore=Boolean.getBoolean("linear.review.restore");
  for(int x:new int[]{20,60,100,200,300,400})level.setChunkForced(x>>4,0,true);
  if(tick==1&&!restore){
   pair(level,100,false);pair(level,200,true);
   var samplePos=new BlockPos(80,150,0);level.setBlock(samplePos,state("linearbearing:gyroscopic_gearbox",Direction.UP),3);var sample=kinetic(level,samplePos);
   var method=sample.getClass().getDeclaredMethod("processDirectGeneratorPhysics",Quaterniond.class);method.setAccessible(true);method.invoke(sample,new Quaterniond());method.invoke(sample,new Quaterniond().rotationY(-.01));
   for(int i=0;i<30;i++)method.invoke(sample,new Quaterniond().rotationY(-.01));report.addProperty("stationarySampleRpm",sample.getGeneratedSpeed());if(sample.getGeneratedSpeed()!=0)throw new AssertionError("Stationary angular sample retained speed");

   var target=new BlockPos(23,150,0);level.setBlock(target,state("linearbearing:magnetic_port",Direction.NORTH),3);level.setBlock(target.south(),state("create:encased_fan",Direction.SOUTH),3);
   var positions=List.of(target,target.south());ship=SubLevelAssemblyHelper.assembleBlocks(level,target,positions,BoundingBox3i.from(positions));shipPort=ship.getPlot().getCenterBlock();
   level.setBlock(new BlockPos(20,150,0),state("linearbearing:magnetic_port",Direction.EAST),3);motor(level,new BlockPos(19,150,0),Direction.EAST,32);
   var origin=new BlockPos(60,150,0);level.setBlock(origin,state("linearbearing:gyroscopic_gearbox",Direction.UP),3);level.setBlock(origin.above(),state("linearbearing:redstone_converter",Direction.UP),3);level.setBlock(origin.east(),Blocks.STONE.defaultBlockState(),3);
   positions=List.of(origin,origin.above(),origin.east());gyroShip=SubLevelAssemblyHelper.assembleBlocks(level,origin,positions,BoundingBox3i.from(positions));gyro=gyroShip.getPlot().getCenterBlock();
   var springTarget=new BlockPos(405,150,0);level.setBlock(springTarget,state("linearbearing:torsional_anchor",Direction.WEST),3);level.setBlock(springTarget.west(),state("simulated:spring",Direction.WEST),3);level.setBlock(springTarget.east(),state("create:encased_fan",Direction.EAST),3);
   positions=List.of(springTarget.west(),springTarget.east(),springTarget);springShip=SubLevelAssemblyHelper.assembleBlocks(level,springTarget,positions,BoundingBox3i.from(positions));springPort=springShip.getPlot().getCenterBlock();
   level.setBlock(new BlockPos(400,150,0),state("linearbearing:torsional_anchor",Direction.EAST),3);level.setBlock(new BlockPos(401,150,0),state("simulated:spring",Direction.EAST),3);motor(level,new BlockPos(399,150,0),Direction.EAST,32);
   var first=(SpringBlockEntity)level.getBlockEntity(new BlockPos(401,150,0));
   var actorRows=new JsonArray();for(var actor:springShip.getPlot().getBlockEntityActors())if(actor instanceof net.minecraft.world.level.block.entity.BlockEntity be){actorRows.add(be.getBlockPos()+" "+be.getBlockState());if(be instanceof com.bearing.linearbearing.blockentity.TorsionalAnchorBlockEntity)springPort=be.getBlockPos();}report.add("springActors",actorRows);
   report.addProperty("springAnchorState",level.getBlockState(springPort).toString());report.addProperty("springHeadState",level.getBlockState(springPort.west()).toString());report.addProperty("worldSpringState",level.getBlockState(new BlockPos(401,150,0)).toString());var other=(SpringBlockEntity)level.getBlockEntity(springPort.west());if(first==null||other==null)throw new AssertionError("Missing spring head "+springPort+" "+report);first.setPartnerPos(other.getBlockPos(),springShip.getUniqueId());other.setPartnerPos(first.getBlockPos(),null);

   pair(level,300,false);motor(level,new BlockPos(304,150,0),Direction.WEST,64);
  }
  if(tick==3&&!restore){RigidBodyHandle.of(ship).teleport(new Vector3d(ship.logicalPose().position()),new Quaterniond().rotationY(Math.PI/2));}
  if(tick==4&&!restore){
   var actual=Sable.HELPER.projectOutOfSubLevel(level,shipPort.getCenter());var correction=new Vector3d(23.5-actual.x,150.5-actual.y,.5-actual.z);
   RigidBodyHandle.of(ship).teleport(new Vector3d(ship.logicalPose().position()).add(correction),new Quaterniond(ship.logicalPose().orientation()));
  }
  if(tick==5&&!restore){hold(level,ship,shipPort);hold(level,springShip,springPort);
   RigidBodyHandle.of(gyroShip).applyAngularImpulse(new Vector3d(0,20,0));}
  if(tick>6&&tick<35&&!restore){var sensor=kinetic(level,gyro);var converter=kinetic(level,gyro.above());peakGyro=Math.max(peakGyro,Math.abs(converter.getSpeed()));if(sensor.calculateAddedStressCapacity()!=0)throw new AssertionError("Gyro generated capacity");}
  if(tick==20){checkPair(level,100,false);checkPair(level,200,true);report.addProperty(restore?"coldPairsRestored":"pairsRunning",true);
   if(!restore){var a=kinetic(level,new BlockPos(300,150,0));var b=kinetic(level,new BlockPos(303,150,0));if(((TransmissionEndpoint)a).createSky$peer()!=null||((TransmissionEndpoint)b).createSky$peer()!=null)throw new AssertionError("Conflicting sources coupled");report.addProperty("conflictingSourcesRejected",true);level.destroyBlock(new BlockPos(304,150,0),false);}
  }
  if(tick==30&&!restore){
   var a=kinetic(level,new BlockPos(20,150,0));var b=kinetic(level,shipPort);var load=kinetic(level,shipPort.south());var sourceWorld=Sable.HELPER.projectOutOfSubLevel(level,a.getBlockPos().getCenter());var targetWorld=Sable.HELPER.projectOutOfSubLevel(level,b.getBlockPos().getCenter());
   report.addProperty("sourceWorld",sourceWorld.toString());report.addProperty("targetWorld",targetWorld.toString());report.addProperty("targetPose",ship.logicalPose().toString());report.addProperty("targetFacing",b.getBlockState().getValue(BlockStateProperties.FACING).toString());
   var ray=level.clip(new net.minecraft.world.level.ClipContext(sourceWorld.add(.51,0,0),sourceWorld.add(4.01,0,0),net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,(net.minecraft.world.entity.Entity)null));
   report.addProperty("rayType",ray.getType().toString());report.addProperty("rayPosition",ray.getBlockPos().toString());report.addProperty("rayDirection",ray.getDirection().toString());report.addProperty("rayState",level.getBlockState(ray.getBlockPos()).toString());
   report.addProperty("rotatedShipSource",a.getSpeed());report.addProperty("rotatedShipReceiver",b.getSpeed());report.addProperty("rotatedShipLoad",load.getSpeed());
   if(Math.abs(b.getSpeed())!=32||Math.abs(load.getSpeed())!=32||!Objects.equals(a.network,b.network))throw new AssertionError("Rotated ship transmission "+report);
   var springSource=kinetic(level,new BlockPos(400,150,0));var springReceiver=kinetic(level,springPort);var springLoad=kinetic(level,springPort.east());
   if(springReceiver.getSpeed()!=-32||springLoad.getSpeed()!=-32||!Objects.equals(springSource.network,springReceiver.network))throw new AssertionError("Ship spring link failed "+springReceiver.getSpeed()+" / "+springLoad.getSpeed());report.addProperty("shipSpringTransmission",true);
   var receiver=kinetic(level,new BlockPos(303,150,0));if(receiver.getSpeed()!=32)throw new AssertionError("Source removal did not permit recoupling "+receiver.getSpeed());report.addProperty("sourceRecoupled",true);
   if(peakGyro==0)throw new AssertionError("Physical gyro produced no converter signal");report.addProperty("physicalGyroPeakRpm",peakGyro);hold(level,gyroShip,gyro);
  }
  if(tick==40&&restore){
   for(int x:new int[]{100,200}){level.destroyBlock(new BlockPos(x-1,150,0),false);}
  }
  if(tick==55&&restore){
   for(int x:new int[]{100,200}){var a=kinetic(level,new BlockPos(x,150,0));var b=kinetic(level,new BlockPos(x+(x==200?5:3),150,0));if(a.getSpeed()!=0||b.getSpeed()!=0||kinetic(level,b.getBlockPos().east()).getSpeed()!=0)throw new AssertionError("Cold source removal left rotation");}
   report.addProperty("coldSourceRemoval",true);finish(event);
  }
  if(tick==65&&!restore){report.addProperty("stoppedGyroSpeed",kinetic(level,gyro).getGeneratedSpeed());report.addProperty("stoppedConverterSpeed",kinetic(level,gyro.above()).getSpeed());if(kinetic(level,gyro.above()).getSpeed()!=0)throw new AssertionError("Stopped gyro retained signal "+report);report.addProperty("stoppedGyro",true);finish(event);}
 }catch(Throwable failure){failure.printStackTrace();try{Files.writeString(Path.of(System.getProperty("linear.review.output")+".failure"),failure+"\n"+report);}catch(Exception nested){nested.printStackTrace();}started=false;event.getServer().halt(false);}}
 private void finish(ServerTickEvent.Post event)throws Exception{Files.writeString(Path.of(System.getProperty("linear.review.output")),new GsonBuilder().setPrettyPrinting().create().toJson(report));started=false;event.getServer().halt(false);}
}
