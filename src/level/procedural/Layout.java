package level.procedural;

import level.Level;
import level.procedural.generator.ProceduralGenerator;
import level.procedural.jump.JumpSimGroup;
import level.procedural.jump.JumpSimulation;
import level.procedural.marker.LayoutMarker;
import level.procedural.marker.resolved.LMDResolvedElement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;

public class Layout {
    public static final boolean DEBUG_RENDER = false;

    public static final boolean DEBUG_RENDER_LM_BOUNDS = true;

    public static final boolean DEBUG_RENDER_SIM = true;
    public static final boolean DEBUG_RENDER_JUMP_MOVEMENT_DISTANCE = false;
    public static final boolean DEBUG_RENDER_VALIDATION_BOUNDS = false;
    public static final boolean DEBUG_RENDER_JUMP_BOUNDS = false;

    public static final boolean DEBUG_RENDER_BEZIER_CURVES = false;

    private final int sectionSize, sectionCount, maxHeight;
    public final ArrayList<LayoutMarker>[] markerSections;
    public final ArrayList<JumpSimGroup>[] jumpSimGroups;
    public final HashSet<JumpSimGroup> roots = new HashSet<>();

    private Level level;

    public Layout(int maxHeight, int sectionSize, int bufferSections, Level level) {
        this.level = level;
        this.maxHeight = maxHeight;
        this.sectionSize = sectionSize;
        sectionCount = ((int) Math.ceil((double) maxHeight / sectionSize)) + bufferSections;
        markerSections = new ArrayList[sectionCount];
        for (int i = 0; i < sectionCount; i++) {
            markerSections[i] = new ArrayList<>();
        }
        jumpSimGroups = new ArrayList[sectionCount];
        for (int i = 0; i < sectionCount; i++) {
            jumpSimGroups[i] = new ArrayList<>();
        }
    }

    public HashSet<LayoutMarker> qProceduralMarkers = new HashSet<>();

    public void generateMarkers() {
        ArrayList<LayoutMarker> markers = new ArrayList<>();
        for (ArrayList<LayoutMarker> markerSection : markerSections) {
            markers.addAll(markerSection);
        }
        markers.forEach(LayoutMarker::generate);
        markers.forEach(ProceduralGenerator::generateBlocks);
        markers.forEach(ProceduralGenerator::generateValidationMarkers);
        markers.forEach(lm -> {
            if (lm.data instanceof LMDResolvedElement rData) {
                rData.setRoot();
                roots.addAll(rData.jumps);
            }
        });
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

    public HashSet<JumpSimGroup> nonReachable(LayoutMarker lm) {
        HashSet<JumpSimGroup> goals = new HashSet<>();
        forEachJumpSimGroup(lm.pos.y, 4, goals::add);
        if (lm.data instanceof LMDResolvedElement) {
            int section = yPosToSection(lm.pos.y);
            if (section < 8) {
                for (JumpSimGroup root : roots) {
                    if (bfsLM(new HashSet<>(), goals, root))
                        return goals;
                }
            } else {
                for (JumpSimGroup root : jumpSimGroups[section - 5]) {
                    if (bfsLM(new HashSet<>(), goals, root))
                        return goals;
                }
                for (JumpSimGroup root : jumpSimGroups[section - 6]) {
                    if (bfsLM(new HashSet<>(), goals, root))
                        return goals;
                }
                for (JumpSimGroup root : jumpSimGroups[section - 7]) {
                    if (bfsLM(new HashSet<>(), goals, root))
                        return goals;
                }
            }
        }
        return goals;
    }

    public boolean bfsLM(HashSet<JumpSimGroup> visited, HashSet<JumpSimGroup> goals, JumpSimGroup root) {
        HashSet<JumpSimGroup> front = new HashSet<>();
        front.add(root);
        return bfsLM(visited, goals, front);
    }

    public boolean bfsLM(HashSet<JumpSimGroup> visited, HashSet<JumpSimGroup> goals, HashSet<JumpSimGroup> front) {
        for (JumpSimGroup group : front) {
            visited.add(group);
            goals.remove(group);
        }
        if (goals.isEmpty())
            return true;
        if (front.isEmpty())
            return false;
        HashSet<JumpSimGroup> newFront = new HashSet<>();
        for (JumpSimGroup group : front) {
            for (Map.Entry<JumpSimulation, JumpSimGroup> jump : group.jumps.entrySet()) {
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
        if (level.outOfBounds(xPos, yPos))
            return;
        markerSections[yPosToSection(yPos)].add(marker);
    }

    public void addJumpGroup(JumpSimGroup group) {
        int xPos = ((int) group.pos.x);
        int yPos = ((int) group.pos.y);
        if (level.outOfBounds(xPos, yPos))
            return;
        jumpSimGroups[yPosToSection(yPos)].add(group);
    }

    public void removeMarker(LayoutMarker marker) {
        int xPos = ((int) marker.pos.x);
        int yPos = ((int) marker.pos.y);
        if (level.outOfBounds(xPos, yPos))
            return;
        markerSections[yPosToSection(yPos)].remove(marker);
        if (marker.data instanceof LMDResolvedElement rData) {
            rData.jumps.forEach(this::removeJumpGroup);
            rData.jumps.clear();
        }
    }

    public void removeJumpGroup(JumpSimGroup group) {
        int xPos = ((int) group.pos.x);
        int yPos = ((int) group.pos.y);
        if (level.outOfBounds(xPos, yPos))
            return;
        jumpSimGroups[yPosToSection(yPos)].remove(group);
    }

    public void forEachMarker(float y, int bufferSections, Consumer<LayoutMarker> action) {
        int min = Math.max(0, yPosToSection(y) - bufferSections);
        int max = Math.min(sectionCount - 1, yPosToSection(y) + bufferSections);
        for (int i = min; i <= max; i++) {
            markerSections[i].forEach(action);
        }
    }

    public void forEachJumpSimGroup(float y, int bufferSections, Consumer<JumpSimGroup> action) {
        int min = Math.max(0, yPosToSection(y) - bufferSections);
        int max = Math.min(sectionCount - 1, yPosToSection(y) + bufferSections);
        for (int i = min; i <= max; i++) {
            jumpSimGroups[i].forEach(action);
        }
    }

    public int yPosToSection(float y) {
        return ((int) (y / sectionSize));
    }
}
