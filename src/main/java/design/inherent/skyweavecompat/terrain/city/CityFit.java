package design.inherent.skyweavecompat.terrain.city;

import java.util.*;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.sathrek.sky_archipelago.worldgen.generator.structure.v2.underground.UndergroundPieceFitEvaluator.Fit;
import org.sathrek.sky_archipelago.worldgen.generator.structure.v2.underground.UndergroundSearchContext;

/** City-only bounded placement, retaining the native layout and final committer. */
public final class CityFit {
    private record Floor(int x, int y, int z, int roof, double weight) {}
    private CityFit() {}

    public static Fit fit(StructureStart start, UndergroundSearchContext search) {
        var site = CityPlacementScope.site();
        if (site == null || start == null || !start.isValid() || start.getPieces().isEmpty()) return rejected("no_city_interior", 0, 0);
        var boxes = start.getPieces().stream().map(piece -> box(piece.getBoundingBox())).distinct().toList();
        if (boxes.size() > StructureBurial.MAX_PIECES || start.getBoundingBox().getXSpan() > 1024
                || start.getBoundingBox().getZSpan() > 1024) return rejected("city_layout_too_large", 0, 0);
        var plan = StructureBurial.plan(boxes);
        var root = start.getPieces().getFirst().getBoundingBox();
        var floors = floors(boxes);
        if (!search.budget().trySupportChecks(plan.samples().size() + floors.size() + 9))
            return rejected("budget_city_samples", 0, plan.samples().size());
        int preferred = site.floorY() - root.minY();
        int[] offsets = site.hollow() ? new int[]{0,-4,4,-8,8}
                : new int[]{0,-8,8,-16,16,-24,24,-32,32,-48,48};
        double best = 0;
        String reason = "city_core_or_floor";
        for (int offset : offsets) {
            int delta = preferred + offset;
            if (!core(root, delta, site.hollow())) continue;
            double buried = plan.buriedFraction(delta, (x,y,z) -> CityPlacementScope.exterior(x,z).contains(y));
            best = Math.max(best, buried);
            double threshold = site.hollow() ? 1 : CityConfiguration.policy().minimumBuriedFraction();
            if (buried + 1e-12 < threshold) { reason = "city_burial"; continue; }
            if (!floorsFit(floors, delta, site.hollow())) { reason = "city_floor_or_roof"; continue; }
            if (site.hollow()) {
                double air = 0, volume = 0;
                for (var sample : plan.samples()) {
                    int y = sample.y() + delta;
                    volume += sample.weight();
                    if (CityPlacementScope.solid(sample.x(),sample.z()).stream().noneMatch(s -> s.contains(y))) air += sample.weight();
                }
                if (air < volume * .55) { reason = "city_hollow_room"; continue; }
            }
            int x = (root.minX() + root.maxX()) >> 1, z = (root.minZ() + root.maxZ()) >> 1;
            if (!CityPlacementScope.validBiome(x, root.minY() + delta, z)) { reason = "city_final_biome"; continue; }
            return new Fit(true, "skyweave_city_interior", delta, buried, plan.samples().size());
        }
        return rejected(reason, best, plan.samples().size());
    }

    private static boolean core(BoundingBox root, int delta, boolean hollow) {
        for (int x : coordinates(root.minX(),root.maxX())) for (int z : coordinates(root.minZ(),root.maxZ())) {
            int floor = root.minY() + delta, top = root.maxY() + delta;
            var body = CityPlacementScope.exterior(x,z);
            int shell = design.inherent.skyweavecompat.terrain.InteriorConfiguration.profile().shellThickness();
            if (!CityClearance.coreFits(new CityClearance.Range(body.bottomY(), body.topY()), floor, top, shell, supported(x,floor,z))) return false;
            if (hollow && !roof(x,top,z)) return false;
        }
        return true;
    }
    private static boolean supported(int x, int y, int z) {
        return CityPlacementScope.solid(x,z).stream().anyMatch(s -> s.intersectsInclusive(y - 12, y));
    }
    private static boolean roof(int x, int top, int z) {
        return CityPlacementScope.solid(x,z).stream().anyMatch(s -> s.bottomY() >= top
                && s.thickness() >= 8 && CityPlacementScope.exterior(x,z).contains(s.topY()));
    }
    private static boolean floorsFit(List<Floor> floors, int delta, boolean hollow) {
        var supported = new HashSet<Floor>();
        double good = 0, total = 0;
        for (var floor : floors) {
            total += floor.weight;
            if (supported(floor.x, floor.y + delta, floor.z)
                    && (!hollow || roof(floor.x, floor.roof + delta, floor.z))) {
                supported.add(floor); good += floor.weight;
            }
        }
        if (good < total * (hollow ? .9 : .8)) return false;
        // Bounded peripheral exposure, rather than a universal 100% floor rule.
        for (var floor : floors) {
            if (supported.contains(floor)) continue;
            if (supported.stream().noneMatch(other -> Math.abs(other.y - floor.y) <= 8
                    && Math.hypot(other.x - floor.x, other.z - floor.z) <= 16)) return false;
        }
        return true;
    }
    private static List<Floor> floors(List<StructureBurial.Box> boxes) {
        var result = new ArrayList<Floor>();
        for (var box : boxes.stream().distinct().toList()) {
            int[] xs = coordinates(box.minX(),box.maxX()-1), zs = coordinates(box.minZ(),box.maxZ()-1);
            double weight = (double)(box.maxX()-box.minX())*(box.maxZ()-box.minZ()) / (xs.length * zs.length);
            for (int x : xs) for (int z : zs) {
                long owners = boxes.stream().filter(other -> other.contains(x,box.minY(),z)).count();
                result.add(new Floor(x,box.minY(),z,box.maxY()-1,weight/owners));
            }
        }
        return result;
    }
    private static int[] coordinates(int min, int max) {
        return java.util.stream.IntStream.of(min, min + (max-min)/2, max).distinct().toArray();
    }
    private static StructureBurial.Box box(BoundingBox b) {
        return new StructureBurial.Box(b.minX(),b.minY(),b.minZ(),b.maxX()+1,b.maxY()+1,b.maxZ()+1);
    }
    private static Fit rejected(String reason, double coverage, int samples) { return new Fit(false, reason, 0, coverage, samples); }
}
