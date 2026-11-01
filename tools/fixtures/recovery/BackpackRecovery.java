package fixture;

import com.tiviacz.travelersbackpack.components.BackpackContainerContents;
import com.tiviacz.travelersbackpack.init.ModDataComponents;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import io.wispforest.accessories.api.AccessoriesCapability;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Full-pack-only inventory custody witness, compiled separately from the minimum fixture. */
public final class BackpackRecovery {
    public static void equip(ServerPlayer player) {
        if (com.tiviacz.travelersbackpack.config.TravelersBackpackConfig.SERVER.backpackSettings.backpackDeathPlace.get())
            throw new AssertionError("Competing backpack death placement is enabled");
        var backpack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("travelersbackpack:standard")));
        if (backpack.isEmpty()) throw new AssertionError("Backpack item missing");
        BackpackWrapper.initializeSize(backpack);
        var cargo = new ItemStack(Items.EMERALD, 13);
        cargo.set(DataComponents.CUSTOM_NAME, Component.literal("recovery cargo"));
        var contents = net.minecraft.core.NonNullList.withSize(27, ItemStack.EMPTY);
        contents.set(0,cargo);
        backpack.set(ModDataComponents.BACKPACK_CONTAINER.get(), BackpackContainerContents.fromItems(27, contents));
        var capability = AccessoriesCapability.getOptionally(player).orElseThrow();
        if (capability.attemptToEquipAccessory(backpack) == null) throw new AssertionError("Native backpack equip refused");
        var goggles = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("create:goggles")));
        if (capability.attemptToEquipAccessory(goggles) == null) throw new AssertionError("Native face accessory equip refused");
        player.getInventory().armor.set(0, new ItemStack(Items.IRON_BOOTS));
    }

    public static void cleared(ServerPlayer player) {
        var capability = AccessoriesCapability.getOptionally(player).orElseThrow();
        if (!capability.getAllEquipped().isEmpty()) throw new AssertionError("Equipped items retained after capture");
    }

    public static void restored(ServerPlayer player) {
        var capability = AccessoriesCapability.getOptionally(player).orElseThrow();
        var backpacks = capability.getEquipped(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals("travelersbackpack:standard"));
        if (backpacks.size() != 1) throw new AssertionError("Expected one recovered equipped backpack: " + backpacks.size());
        var backpack = backpacks.getFirst().stack();
        var cargo = backpack.get(ModDataComponents.BACKPACK_CONTAINER.get()).getItems();
        if (cargo.stream().filter(stack -> stack.is(Items.EMERALD)).mapToInt(ItemStack::getCount).sum() != 13
                || !Component.literal("recovery cargo").equals(cargo.getFirst().get(DataComponents.CUSTOM_NAME)))
            throw new AssertionError("Backpack nested contents changed");
        if (capability.getEquipped(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals("create:goggles")).size() != 1
                || !player.getInventory().armor.getFirst().is(Items.IRON_BOOTS)) throw new AssertionError("Equipment not restored");
        if (player.getInventory().items.stream().anyMatch(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("travelersbackpack")))
            throw new AssertionError("Duplicate loose backpack");
    }
}
