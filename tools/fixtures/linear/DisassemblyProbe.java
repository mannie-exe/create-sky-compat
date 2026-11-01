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
public class DisassemblyProbe {
 private boolean start, recovered;private int tick;private ServerSubLevel ship;private BlockPos base=new BlockPos(0,150,0),carriage;private final JsonObject report=new JsonObject();
 public DisassemblyProbe(){NeoForge.EVENT_BUS.addListener((ServerStartedEvent e)->start=true);NeoForge.EVENT_BUS.addListener(this::tick);}
 private net.minecraft.world.InteractionResult use(ServerLevel level,BlockPos at)throws Exception{
  var block=level.getBlockState(at).getBlock();var method=block.getClass().getDeclaredMethod("useWithoutItem",BlockState.class,Level.class,BlockPos.class,Player.class,BlockHitResult.class);method.setAccessible(true);
  var fake=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);fake.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
  return (net.minecraft.world.InteractionResult)method.invoke(block,level.getBlockState(at),level,at,fake,new BlockHitResult(at.getCenter(),Direction.UP,at,false));
 }
 private boolean locked(ServerLevel level)throws Exception{var be=level.getBlockEntity(carriage);var f=be.getClass().getDeclaredField("isLocked");f.setAccessible(true);return f.getBoolean(be);}
 private void cargo(ServerLevel level,BlockPos at){
  var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(at);var item=chest.getItem(0);
  if(!item.is(net.minecraft.world.item.Items.COPPER_INGOT)||item.getCount()!=4||!net.minecraft.network.chat.Component.literal("cargo").equals(item.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME)))throw new AssertionError("Cargo changed "+item);
 }
 private void tick(ServerTickEvent.Post event){if(!start)return;try{
  var server=event.getServer();ServerLevel level=server.overworld();tick++;
  String scenario=System.getProperty("linear.review.case","floor");boolean height=scenario.equals("height"), quarter=scenario.equals("quarter");
  if(tick==1){
   var casing=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("linearbearing:linear_casing")).defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE,AttachFace.FLOOR).setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH);
   for(int z=-2;z<=2;z++)if(z!=0)level.setBlock(base.offset(0,0,z),casing,3);
   var bearing=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("linearbearing:linear_bearing")).defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE,AttachFace.FLOOR).setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH);level.setBlock(base,bearing,3);
   level.setBlock(base.above(),Blocks.STONE.defaultBlockState(),3);level.setBlock(base.above().east(),Blocks.CHEST.defaultBlockState(),3);
   var stack=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COPPER_INGOT,4);stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("cargo"));
   ((net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(base.above().east())).setItem(0,stack);
   var glue=new com.simibubi.create.content.contraptions.glue.SuperGlueEntity(level,new AABB(base.above()).minmax(new AABB(base.above().east())));level.addFreshEntity(glue);
   if(use(level,base)!=net.minecraft.world.InteractionResult.SUCCESS)throw new AssertionError("Assembly failed");
   for(var body:SubLevelContainer.getContainer(level).getAllSubLevels()){
    var center=body.getPlot().getCenterBlock();for(int x=-2;x<=2;x++)for(int y=-2;y<=1;y++)for(int z=-1;z<=1;z++){
     var at=center.offset(x,y,z);var be=level.getBlockEntity(at);if(be!=null&&be.getClass().getSimpleName().equals("LinearMovingBlockEntity")){ship=(ServerSubLevel)body;carriage=at;}
    }
   }
   if(ship==null)throw new AssertionError("No carriage");cargo(level,carriage.above().east());
  }
  if(tick==5&&height){
   dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle.of(ship).teleport(new org.joml.Vector3d(ship.logicalPose().position()).add(0,169,0),new org.joml.Quaterniond(ship.logicalPose().orientation()));
   base=new BlockPos(0,319,0);var casing=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("linearbearing:linear_casing")).defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE,AttachFace.FLOOR).setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH);level.setBlock(base,casing,3);
  }
  if(tick==5&&quarter){
   var body=dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle.of(ship);
   body.teleport(new org.joml.Vector3d(ship.logicalPose().position()),new org.joml.Quaterniond().rotationY(Math.PI/2));
   var casing=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("linearbearing:linear_casing")).defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE,AttachFace.FLOOR).setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.WEST);level.setBlock(base,casing,3);
  }
  if(tick==10)level.setBlock(carriage.above(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
  if(tick==20){
   if(!locked(level))throw new AssertionError("No engaged brake");
   if(height){
    if(use(level,base)!=net.minecraft.world.InteractionResult.FAIL||!locked(level))throw new AssertionError("Height overflow was accepted");cargo(level,carriage.above().east());report.addProperty("heightOverflowRefused",true);
   }else{
    var targetChest=quarter?base.above().north():base.above().east();level.setBlock(targetChest,Blocks.BEDROCK.defaultBlockState(),3);
    if(use(level,base)!=net.minecraft.world.InteractionResult.FAIL||!locked(level)||!level.getBlockState(targetChest).is(Blocks.BEDROCK))throw new AssertionError("Blocked teardown changed state");cargo(level,carriage.above().east());report.addProperty("blockedDestinationPreserved",true);
    level.setBlock(targetChest,Blocks.AIR.defaultBlockState(),3);
    if(use(level,base)!=net.minecraft.world.InteractionResult.SUCCESS)throw new AssertionError("Clear teardown failed");
    if(!BuiltInRegistries.BLOCK.getKey(level.getBlockState(base).getBlock()).toString().equals("linearbearing:linear_bearing"))throw new AssertionError("Bearing was not recovered");cargo(level,targetChest);
    long rails=0,bearings=0;for(var at:BlockPos.betweenClosed(base.offset(-2,-1,-3),base.offset(3,3,3))){String id=BuiltInRegistries.BLOCK.getKey(level.getBlockState(at).getBlock()).toString();if(id.equals("linearbearing:linear_casing"))rails++;if(id.equals("linearbearing:linear_bearing"))bearings++;}
    if(rails!=4||bearings!=1)throw new AssertionError("Resources changed "+rails+" rails / "+bearings+" bearings");
    int drops=0;for(var entity:level.getAllEntities())if(entity instanceof net.minecraft.world.entity.item.ItemEntity)drops++;
    if(drops!=0)throw new AssertionError("Unexpected item drops "+drops);
    recovered=true;report.addProperty("bearingRecovered",true);report.addProperty("railCount",rails);report.addProperty("cargoNbtPreserved",true);report.addProperty("unexpectedDrops",drops);
   }
   if(height){Files.writeString(Path.of(System.getProperty("linear.review.output")),new GsonBuilder().setPrettyPrinting().create().toJson(report));server.halt(false);start=false;}
  }
  if(tick==30&&recovered){
   if(SubLevelContainer.getContainer(level).getSubLevel(ship.getUniqueId())!=null)throw new AssertionError("Disassembled body retained");report.addProperty("bodyRemoved",true);
   if(use(level,base)!=net.minecraft.world.InteractionResult.SUCCESS)throw new AssertionError("Recovered bearing could not reassemble");report.addProperty("reassembly",true);
   Files.writeString(Path.of(System.getProperty("linear.review.output")),new GsonBuilder().setPrettyPrinting().create().toJson(report));server.halt(false);start=false;
  }
 }catch(Throwable e){e.printStackTrace();try{Files.writeString(Path.of(System.getProperty("linear.review.output")+".failure"),e.toString()+"\n"+report);}catch(Exception f){f.printStackTrace();}start=false;event.getServer().halt(false);}}
}
