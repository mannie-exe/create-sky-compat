package design.inherent.createskycompat.mixin;

import design.inherent.createskycompat.NeighborConfig;
import design.inherent.createskycompat.placement.*;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.sathrek.sky_archipelago.config.SkyIslandSettings;
import org.sathrek.sky_archipelago.worldgen.generator.field.IslandField;
import org.spongepowered.asm.mixin.*;
import java.util.List;

@Mixin(ChunkGeneratorStructureState.class)
public abstract class VillageStructureStateMixin implements VillageStructureState {
    @Shadow @Final @Mutable private List<Holder<StructureSet>> possibleStructureSets;
    @Shadow @Final private long levelSeed;
    @Shadow @Final private net.minecraft.world.level.biome.BiomeSource biomeSource;
    @Shadow @Final private net.minecraft.world.level.levelgen.RandomState randomState;
    @Unique private boolean createSky$configured;

    @Override public void createSky$configureVillages(IslandField field, SkyIslandSettings settings) {
        if (createSky$configured) throw new IllegalStateException("Village placement state already initialized");
        var id = ResourceLocation.parse("skyvillages:skyvillage");
        possibleStructureSets = possibleStructureSets.stream().map(holder -> {
            var set = holder.value();
            if (set.structures().stream().noneMatch(entry -> entry.structure().is(id))) return holder;
            if (set.structures().size() != 1 || !(set.placement() instanceof RandomSpreadStructurePlacement spread)) {
                throw new IllegalStateException("Create Sky requires a dedicated random-spread Sky Villages structure set");
            }
            return Holder.direct(new StructureSet(set.structures(),
                    new ArchipelagoVillagePlacement(spread, field, settings, levelSeed, NeighborConfig.settings(),
                            biomeSource, randomState, set.structures().getFirst().structure().value().biomes())));
        }).toList();
        createSky$configured = true;
    }
}
