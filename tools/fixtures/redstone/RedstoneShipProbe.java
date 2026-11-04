package linearreview;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.ggrgg.createredstonelinkgui.common.menu.RedstoneLinkMenu;
import design.inherent.skyweavecompat.redstone.*;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import com.ggrgg.createredstonelinkgui.common.network.RedstoneLinkMovePayload;
import java.util.*;

/** Loaded only for the full Sable combination. Native plot coordinates, not a distance mock. */
public final class RedstoneShipProbe {
    private static BlockPos plot;
    public static void prepare(ServerLevel level) {
        var source=new BlockPos(20,100,0);
        for(int x=0;x<4;x++)level.setBlock(source.below().east(x),Blocks.STONE.defaultBlockState(),3);
        var link=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:redstone_link"));
        level.setBlock(source,link.defaultBlockState().setValue(BlockStateProperties.FACING,Direction.UP),3);
        var positions=new ArrayList<BlockPos>(); positions.add(source);
        for(int x=0;x<4;x++) positions.add(source.below().east(x));
        var body=SubLevelAssemblyHelper.assembleBlocks(level,source,positions,BoundingBox3i.from(positions));
        plot=body.getPlot().getCenterBlock();
    }
    public static void check(ServerLevel level,JsonObject report) {
        var player=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"ShipLinkFixture"));
        player.setGameMode(GameType.SURVIVAL);
        var projected=Sable.HELPER.projectOutOfSubLevel(level,plot.getCenter());
        player.setPos(projected.x,projected.y,projected.z+2);
        var menu=new RedstoneLinkMenu(1,player.getInventory(),plot);player.containerMenu=menu;
        if(!menu.stillValid(player))throw new AssertionError("nearby projected ship menu invalid");
        if(!GuiRelocation.allow(player,new RedstoneLinkMovePayload(plot,plot.east(2),Direction.UP)))throw new AssertionError("same-ship ordinary link relocation denied");
        if(GuiRelocation.allow(player,new RedstoneLinkMovePayload(plot,BlockPos.containing(projected),Direction.UP)))throw new AssertionError("cross-frame relocation accepted");
        report.addProperty("projectedShipMenuAndSameFrameRelocationAllowed",true);
        report.addProperty("crossFrameRelocationDenied",true);
    }
}
