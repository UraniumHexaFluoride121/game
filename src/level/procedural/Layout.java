package level.procedural;

import foundation.MainPanel;
import level.procedural.generator.ProceduralGenerator;
import level.procedural.marker.LayoutMarker;
import level.procedural.marker.resolved.LMDResolvedElement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Layout {
    public static final boolean DEBUG_RENDER = false;

    private final int sectionSize, sectionCount, maxHeight;
    public final ArrayList<LayoutMarker>[] markerSections;
    public final HashSet<LMDResolvedElement> roots = new HashSet<>();

    public Layout(int maxHeight, int sectionSize, int bufferSections) {
        this.maxHeight = maxHeight;
        this.sectionSize = sectionSize;
        sectionCount = ((int) Math.ceil((double) maxHeight / sectionSize)) + bufferSections;
        markerSections = new ArrayList[sectionCount];
        for (int i = 0; i < sectionCount; i++) {
            markerSections[i] = new ArrayList<>();
        }
    }

    public HashSet<LayoutMarker> qProceduralMarkers = new HashSet<>();

    public void generateMarkers() {
        ArrayList<LayoutMarker> markers = new ArrayList<>();
        for (ArrayList<LayoutMarker> markerSection : markerSections) {
            markers.addAll(markerSection);
        }
        markers.forEach(LayoutMarker::generate);
        markers.forEach(lm -> {
            if (lm.data instanceof LMDResolvedElement rData) {
                rData.setRoot();
                roots.add(rData);
            }
        });
        markers.forEach(ProceduralGenerator::generateBlocks);
        markers.forEach(ProceduralGenerator::generateValidationMarkers);
        markers.forEach(this::addProceduralLM);
        while (!qProceduralMarkers.isEmpty()) {
            HashSet<LayoutMarker> generatingMarkers = qProceduralMarkers;
            qProceduralMarkers = new HashSet<>();
            generatingMarkers.forEach(lm -> {
                LMDResolvedElement data = ((LMDResolvedElement) lm.data);
                data.gen.generateMarkers(lm, data.genType);
            });
        }
    }

    public HashSet<LMDResolvedElement> nonReachable(LayoutMarker lm) {
        HashSet<LMDResolvedElement> goals = new HashSet<>();
        forEachMarker(lm.pos.y, 2, nearbyLM -> {
            if (nearbyLM.data instanceof LMDResolvedElement nearbyLMData)
                goals.add(nearbyLMData);
        });
        if (lm.data instanceof LMDResolvedElement) {
            int section = yPosToSection(lm.pos.y);
            if (section < 5) {
                for (LMDResolvedElement root : roots) {
                    if (bfsLM(new HashSet<>(), goals, root))
                        return goals;
                }
            } else {
                for (LayoutMarker root : markerSections[section - 3]) {
                    if (root.data instanceof LMDResolvedElement rootData) {
                        if (bfsLM(new HashSet<>(), goals, rootData))
                            return goals;
                    }
                }
                for (LayoutMarker root : markerSections[section - 4]) {
                    if (root.data instanceof LMDResolvedElement rootData) {
                        if (bfsLM(new HashSet<>(), goals, rootData))
                            return goals;
                    }
                }
            }
        }
        return goals;
    }

    public boolean dfsLM(HashSet<LMDResolvedElement> visited, HashSet<LMDResolvedElement> goals, LMDResolvedElement current) {
        visited.add(current);
        goals.remove(current);
        if (goals.isEmpty())
            return true;
        for (Map.Entry<JumpSimulation, LMDResolvedElement> jump : current.jumps.entrySet()) {
            if (jump.getKey().hasValidJump && !visited.contains(jump.getValue()) && dfsLM(visited, goals, jump.getValue())) {
                return true;
            }
        }
        return false;
    }

    public boolean bfsLM(HashSet<LMDResolvedElement> visited, HashSet<LMDResolvedElement> goals, LMDResolvedElement root) {
        HashSet<LMDResolvedElement> front = new HashSet<>();
        front.add(root);
        return bfsLM(visited, goals, front);
    }

    public boolean bfsLM(HashSet<LMDResolvedElement> visited, HashSet<LMDResolvedElement> goals, HashSet<LMDResolvedElement> front) {
        for (LMDResolvedElement lm : front) {
            visited.add(lm);
            goals.remove(lm);
        }
        if (goals.isEmpty())
            return true;
        if (front.isEmpty())
            return false;
        HashSet<LMDResolvedElement> newFront = new HashSet<>();
        for (LMDResolvedElement lm : front) {
            for (Map.Entry<JumpSimulation, LMDResolvedElement> jump : lm.jumps.entrySet()) {
                if (jump.getKey().hasValidJump && !visited.contains(jump.getValue())) {
                    newFront.add(jump.getValue());
                }
            }
        }
        return bfsLM(visited, goals, newFront);
    }

    public void addProceduralLM(LayoutMarker lm) {
        if (lm.data instanceof LMDResolvedElement)
            qProceduralMarkers.add(lm);
    }

    public void addMarker(LayoutMarker marker) {
        int xPos = ((int) marker.pos.x);
        int yPos = ((int) marker.pos.y);
        if (MainPanel.level.outOfBounds(xPos, yPos))
            return;
        markerSections[yPosToSection(yPos)].add(marker);
    }

    public void removeMarker(LayoutMarker marker) {
        int xPos = ((int) marker.pos.x);
        int yPos = ((int) marker.pos.y);
        if (MainPanel.level.outOfBounds(xPos, yPos))
            return;
        markerSections[yPosToSection(yPos)].remove(marker);
    }

    public void forEachMarker(float y, int bufferSections, Consumer<LayoutMarker> action) {
        int min = Math.max(0, yPosToSection(y) - bufferSections);
        int max = Math.min(sectionCount - 1, yPosToSection(y) + bufferSections);
        for (int i = min; i <= max; i++) {
            markerSections[i].forEach(action);
        }
    }

    public boolean forEachMarkerBreak(float y, int bufferSections, Predicate<LayoutMarker> action) {
        int min = Math.max(0, yPosToSection(y) - bufferSections);
        int max = Math.min(sectionCount - 1, yPosToSection(y) + bufferSections);
        boolean breakLoop = false;
        for (int i = min; i <= max; i++) {
            for (LayoutMarker layoutMarker : markerSections[i]) {
                if (action.test(layoutMarker)) {
                    breakLoop = true;
                    break;
                }
            }
            if (breakLoop)
                break;
        }
        return breakLoop;
    }

    public int yPosToSection(float y) {
        return ((int) (y / sectionSize));
    }
}
