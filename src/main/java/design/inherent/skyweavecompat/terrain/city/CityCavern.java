package design.inherent.skyweavecompat.terrain.city;

import design.inherent.skyweavecompat.terrain.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.sathrek.sky_archipelago.worldgen.generator.core.SkyIslandChunkGenerator;
import org.sathrek.sky_archipelago.worldgen.generator.structure.v2.common.placement.PendingStructurePlacement;

/** Local, pre-template clearance from canonical city pieces; never generates or loads chunks. */
public final class CityCavern {
    private static final ResourceLocation CITY = ResourceLocation.withDefaultNamespace("ancient_city");
    private CityCavern() {}

    public static void apply(WorldGenLevel level, ChunkAccess chunk, StructureManager manager, SkyIslandChunkGenerator generator) {
        var adapted = (GeologicalGenerator)(Object)generator;
        if (!adapted.skyweave$isEnabled()) return;
        var city = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(CITY);
        var starts = manager.startsForStructure(chunk.getPos(), structure -> structure == city);
        if (starts.isEmpty()) return;
        var field = adapted.skyweave$field(level.getLevel().getChunkSource().randomState());
        var settings = generator.resolvedSkyIslandSettings();
        var snapshot = field.sampleChunkTerrainSnapshot(chunk.getPos(), settings);
        var cp = chunk.getPos();
        var pos = new BlockPos.MutableBlockPos();
        boolean changed = false;
        int shell = InteriorConfiguration.profile().shellThickness();
        for (var start : starts) {
            if (!start.isValid() || PendingStructurePlacement.isRevoked(start) || start.getPieces().isEmpty()) continue;
            var center = start.getPieces().getFirst().getBoundingBox().getCenter();
            var host = field.sampleDominantIslandDescriptor(center.getX(), center.getZ(), settings);
            if (host == null || host.archetype().waterBased()) continue;
            var rigid = new ObjectArrayList<Beardifier.Rigid>();
            var junctions = new ObjectArrayList<JigsawJunction>();
            var pieces = new ArrayList<CityClearance.Piece>();
            for (var piece : start.getPieces()) {
                if (!piece.isCloseToChunk(cp, CityClearance.RADIUS)
                        || !(piece instanceof PoolElementStructurePiece pool)
                        || pool.getElement().getProjection() != StructureTemplatePool.Projection.RIGID) continue;
                var box = piece.getBoundingBox();
                rigid.add(new Beardifier.Rigid(box, TerrainAdjustment.BEARD_BOX, pool.getGroundLevelDelta()));
                pieces.add(new CityClearance.Piece(new StructureBurial.Box(box.minX(), box.minY(), box.minZ(),
                        box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1), box.minY() + pool.getGroundLevelDelta()));
                for (var junction : pool.getJunctions())
                    if (junction.getSourceX() > cp.getMinBlockX() - 12 && junction.getSourceX() < cp.getMaxBlockX() + 12
                            && junction.getSourceZ() > cp.getMinBlockZ() - 12 && junction.getSourceZ() < cp.getMaxBlockZ() + 12)
                        junctions.add(junction);
            }
            if (pieces.isEmpty()) continue;
            var kernel = new Beardifier(rigid.iterator(), junctions.iterator());
            var exterior = new HashMap<Long, CityClearance.Range>();
            CityClearance.Body body = (x, z) -> exterior.computeIfAbsent((long)x << 32 ^ (z & 0xffffffffL), ignored -> {
                var outer = ((InteriorField)(Object)field).skyweave$exteriorOf(host, x, z, settings);
                return outer.exists() ? new CityClearance.Range(outer.bottomY(), outer.topY()) : CityClearance.Range.EMPTY;
            });
            for (int x = cp.getMinBlockX(); x <= cp.getMaxBlockX(); x++) for (int z = cp.getMinBlockZ(); z <= cp.getMaxBlockZ(); z++) {
                var range = CityClearance.influence(pieces, x, z);
                if (range.bottom() > range.top()) continue;
                var column = snapshot.columnDataAt(x, z);
                if (!host.equals(column.dominantDescriptor())) continue;
                range = range.intersect(CityClearance.retainShell(x, z, shell, body))
                        .intersect(new CityClearance.Range(chunk.getMinBuildHeight(), chunk.getMaxBuildHeight() - 1));
                var river = column.river();
                var water = column.waterArchetype();
                for (int y = range.bottom(); y <= range.top(); y++) {
                    if (CityClearance.protectsFloor(pieces, x, y, z)
                            || river.affected() && y >= river.solidSurfaceY() - 3
                            || water.containsWaterAt(y) || water.containsProtectedSealAt(y)) continue;
                    pos.set(x, y, z);
                    var state = chunk.getBlockState(pos);
                    if (!state.is(BlockTags.OVERWORLD_CARVER_REPLACEABLES) || !state.getFluidState().isEmpty()
                            || state.hasBlockEntity()) continue;
                    if (!CityClearance.clears(kernel.compute(new DensityFunction.SinglePointContext(x, y, z)))) continue;
                    chunk.setBlockState(pos, Blocks.CAVE_AIR.defaultBlockState(), false);
                    changed = true;
                }
            }
        }
        if (changed) Heightmap.primeHeightmaps(chunk, EnumSet.of(Heightmap.Types.WORLD_SURFACE_WG, Heightmap.Types.OCEAN_FLOOR_WG));
    }
}
