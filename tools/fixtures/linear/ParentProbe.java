package linearreview;
import com.google.gson.*;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.physics.constraint.*;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.nio.file.*;
import org.joml.Vector3d;
import org.joml.Quaterniond;
@Mod("linear_review_probe")
public class ParentProbe {
 private boolean start;private int tick;private ServerSubLevel parent,ship;private FixedConstraintHandle hold;private BlockPos base,carriage,control;private Vec3 freeStart,lockStart,releaseStart;private Quaterniond relative;private final JsonObject report=new JsonObject();
 public ParentProbe(){NeoForge.EVENT_BUS.addListener((ServerStartedEvent e)->start=true);NeoForge.EVENT_BUS.addListener(this::tick);}
 private Vec3 point(ServerLevel level){return Sable.HELPER.projectOutOfSubLevel(level,carriage.getCenter());}
 private Vec3 relativePoint(ServerLevel level){return parent.logicalPose().transformPositionInverse(point(level));}
 private Quaterniond relativeOrientation(){return new Quaterniond(parent.logicalPose().orientation()).conjugate().mul(ship.logicalPose().orientation());}
 private Vector3d rail(){return parent.logicalPose().transformNormal(new Vector3d(0,0,1));}
 private boolean locked(ServerLevel level)throws Exception{var be=level.getBlockEntity(carriage);var f=be.getClass().getDeclaredField("isLocked");f.setAccessible(true);return f.getBoolean(be);}
 private FixedConstraintHandle hold(ServerLevel level){
  Vec3 world=Sable.HELPER.projectOutOfSubLevel(level,base.getCenter());
  return SubLevelContainer.getContainer(level).physicsSystem().getPipeline().addConstraint(parent,null,new FixedConstraintConfiguration(new Vector3d(base.getX()+0.5,base.getY()+0.5,base.getZ()+0.5),new Vector3d(world.x,world.y,world.z),new Quaterniond(parent.logicalPose().orientation()).conjugate()));
 }
 private void tick(ServerTickEvent.Post event){if(!start)return;try{
  var server=event.getServer();ServerLevel level=server.overworld();tick++;if(Boolean.getBoolean("linear.review.restore")){restore(event);return;}
  if(tick==1){
   base=new BlockPos(0,150,0);var positions=new java.util.ArrayList<BlockPos>();
   var casing=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("linearbearing:linear_casing")).defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE,AttachFace.FLOOR).setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH);
   for(int z=-6;z<=6;z++){var at=base.offset(0,0,z);positions.add(at);level.setBlock(at,casing,3);}
   var bearing=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("linearbearing:linear_bearing")).defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE,AttachFace.FLOOR).setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH);level.setBlock(base,bearing,3);
   parent=SubLevelAssemblyHelper.assembleBlocks(level,base,positions,BoundingBox3i.from(positions));
   if(parent==null)throw new AssertionError("No parent");base=parent.getPlot().getCenterBlock();
  }
  if(tick==2)RigidBodyHandle.of(parent).teleport(new Vector3d(parent.logicalPose().position()),new Quaterniond().rotationY(Math.PI/3));
  if(tick==3){
   hold=hold(level);level.setBlock(base.above(),Blocks.STONE.defaultBlockState(),3);var block=level.getBlockState(base).getBlock();var state=level.getBlockState(base);
   var fake=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);fake.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
   var method=block.getClass().getDeclaredMethod("useWithoutItem",BlockState.class,Level.class,BlockPos.class,Player.class,BlockHitResult.class);method.setAccessible(true);method.invoke(block,state,level,base,fake,new BlockHitResult(base.getCenter(),Direction.UP,base,false));
   for(var body:SubLevelContainer.getContainer(level).getAllSubLevels()){
    if(body==parent)continue;var center=body.getPlot().getCenterBlock();
    for(int x=-1;x<=1;x++)for(int y=-2;y<=1;y++)for(int z=-1;z<=1;z++){
     var at=center.offset(x,y,z);var be=level.getBlockEntity(at);if(be!=null&&be.getClass().getSimpleName().equals("LinearMovingBlockEntity")){ship=(ServerSubLevel)body;carriage=at;}
    }
   }
   if(ship==null)throw new AssertionError("No child carriage");control=carriage.above();report.addProperty("parentOrientation",parent.logicalPose().orientation().toString());
  }
  if(tick==25){freeStart=point(level);RigidBodyHandle.of(ship).applyLinearImpulse(rail().mul(6));}
  if(tick==32){double moved=point(level).distanceTo(freeStart);report.addProperty("freeTravel",moved);if(moved<0.01)throw new AssertionError("No relative free travel");level.setBlock(control,Blocks.REDSTONE_BLOCK.defaultBlockState(),3);}
  if(tick==40){
   if(!locked(level))throw new AssertionError("Parent carriage did not lock");
   if(!SubLevelHelper.getConnectedChain(parent).contains(ship)||!SubLevelHelper.getConnectedChain(ship).contains(parent))throw new AssertionError("Missing reciprocal Sable actor relation");report.addProperty("reciprocalActorChain",true);
   lockStart=relativePoint(level);relative=relativeOrientation();hold.remove();RigidBodyHandle.of(parent).applyAngularImpulse(new Vector3d(0,200,0));RigidBodyHandle.of(ship).applyLinearImpulse(rail().mul(6));
  }
  if(tick==48){
   double moved=relativePoint(level).distanceTo(lockStart);double angle=2*Math.acos(Math.min(1,Math.abs(relative.dot(relativeOrientation()))));report.addProperty("relativeLoadTravel",moved);report.addProperty("relativeAngleError",angle);report.addProperty("movingParentOrientation",parent.logicalPose().orientation().toString());
   if(moved>0.03||angle>0.02)throw new AssertionError("Relative brake failed "+report);hold=hold(level);
   if(Boolean.getBoolean("linear.review.reopen")){
    report.addProperty("parentId",parent.getUniqueId().toString());report.addProperty("shipId",ship.getUniqueId().toString());report.addProperty("base",base.asLong());report.addProperty("carriage",carriage.asLong());
    var anchor=relativePoint(level);var savedPoint=new JsonArray();savedPoint.add(anchor.x);savedPoint.add(anchor.y);savedPoint.add(anchor.z);report.add("relativePoint",savedPoint);
    Files.writeString(Path.of(System.getProperty("linear.review.output")),new GsonBuilder().setPrettyPrinting().create().toJson(report));server.halt(false);start=false;
   }

  }
  if(tick==50)level.setBlock(control,Blocks.STONE.defaultBlockState(),3);
  if(tick==55){
   if(locked(level))throw new AssertionError("Parent brake did not release");
   if(SubLevelHelper.getConnectedChain(parent).contains(ship)||SubLevelHelper.getConnectedChain(ship).contains(parent))throw new AssertionError("Stale actor link");report.addProperty("actorChainReleased",true);
   releaseStart=relativePoint(level);RigidBodyHandle.of(ship).applyLinearImpulse(rail().mul(6));
  }
  if(tick==63){double moved=relativePoint(level).distanceTo(releaseStart);report.addProperty("releasedTravel",moved);if(moved<0.01)throw new AssertionError("No released relative travel");}
  if(tick==65)level.setBlock(control,Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
  if(tick==72){if(!locked(level))throw new AssertionError("Second parent lock failed");var under=BlockPos.containing(relativePoint(level));if(!BuiltInRegistries.BLOCK.getKey(level.getBlockState(under).getBlock()).toString().equals("linearbearing:linear_casing"))throw new AssertionError("No parent rail");level.destroyBlock(under,false);}
  if(tick==80){if(locked(level))throw new AssertionError("Parent rail removal retained brake");report.addProperty("railRemovalReleased",true);Files.writeString(Path.of(System.getProperty("linear.review.output")),new GsonBuilder().setPrettyPrinting().create().toJson(report));server.halt(false);start=false;}
 }catch(Throwable e){e.printStackTrace();try{Files.writeString(Path.of(System.getProperty("linear.review.output")+".failure"),e.toString()+"\n"+report);}catch(Exception f){f.printStackTrace();}start=false;event.getServer().halt(false);}} private void restore(ServerTickEvent.Post event)throws Exception{
  var level=event.getServer().overworld();var previous=JsonParser.parseString(Files.readString(Path.of(System.getProperty("linear.review.previous")))).getAsJsonObject();
  var container=SubLevelContainer.getContainer(level);parent=(ServerSubLevel)container.getSubLevel(java.util.UUID.fromString(previous.get("parentId").getAsString()));ship=(ServerSubLevel)container.getSubLevel(java.util.UUID.fromString(previous.get("shipId").getAsString()));
  if(parent==null||ship==null){if(tick>60)throw new AssertionError("Saved parent/child were not loaded");return;}
  base=BlockPos.of(previous.get("base").getAsLong());carriage=BlockPos.of(previous.get("carriage").getAsLong());control=carriage.above();
  if(tick==20){
   if(!locked(level))throw new AssertionError("Saved powered carriage did not rebuild brake");
   if(!SubLevelHelper.getConnectedChain(parent).contains(ship)||!SubLevelHelper.getConnectedChain(ship).contains(parent))throw new AssertionError("Restored actor relation missing");
   var point=previous.getAsJsonArray("relativePoint");var expected=new Vec3(point.get(0).getAsDouble(),point.get(1).getAsDouble(),point.get(2).getAsDouble());double moved=relativePoint(level).distanceTo(expected);
   if(moved>0.03)throw new AssertionError("Restored relative pose changed "+moved);report.addProperty("restoredRelativeError",moved);report.addProperty("restoredBrake",true);report.addProperty("restoredActorChain",true);
   level.setBlock(control,Blocks.STONE.defaultBlockState(),3);
  }
  if(tick==25){if(locked(level))throw new AssertionError("Restored brake did not release");report.addProperty("restoredRelease",true);Files.writeString(Path.of(System.getProperty("linear.review.output")),new GsonBuilder().setPrettyPrinting().create().toJson(report));event.getServer().halt(false);start=false;}
 }

}
