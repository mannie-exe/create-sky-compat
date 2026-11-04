package linearreview;
import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.nio.file.*;
@Mod("linear_review_probe")
public class BootstrapProbe {
 public BootstrapProbe() { NeoForge.EVENT_BUS.addListener(this::started); }
 private void started(ServerStartedEvent event) {
  try {
   var server=event.getServer();var level=server.overworld();var out=new JsonObject();
   var ids=new String[]{"linear_bearing","linear_casing","torsional_anchor","magnetic_port","gyroscopic_gearbox","redstone_converter","andesite_lamp","slippery_copycat","bubble_gum_block"};
   for(var id:ids) {
    var key=ResourceLocation.fromNamespaceAndPath("linearbearing",id);
    if(!BuiltInRegistries.BLOCK.containsKey(key))throw new AssertionError("Missing block "+id);
    if(server.getRecipeManager().byKey(key).isEmpty())throw new AssertionError("Missing recipe "+id);
   }
   var gum=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("linearbearing:bubble_gum_block"));
   var pos=new BlockPos(0,140,0);level.setBlock(pos,gum.defaultBlockState(),3);
   var drops=Block.getDrops(gum.defaultBlockState(),level,pos,level.getBlockEntity(pos),null,new ItemStack(Items.IRON_PICKAXE));
   if(drops.size()!=1||!drops.getFirst().is(gum.asItem())||drops.getFirst().getCount()!=1)throw new AssertionError("Incorrect gum drops "+drops);
   var converterBlock=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("linearbearing:redstone_converter"));
   var converterPos=pos.offset(5,0,0);level.setBlock(converterPos,converterBlock.defaultBlockState(),3);
   var converter=(com.bearing.linearbearing.blockentity.RedstoneConverterBlockEntity)level.getBlockEntity(converterPos);
   var signals=new JsonArray();
   for(float speed:new float[]{-256,-120,-119,-8,-7,0,7,8,15,16,119,120,256}) {
    converter.setSpeed(speed);int got=converter.getRedstoneSignal();int expected=Math.min(15,(int)(Math.abs(speed)/8));
    if(got!=expected)throw new AssertionError("Converter threshold "+speed+" -> "+got);
    signals.add(got);
   }
   out.add("converterSignals",signals);
   var lamp=(com.bearing.linearbearing.blocks.AndesiteLampBlock)BuiltInRegistries.BLOCK.get(ResourceLocation.parse("linearbearing:andesite_lamp"));
   var lampPos=pos.offset(10,0,0);int lampStates=0;
   for(var state:lamp.getStateDefinition().getPossibleStates()) {
    level.setBlock(lampPos,state,3);int expected=state.getValue(com.bearing.linearbearing.blocks.AndesiteLampBlock.POWER);
    if(state.getValue(com.bearing.linearbearing.blocks.AndesiteLampBlock.MODE)==com.bearing.linearbearing.blocks.AndesiteLampBlock.LampMode.INVERTED)expected=15-expected;
    if(state.getLightEmission(level,lampPos)!=expected)throw new AssertionError("Lamp emission "+state);lampStates++;
   }
   out.addProperty("lampStates",lampStates);
   var slippery=(com.bearing.linearbearing.blocks.SlipperyCopycatBlock)BuiltInRegistries.BLOCK.get(ResourceLocation.parse("linearbearing:slippery_copycat"));
   var copyPos=pos.offset(15,0,0);level.setBlock(copyPos,slippery.defaultBlockState(),3);
   var copy=(com.simibubi.create.content.decoration.copycat.CopycatBlockEntity)level.getBlockEntity(copyPos);copy.setMaterial(net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState());
   if(!copy.getMaterial().is(net.minecraft.world.level.block.Blocks.OAK_PLANKS)||Math.abs(slippery.getFriction(level.getBlockState(copyPos),level,copyPos,null)-0.989f)>0.00001f)throw new AssertionError("Copied material changed friction");out.addProperty("copyMaterialRetained",true);
   float friction=slippery.getFriction(slippery.defaultBlockState(),level,pos,null);
   if(Math.abs(friction-0.989f)>0.00001f)throw new AssertionError("Slippery friction");out.addProperty("slipperyFriction",friction);
   var fake=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);fake.setShiftKeyDown(false);
   if(!gum.defaultBlockState().getCollisionShape(level,pos,net.minecraft.world.phys.shapes.CollisionContext.of(fake)).isEmpty())throw new AssertionError("Gum normal collision");
   fake.setShiftKeyDown(true);fake.setPose(net.minecraft.world.entity.Pose.CROUCHING);
   boolean direct=((com.bearing.linearbearing.blocks.BubbleGumBlock)gum).getCollisionShape(gum.defaultBlockState(),level,pos,net.minecraft.world.phys.shapes.CollisionContext.of(fake)).isEmpty();
   boolean cached=gum.defaultBlockState().getCollisionShape(level,pos,net.minecraft.world.phys.shapes.CollisionContext.of(fake)).isEmpty();
   Files.writeString(Path.of(System.getProperty("linear.review.output")+".collision"),"crouching="+fake.isCrouching()+", directEmpty="+direct+", stateEmpty="+cached);
   if(gum.defaultBlockState().getCollisionShape(level,pos,net.minecraft.world.phys.shapes.CollisionContext.of(fake)).isEmpty())throw new AssertionError("Gum crouching collision");
   out.addProperty("gumCrouchingCollision",true);
   out.addProperty("registeredObtainableBlocks",ids.length);out.addProperty("loadedRecipes",ids.length);out.addProperty("bubbleGumDrops",drops.getFirst().getCount());
   out.addProperty("bootstrapOwnedByCompat",design.inherent.skyweavecompat.OptionalIntegrations.enabled(design.inherent.skyweavecompat.Feature.LINEAR_BOOTSTRAP));
   assemblyTests(server,out);
   transmissionTests(server,out);
   Files.writeString(Path.of(System.getProperty("linear.review.output")),new GsonBuilder().setPrettyPrinting().create().toJson(out));
  } catch(Throwable e) {e.printStackTrace();try{Files.writeString(Path.of(System.getProperty("linear.review.output")+".failure"),e.toString());}catch(Exception f){f.printStackTrace();}}
  event.getServer().halt(false);
 }
 private static net.minecraft.world.level.block.state.BlockState state(String id,net.minecraft.core.Direction facing) {
  return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id)).defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,facing);
 }
 private static void transmissionTests(net.minecraft.server.MinecraftServer server,JsonObject report) throws Exception {
  var level=server.overworld();var rows=new JsonArray();
  for(boolean spring:new boolean[]{false,true}) {
   var from=new BlockPos(spring?60:20,150,0);var to=from.east(spring?5:3);
   String id=spring?"linearbearing:torsional_anchor":"linearbearing:magnetic_port";
   level.setBlock(from,state(id,net.minecraft.core.Direction.EAST),3);level.setBlock(to,state(id,net.minecraft.core.Direction.WEST),3);
   var a=(com.simibubi.create.content.kinetics.base.KineticBlockEntity)level.getBlockEntity(from);
   var b=(com.simibubi.create.content.kinetics.base.KineticBlockEntity)level.getBlockEntity(to);
   if(spring) {
    level.setBlock(from.east(),state("simulated:spring",net.minecraft.core.Direction.EAST),3);
    level.setBlock(to.west(),state("simulated:spring",net.minecraft.core.Direction.WEST),3);
    var sa=(dev.simulated_team.simulated.content.blocks.spring.SpringBlockEntity)level.getBlockEntity(from.east());
    var sb=(dev.simulated_team.simulated.content.blocks.spring.SpringBlockEntity)level.getBlockEntity(to.west());
    sa.setPartnerPos(sb.getBlockPos(),null);sb.setPartnerPos(sa.getBlockPos(),null);
   }
   var motorPos=from.west();level.setBlock(motorPos,state("create:creative_motor",net.minecraft.core.Direction.EAST),3);
   var motor=(com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity)level.getBlockEntity(motorPos);
   motor.generatedSpeed.setValue(32);motor.tick();motor.updateGeneratedRotation();
   var loadPos=to.east();level.setBlock(loadPos,state("create:encased_fan",net.minecraft.core.Direction.EAST),3);
   var load=(com.simibubi.create.content.kinetics.base.KineticBlockEntity)level.getBlockEntity(loadPos);
   a.tick();b.tick();load.tick();
   float expected=spring?-32:32;
   var row=new JsonObject();row.addProperty("spring",spring);row.addProperty("sourceSpeed",a.getSpeed());row.addProperty("receiverSpeed",b.getSpeed());row.addProperty("loadSpeed",load.getSpeed());
   row.addProperty("sameNetwork",java.util.Objects.equals(a.network,b.network));row.addProperty("sourceCount",a.getOrCreateNetwork().sources.size());
   Files.writeString(Path.of(System.getProperty("linear.review.output")+".network"),row.toString());
   if(a.getSpeed()!=32||b.getSpeed()!=expected||load.getSpeed()!=expected)throw new AssertionError("Native transmission "+row);
   if(!java.util.Objects.equals(a.network,b.network)||a.getOrCreateNetwork().sources.size()!=1)throw new AssertionError("Network accounting "+row);
   if(a.getGeneratedSpeed()!=0||b.getGeneratedSpeed()!=0||a.calculateAddedStressCapacity()!=0||b.calculateAddedStressCapacity()!=0)throw new AssertionError("Transmission generated power");
   var network=a.getOrCreateNetwork();float capacity=motor.calculateAddedStressCapacity();network.updateCapacityFor(motor,0.01f);
   if(!load.isOverStressed()||load.getSpeed()!=0)throw new AssertionError("Remote load failed to stall");
   network.updateCapacityFor(motor,capacity);if(load.getSpeed()!=expected)throw new AssertionError("Load did not recover");
   row.addProperty("nativeOverstress",true);
   level.destroyBlock(motorPos,false);a.tick();b.tick();
   if(a.getSpeed()!=0||b.getSpeed()!=0||load.getSpeed()!=0)throw new AssertionError("Source removal left rotation");
   row.addProperty("sourceRemovalStopped",true);rows.add(row);
  }

  var port=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("linearbearing:magnetic_port"));
  for(String invalid:new String[]{"facing","obstruction","range","dimension"}) {
   var from=new BlockPos(110+rows.size()*15,150,20);var to=from.east(invalid.equals("range")?6:3);
   var targetLevel=invalid.equals("dimension")?server.getLevel(net.minecraft.world.level.Level.NETHER):level;
   level.setBlock(from,state("linearbearing:magnetic_port",net.minecraft.core.Direction.EAST),3);
   if(targetLevel!=level)level.setBlock(to,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
   targetLevel.setBlock(to,state("linearbearing:magnetic_port",invalid.equals("facing")?net.minecraft.core.Direction.NORTH:net.minecraft.core.Direction.WEST),3);
   if(invalid.equals("obstruction"))level.setBlock(from.east(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
   var a=(com.simibubi.create.content.kinetics.base.KineticBlockEntity)level.getBlockEntity(from);
   var b=(com.simibubi.create.content.kinetics.base.KineticBlockEntity)targetLevel.getBlockEntity(to);
   var driver=from.west();level.setBlock(driver,state("create:creative_motor",net.minecraft.core.Direction.EAST),3);
   var motor=(com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity)level.getBlockEntity(driver);motor.generatedSpeed.setValue(32);motor.tick();motor.updateGeneratedRotation();
   a.tick();b.tick();
   if(a.getSpeed()!=32||b.getSpeed()!=0)throw new AssertionError("Invalid-coupling drive control "+invalid+": "+a.getSpeed()+" / "+b.getSpeed());
   if(((design.inherent.skyweavecompat.linear.TransmissionEndpoint)a).skyweave$peer()!=null||((design.inherent.skyweavecompat.linear.TransmissionEndpoint)b).skyweave$peer()!=null)throw new AssertionError("Accepted invalid coupling "+invalid);
   var row=new JsonObject();row.addProperty("rejected",invalid);rows.add(row);
  }
  var gyroBlock=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("linearbearing:gyroscopic_gearbox"));
  var gyroPos=new BlockPos(240,150,20);level.setBlock(gyroPos,state("linearbearing:gyroscopic_gearbox",net.minecraft.core.Direction.UP),3);
  var gyro=(com.bearing.linearbearing.blockentity.GyroscopicGearboxBlockEntity)level.getBlockEntity(gyroPos);
  var sample=gyro.getClass().getDeclaredMethod("processDirectGeneratorPhysics",org.joml.Quaterniond.class);sample.setAccessible(true);
  sample.invoke(gyro,new org.joml.Quaterniond());sample.invoke(gyro,new org.joml.Quaterniond().rotationY(0.1));
  if(gyro.getGeneratedSpeed()==0||gyro.calculateAddedStressCapacity()!=0)throw new AssertionError("Gyro sensor output/capacity");
  report.addProperty("gyroSampledSpeed",gyro.getGeneratedSpeed());report.addProperty("gyroCapacity",gyro.calculateAddedStressCapacity());
  report.add("transmission",rows);
 }

 private static void assemblyTests(net.minecraft.server.MinecraftServer server,JsonObject report) throws Exception {
  var level=server.overworld();var rows=new JsonArray();
  for(boolean blocked:new boolean[]{false,true}) {
   var pos=new BlockPos(blocked?320:300,150,0);var block=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("linearbearing:linear_bearing"));
   var state=block.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.ATTACH_FACE,net.minecraft.world.level.block.state.properties.AttachFace.FLOOR);
   level.setBlock(pos,state,3);level.setBlock(pos.above(),blocked?net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
   var fake=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);fake.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
   var method=block.getClass().getDeclaredMethod("useWithoutItem",net.minecraft.world.level.block.state.BlockState.class,net.minecraft.world.level.Level.class,BlockPos.class,net.minecraft.world.entity.player.Player.class,net.minecraft.world.phys.BlockHitResult.class);method.setAccessible(true);
   var before=dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(level).getAllSubLevels().size();
   var result=method.invoke(block,state,level,pos,fake,new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),net.minecraft.core.Direction.UP,pos,false));
   var after=dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(level).getAllSubLevels().size();
   if(blocked) {
    if(!level.getBlockState(pos).equals(state)||!level.getBlockState(pos.above()).is(net.minecraft.world.level.block.Blocks.BEDROCK)||before!=after)throw new AssertionError("Failed assembly changed source resources");
   }else {
    if(!BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString().equals("linearbearing:linear_casing")||!level.getBlockState(pos.above()).isAir()||after!=before+1)throw new AssertionError("Empty-front assembly");
   }
   var row=new JsonObject();row.addProperty("blocked",blocked);row.addProperty("result",result.toString());row.addProperty("sublevelsAdded",after-before);rows.add(row);
  }
  report.add("assembly",rows);
 }

}
