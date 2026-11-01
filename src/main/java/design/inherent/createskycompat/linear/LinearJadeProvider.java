package design.inherent.createskycompat.linear;

import com.bearing.linearbearing.blockentity.LinearMovingBlockEntity;
import com.bearing.linearbearing.blockentity.MagneticPortBlockEntity;
import com.bearing.linearbearing.blockentity.TorsionalAnchorBlockEntity;
import com.bearing.linearbearing.blocks.LinearBearingBlock;
import com.bearing.linearbearing.blocks.LinearCasingBlock;
import com.bearing.linearbearing.blocks.LinearMovingBlock;
import com.bearing.linearbearing.blocks.GyroscopicGearboxBlock;
import com.bearing.linearbearing.blocks.MagneticPortBlock;
import com.bearing.linearbearing.blocks.TorsionalAnchorBlock;
import design.inherent.createskycompat.CreateSkyCompat;
import design.inherent.createskycompat.Feature;
import design.inherent.createskycompat.OptionalIntegrations;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

/** Only server-observed brake/link state is presented as status; no inferred client physics. */
public enum LinearJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final String DATA = "create_sky_compat:linear";
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(CreateSkyCompat.ID, "linear_machinery");

    public static void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(INSTANCE, LinearMovingBlockEntity.class);
        if (OptionalIntegrations.enabled(Feature.LINEAR_TRANSMISSION)) {
            registration.registerBlockDataProvider(INSTANCE, MagneticPortBlockEntity.class);
            registration.registerBlockDataProvider(INSTANCE, TorsionalAnchorBlockEntity.class);
        }
    }
    public static void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(INSTANCE, LinearBearingBlock.class);
        registration.registerBlockComponent(INSTANCE, LinearCasingBlock.class);
        registration.registerBlockComponent(INSTANCE, LinearMovingBlock.class);
        if (OptionalIntegrations.enabled(Feature.LINEAR_TRANSMISSION)) {
            registration.registerBlockComponent(INSTANCE, MagneticPortBlock.class);
            registration.registerBlockComponent(INSTANCE, TorsionalAnchorBlock.class);
        }
        if (OptionalIntegrations.enabled(Feature.LINEAR_GYRO))
            registration.registerBlockComponent(INSTANCE, GyroscopicGearboxBlock.class);
    }

    @Override public ResourceLocation getUid() { return UID; }

    @Override public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        var data = new CompoundTag();
        var entity = accessor.getBlockEntity();
        if (entity instanceof BrakedCarriage carriage) data.putBoolean("braked", carriage.createSky$brake() != null);
        if (entity instanceof TransmissionEndpoint endpoint) {
            var peer = endpoint.createSky$peer();
            data.putBoolean("linked", peer != null && !peer.isRemoved() && peer.getLevel() == entity.getLevel());
        }
        tag.put(DATA, data);
    }

    @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        var block = accessor.getBlock();
        var data = accessor.getServerData().getCompound(DATA);
        if (block instanceof LinearMovingBlock) {
            if (data.contains("braked")) tooltip.add(text(data.getBoolean("braked") ? "braked" : "free"));
            if (accessor.showDetails()) tooltip.add(text("brake_help"));
        } else if (block instanceof LinearBearingBlock) {
            tooltip.add(text("passive"));
            if (accessor.showDetails()) tooltip.add(text("assembly"));
        } else if (block instanceof LinearCasingBlock) {
            tooltip.add(text("guide"));
            if (accessor.showDetails()) tooltip.add(text("recovery"));
        } else if (block instanceof GyroscopicGearboxBlock) {
            tooltip.add(text("sensor"));
            if (accessor.showDetails()) tooltip.add(text("sensor_help"));
        } else if (data.contains("linked")) {
            tooltip.add(text(data.getBoolean("linked") ? "linked" : "unlinked"));
            if (accessor.showDetails()) tooltip.add(text(block instanceof MagneticPortBlock ? "magnetic" : "spring"));
        }
    }

    private static Component text(String key) { return Component.translatable("create_sky_compat.jade.linear." + key); }
}
