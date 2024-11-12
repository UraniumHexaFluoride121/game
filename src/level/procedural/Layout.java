package level.procedural;

import foundation.MainPanel;
import level.procedural.marker.LayoutMarker;

import java.util.ArrayList;
import java.util.function.Consumer;

public class Layout {
    public static final boolean DEBUG_LAYOUT_RENDER = false;

    private final int sectionSize, sectionCount, maxHeight;
    private final ArrayList<LayoutMarker>[] markerSections;

    public Layout(int maxHeight, int sectionSize, int bufferSections) {
        this.maxHeight = maxHeight;
        this.sectionSize = sectionSize;
        sectionCount = ((int) Math.ceil((double) maxHeight / sectionSize)) + bufferSections;
        markerSections = new ArrayList[sectionCount];
        for (int i = 0; i < sectionCount; i++) {
            markerSections[i] = new ArrayList<>();
        }
    }

    public void generateMarkers() {
        ArrayList<LayoutMarker> markers = new ArrayList<>();
        for (ArrayList<LayoutMarker> markerSection : markerSections) {
            markers.addAll(markerSection);
        }
        markers.forEach(LayoutMarker::generate);
        markers.forEach(LayoutMarker::generateMarkers);
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

    private int yPosToSection(float y) {
        return ((int) (y / sectionSize));
    }
}
