package linearreview;
import com.google.gson.*;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
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
@Mod("linear_review_probe")
public class CarriageProbe {
 private void jade(net.minecraft.world.level.block.entity.BlockEntity entity,boolean expected)throws Exception{
  if(!net.neoforged.fml.ModList.get().isLoaded("jade"))return;
  var accessor=Class.forName("snownee.jade.api.BlockAccessor");
  Object proxy=java.lang.reflect.Proxy.newProxyInstance(accessor.getClassLoader(),new Class[]{accessor},(object,method,args)->method.getName().equals("getBlockEntity")?entity:null);
  var type=Class.forName("design.inherent.createskycompat.linear.LinearJadeProvider");var provider=type.getField("INSTANCE").get(null);var data=new net.minecraft.nbt.CompoundTag();
  type.getMethod("appendServerData",net.minecraft.nbt.CompoundTag.class,accessor).invoke(provider,data,proxy);
  var state=data.getCompound("create_sky_compat:linear");if(!state.contains("braked")||state.getBoolean("braked")!=expected)throw new AssertionError("Jade brake status disagrees with native constraint");
  report.addProperty(expected?"jadeBraked":"jadeReleased",true);
 }
 private boolean start;private int tick;private ServerSubLevel ship;private BlockPos carriage;private Vec3 initial, freeStart, lockStart, releaseStart;private Direction normal=Direction.UP, rail=Direction.SOUTH;private BlockPos control;private final BlockPos connector=new BlockPos(20,150,20);private final JsonObject report=new JsonObject();
 public CarriageProbe(){NeoForge.EVENT_BUS.addListener((ServerStartedEvent event)->start=true);NeoForge.EVENT_BUS.addListener(this::tick);}
 private void tick(ServerTickEvent.Post event){if(!start)return;try{
  var server=event.getServer();ServerLevel level=server.overworld();tick++;
  if(tick==1){
   BlockPos base=new BlockPos(0,150,0);var block=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("linearbearing:linear_bearing"));
   String scenario=System.getProperty("linear.review.case","floor");report.addProperty("case",scenario);
   AttachFace face=scenario.equals("ceiling")?AttachFace.CEILING:scenario.startsWith("wall")?AttachFace.WALL:AttachFace.FLOOR;
   Direction facing=scenario.equals("ceiling")?Direction.EAST:Direction.NORTH;
   boolean back=scenario.equals("wall-back");normal=face==AttachFace.FLOOR?Direction.UP:face==AttachFace.CEILING?Direction.DOWN:facing;
   rail=face==AttachFace.WALL?(back?facing.getClockWise():Direction.UP):(facing.getAxis()==Direction.Axis.X?Direction.EAST:Direction.SOUTH);
   var state=block.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE,face).setValue(BlockStateProperties.HORIZONTAL_FACING,facing).setValue(com.bearing.linearbearing.blocks.LinearBearingBlock.SIDE,back?com.bearing.linearbearing.blocks.LinearBearingBlock.SliderSide.BACK:com.bearing.linearbearing.blocks.LinearBearingBlock.SliderSide.FRONT);
   var casing=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("linearbearing:linear_casing")).defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE,face).setValue(BlockStateProperties.HORIZONTAL_FACING,facing).setValue(com.bearing.linearbearing.blocks.LinearCasingBlock.SIDE,back?com.bearing.linearbearing.blocks.LinearCasingBlock.SliderSide.BACK:com.bearing.linearbearing.blocks.LinearCasingBlock.SliderSide.FRONT);
   for(int distance=-32;distance<=32;distance++)if(distance!=0)level.setBlock(base.relative(rail,distance),casing,3);
   level.setBlock(base,state,3);level.setBlock(base.relative(normal),Blocks.STONE.defaultBlockState(),3);
   var fake=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);fake.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
   var use=block.getClass().getDeclaredMethod("useWithoutItem",BlockState.class,Level.class,BlockPos.class,Player.class,BlockHitResult.class);use.setAccessible(true);use.invoke(block,state,level,base,fake,new BlockHitResult(base.getCenter(),normal,base,false));
   for(var body:SubLevelContainer.getContainer(level).getAllSubLevels()){
    var center=body.getPlot().getCenterBlock();
    for(int x=-1;x<=1;x++)for(int y=-2;y<=1;y++)for(int z=-1;z<=1;z++){
     var pos=center.offset(x,y,z);var entity=level.getBlockEntity(pos);
     if(entity!=null&&entity.getClass().getSimpleName().equals("LinearMovingBlockEntity")){ship=(ServerSubLevel)body;carriage=pos;}
    }
   }
   var dock=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("simulated:docking_connector"));
   level.setBlock(connector.below(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
   level.setBlock(connector,dock.defaultBlockState().setValue(BlockStateProperties.FACING,Direction.EAST).setValue(BlockStateProperties.POWERED,true),3);
   if(ship==null)throw new AssertionError("No carriage");control=carriage.relative(normal);initial=Sable.HELPER.projectOutOfSubLevel(level,carriage.getCenter());report.addProperty("initial",initial.toString());
  }
  if(tick==25){
   if(!BuiltInRegistries.BLOCK.getKey(level.getBlockState(connector.east()).getBlock()).toString().equals("simulated:paired_docking_connector"))throw new AssertionError("Native docking control did not extend");
   report.addProperty("nativeDockingExtended",true);
   var current=Sable.HELPER.projectOutOfSubLevel(level,carriage.getCenter());report.addProperty("freeVerticalDrift",current.y-initial.y);freeStart=current;
   dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle.of(ship).applyLinearImpulse(new org.joml.Vector3d(rail.getStepX()*6,rail.getStepY()*6,rail.getStepZ()*6));
  }
  if(tick==32){
   double moved=Math.abs(Sable.HELPER.projectOutOfSubLevel(level,carriage.getCenter()).subtract(freeStart).dot(Vec3.atLowerCornerOf(rail.getNormal())));report.addProperty("freeTravel",moved);if(moved<0.01)throw new AssertionError("Carriage did not slide freely: "+moved);
   level.setBlock(control,Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
  }
  if(tick==40){
   var entity=level.getBlockEntity(carriage);var field=entity.getClass().getDeclaredField("isLocked");field.setAccessible(true);if(!field.getBoolean(entity))throw new AssertionError("Brake did not lock");
   jade(entity,true);
   lockStart=Sable.HELPER.projectOutOfSubLevel(level,carriage.getCenter());dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle.of(ship).applyLinearImpulse(new org.joml.Vector3d(rail.getStepX()*6,rail.getStepY()*6,rail.getStepZ()*6));
  }
  if(tick==48){double moved=Sable.HELPER.projectOutOfSubLevel(level,carriage.getCenter()).distanceTo(lockStart);report.addProperty("brakedLoadTravel",moved);if(moved>0.03)throw new AssertionError("Brake slipped under load "+moved);}
  if(tick==50){level.setBlock(control,Blocks.STONE.defaultBlockState(),3);level.setBlock(connector.below(),Blocks.AIR.defaultBlockState(),3);}
  if(tick==55){
   var entity=level.getBlockEntity(carriage);var field=entity.getClass().getDeclaredField("isLocked");field.setAccessible(true);if(field.getBoolean(entity))throw new AssertionError("Brake did not release");
   jade(entity,false);
   releaseStart=Sable.HELPER.projectOutOfSubLevel(level,carriage.getCenter());dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle.of(ship).applyLinearImpulse(new org.joml.Vector3d(rail.getStepX()*6,rail.getStepY()*6,rail.getStepZ()*6));
  }
  if(tick==63){double moved=Math.abs(Sable.HELPER.projectOutOfSubLevel(level,carriage.getCenter()).subtract(releaseStart).dot(Vec3.atLowerCornerOf(rail.getNormal())));report.addProperty("releasedTravel",moved);if(moved<0.01)throw new AssertionError("Released carriage did not move "+moved);}
  if(tick==65)level.setBlock(control,Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
  if(tick==72){
   var entity=level.getBlockEntity(carriage);var field=entity.getClass().getDeclaredField("isLocked");field.setAccessible(true);if(!field.getBoolean(entity))throw new AssertionError("Second lock failed");
   var under=BlockPos.containing(Sable.HELPER.projectOutOfSubLevel(level,carriage.getCenter()));
   if(!BuiltInRegistries.BLOCK.getKey(level.getBlockState(under).getBlock()).toString().equals("linearbearing:linear_casing"))throw new AssertionError("No rail under locked carriage");level.destroyBlock(under,false);
  }
  if(tick==80){
   if(!level.getBlockState(connector.east()).isAir())throw new AssertionError("Native docking control did not retract");report.addProperty("nativeDockingRetracted",true);
   var entity=level.getBlockEntity(carriage);var field=entity.getClass().getDeclaredField("isLocked");field.setAccessible(true);if(field.getBoolean(entity))throw new AssertionError("Removed rail retained brake");report.addProperty("railRemovalReleased",true);
   Files.writeString(Path.of(System.getProperty("linear.review.output")),new GsonBuilder().setPrettyPrinting().create().toJson(report));server.halt(false);start=false;
  }
 }catch(Throwable e){e.printStackTrace();try{Files.writeString(Path.of(System.getProperty("linear.review.output")+".failure"),e.toString());}catch(Exception f){f.printStackTrace();}start=false;event.getServer().halt(false);}}
}
