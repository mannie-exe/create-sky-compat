package design.inherent.skyweavecompat.linkage;

import com.enxv.aerouniversaljoint.content.*;
import design.inherent.skyweavecompat.SkyweaveCompat;
import design.inherent.skyweavecompat.mixin.linkage.ConversionStateAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

/** Link/actuator state comes from the server; native Create inspection still owns kinetic statistics. */
public enum LinkageJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;
    private static final String DATA = "skyweave_compat:linkage";
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(SkyweaveCompat.ID, "transmission_linkage");

    public static void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(INSTANCE, HydraulicConnectionHeadBlockEntity.class);
        registration.registerBlockDataProvider(INSTANCE, UniversalJointBlockEntity.class);
        registration.registerBlockDataProvider(INSTANCE, DampingStressBearingBlockEntity.class);
    }
    public static void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(INSTANCE, HydraulicConnectionHeadBlock.class);
        registration.registerBlockComponent(INSTANCE, UniversalJointBlock.class);
        registration.registerBlockComponent(INSTANCE, DampingStressBearingBlock.class);
    }
    @Override public ResourceLocation getUid() { return UID; }

    @Override public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        var data = new CompoundTag();
        if (accessor.getBlockEntity() instanceof HydraulicConnectionHeadBlockEntity head) {
            data.putBoolean("linked", head.hasLink());
            if (head.hasLink()) {
                var other = head.getLoadedLinkedConnectionHead();
                data.putBoolean("available", other != null && !other.isRemoved() && other.references(head));
                data.putInt("target", head.getExpectedLengthTenths());
                data.putBoolean("regulated", head.isExpectedLengthControlledByRegulator());
                data.putBoolean("giant", head.isGiantHydraulicLink());
                data.putBoolean("free", head.isFreeMode());
                data.putInt("flow", head.getStretchResistance());
                data.putInt("force", head.getReturnForce());
                if (other != null) {
                    var from = dev.ryanhcode.sable.Sable.HELPER.projectOutOfSubLevel(accessor.getLevel(), head.getBlockPos().getCenter());
                    var to = dev.ryanhcode.sable.Sable.HELPER.projectOutOfSubLevel(accessor.getLevel(), other.getBlockPos().getCenter());
                    data.putDouble("length", from.distanceTo(to));
                }
            }
        } else if (accessor.getBlockEntity() instanceof DampingStressBearingBlockEntity bearing) {
            var output = bearing.getOutputKinetics();
            data.putFloat("rpm", output.getGeneratedSpeed());
            data.putFloat("su", output.calculateAddedStressCapacity() * Math.abs(output.getTheoreticalSpeed()));
            var state = (ConversionStateAccessor) bearing;
            if (state.skyweave$suppressed() && state.skyweave$suppressedBy() != null)
                data.putString("suppressedBy", state.skyweave$suppressedBy());
        } else if (accessor.getBlockEntity() instanceof UniversalJointBlockEntity joint) {
            data.putBoolean("linked", joint.hasLink());
        }
        tag.put(DATA, data);
    }

    @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        var data = accessor.getServerData().getCompound(DATA);
        if (data.contains("rpm")) {
            tooltip.add(text("output", decimal(data.getFloat("rpm")), decimal(data.getFloat("su"))));
            if (data.contains("suppressedBy")) {
                var id = ResourceLocation.tryParse(data.getString("suppressedBy"));
                if (id != null) tooltip.add(text("suppressed", net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id).getName()));
            }
            return;
        }
        if (!data.contains("linked")) return;
        tooltip.add(text(!data.getBoolean("linked") ? "unlinked"
                : data.contains("available") && !data.getBoolean("available") ? "unavailable" : "linked"));
        if (data.contains("length")) tooltip.add(text("length", decimal(data.getDouble("length")), decimal(data.getInt("target") / 10.0)));
        if (!accessor.showDetails()) return;
        if (data.getBoolean("giant")) {
            tooltip.add(text("hydraulics", data.getInt("force"), data.getInt("flow")));
            tooltip.add(text(data.getBoolean("free") ? "vented" : "closed"));
        }
        if (data.getBoolean("regulated")) tooltip.add(text("regulated"));
        tooltip.add(text("remove"));
    }

    private static String decimal(double value) { return String.format(java.util.Locale.ROOT, "%.1f", value); }
    private static Component text(String key, Object... values) {
        return Component.translatable("skyweave_compat.jade.linkage." + key, values);
    }
}
