package design.inherent.createskycompat.redstone;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class RedstoneScraping {
    public static void register() { NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, RedstoneScraping::scrape); }
    private static void scrape(BlockEvent.BlockToolModificationEvent event) {
        if (event.isSimulated() || event.getItemAbility() != ItemAbilities.AXE_SCRAPE || event.getFinalState() == null
                || !event.getHeldItemStack().canPerformAction(ItemAbilities.AXE_SCRAPE)
                || WeatheringCopper.getPrevious(event.getState()).isEmpty()) return;
        if (event.getLevel() instanceof ServerLevel level)
            Block.popResourceFromFace(level, event.getPos(), event.getContext().getClickedFace(), new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("create_redstone_additions:verdigris")), 3 + level.random.nextInt(4)));
    }
    private RedstoneScraping() {}
}
